package com.oneshop.service;

import com.oneshop.dto.request.CheckoutForm;
import com.oneshop.dto.response.OrderView;
import com.oneshop.entity.*;
import com.oneshop.exception.OrderOperationException;
import com.oneshop.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    public record Placed(String code) { }
    public record FulfillmentStore(Long id, String name, boolean selected) { }
    private final OrderRepository orders;
    private final UserRepository users;
    private final ProductRepository products;
    private final StoreRepository stores;
    private final StoreStockRepository stocks;
    private final CartService carts;
    private final ApplicationEventPublisher events;
    private final BigDecimal shippingFee;
    private final com.oneshop.security.StoreAccess storeAccess;
    private final BankTransferSettings bankTransfer;
    @Value("${app.support.phone:${SUPPORT_PHONE:}}")
    private String supportPhone;

    public OrderService(OrderRepository orders, UserRepository users, ProductRepository products,
            StoreRepository stores, StoreStockRepository stocks, CartService carts,
            ApplicationEventPublisher events, com.oneshop.security.StoreAccess storeAccess, BankTransferSettings bankTransfer,
            @Value("${app.orders.shipping-fee:30000}") BigDecimal shippingFee) {
        this.orders = orders; this.users = users; this.products = products; this.stores = stores;
        this.stocks = stocks; this.carts = carts; this.events = events;
        if (shippingFee.signum() < 0) throw new IllegalStateException("Shipping fee cannot be negative");
        this.shippingFee = shippingFee; this.storeAccess = storeAccess; this.bankTransfer = bankTransfer;
    }

    public BigDecimal getShippingFee() { return shippingFee; }

    @Transactional
    public String place(String email, CheckoutForm form) {
        User customer = users.findForCartChange(email).filter(User::isActive)
                .orElseThrow(() -> new OrderOperationException("Tài khoản không còn hoạt động."));
        // The same submission must never create another order or deduct stock twice.
        var previous = orders.findByCheckoutToken(form.getCheckoutToken());
        if (previous.isPresent()) {
            if (customer.getEmail().equalsIgnoreCase(previous.get().getCustomerEmail())) return previous.get().getCode();
            throw new OrderOperationException("Phiên đặt hàng không hợp lệ. Vui lòng tải lại trang.");
        }
        if (form.getPaymentMethod() == null) throw new OrderOperationException("Vui lòng chọn phương thức thanh toán.");
        if (form.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && !bankTransfer.isConfigured())
            throw new OrderOperationException("Chuyển khoản đang tạm ngừng. Vui lòng chọn COD hoặc liên hệ OneShop.");
        var cart = carts.getCart(email, null);
        if (cart.items().isEmpty()) throw new OrderOperationException("Giỏ hàng đang trống. Hãy chọn sản phẩm trước nhé.");
        if (cart.items().stream().anyMatch(line -> line.warning() != null))
            throw new OrderOperationException("Có sản phẩm hết hàng hoặc lựa chọn đã thay đổi. Vui lòng kiểm tra lại giỏ hàng.");
        Map<Long, Integer> requested = new TreeMap<>();
        cart.items().forEach(line -> requested.merge(line.productId(), line.quantity(), Integer::sum));
        Store store = reserveAvailableStore(requested);
        Order order = new Order();
        order.setCode("OS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        order.setCheckoutToken(form.getCheckoutToken()); order.setCustomer(customer);
        order.setCustomerEmail(customer.getEmail()); order.setCustomerName(form.getCustomerName());
        order.setCustomerPhone(form.getCustomerPhone()); order.setShippingAddress(form.getShippingAddress());
        order.setNote(form.getNote()); order.setStore(store); order.setShippingFee(shippingFee);
        order.setTotalAmount(cart.subtotal().add(shippingFee));
        order.setPaymentMethod(form.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.UNPAID);
        if (form.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            order.setBankName(bankTransfer.getBankName()); order.setBankAccountNumber(bankTransfer.getAccountNumber());
            order.setBankAccountHolder(bankTransfer.getAccountHolder()); order.setBankQrImage(bankTransfer.getQrImage());
        }
        for (var line : cart.items()) {
            OrderItem item = new OrderItem(); item.setOrder(order);
            item.setProduct(products.getReferenceById(line.productId())); item.setProductName(line.name());
            item.setImageUrl(line.imageUrl()); item.setSelectedSize(line.size()); item.setSelectedShade(line.shade());
            item.setQuantity(line.quantity()); item.setUnitPrice(line.unitPrice()); item.setLineTotal(line.lineTotal());
            order.getItems().add(item);
        }
        addEvent(order, "OneShop đã nhận đơn hàng của bạn. " + (order.getPaymentMethod() == PaymentMethod.COD
                ? "Thanh toán khi nhận hàng (COD)." : "Vui lòng chuyển khoản đúng số tiền và nội dung mã đơn; đơn chờ đối soát thanh toán."));
        orders.saveAndFlush(order);
        carts.clearCart(email, null);
        events.publishEvent(new Placed(order.getCode()));
        return order.getCode();
    }

    @Transactional(readOnly = true)
    public Page<OrderView> forCustomer(String email, int page) {
        return orders.findByCustomerEmailIgnoreCase(email, pageable(page)).map(this::view);
    }
    @Transactional(readOnly = true)
    public OrderView forCustomer(String email, String code) {
        return view(orders.findByCodeAndCustomerEmailIgnoreCase(code, email).orElseThrow(this::notFound));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<OrderView> forAdmin(OrderStatus status, int page) {
        return (status == null ? orders.findAll(pageable(page)) : orders.findByStatus(status, pageable(page))).map(this::view);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public OrderView forAdmin(String code) { return view(orders.findByCode(code).orElseThrow(this::notFound)); }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<FulfillmentStore> fulfillmentStores(String code) {
        Order order = orders.findByCode(code).orElseThrow(this::notFound);
        Long selected = order.getStore() == null ? null : order.getStore().getId();
        return stores.findByActiveTrueOrderByNameAsc().stream()
                .map(store -> new FulfillmentStore(store.getId(), store.getName(), store.getId().equals(selected))).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void assignStore(String code, Long storeId) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        if (order.getStatus() != OrderStatus.NEW)
            throw new OrderOperationException("Chỉ đổi cửa hàng khi đơn đang chờ xác nhận.");
        Store previous = order.getStore();
        if (previous == null) throw new OrderOperationException("Đơn chưa có kho giữ hàng. Vui lòng kiểm tra dữ liệu đơn.");
        Store target = stores.findById(storeId).filter(Store::isActive)
                .orElseThrow(() -> new OrderOperationException("Cửa hàng không còn hoạt động."));
        if (previous.getId().equals(target.getId())) return;
        Map<Long, Integer> requested = new TreeMap<>();
        order.getItems().forEach(item -> requested.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum));
        // Use the same store/product lock order as checkout, even for transfers in opposite directions.
        Map<Long, List<StoreStock>> inventory = new HashMap<>();
        java.util.stream.Stream.of(previous.getId(), target.getId()).sorted()
                .forEach(id -> inventory.put(id, stocks.lockForOrder(id, requested.keySet())));
        var targetInventory = inventory.get(target.getId());
        if (!canFulfill(targetInventory, requested)) throw insufficientStock(target);
        var previousInventory = inventory.get(previous.getId());
        for (var entry : requested.entrySet()) {
            StoreStock before = previousInventory.stream().filter(row -> row.getProduct().getId().equals(entry.getKey()))
                    .findFirst().orElseThrow(() -> new OrderOperationException("Không tìm thấy kho giữ hàng của đơn."));
            before.setQuantity(Math.addExact(before.getQuantity(), entry.getValue()));
        }
        deduct(targetInventory, requested);
        order.setStore(target);
        addEvent(order, "OneShop đã phân công " + target.getName() + " xử lý đơn hàng.");
        orders.saveAndFlush(order);
    }

    private Store reserveAvailableStore(Map<Long, Integer> requested) {
        for (Store store : stores.findByActiveTrueOrderByIdAsc()) {
            var inventory = stocks.lockForOrder(store.getId(), requested.keySet());
            if (canFulfill(inventory, requested)) {
                deduct(inventory, requested);
                return store;
            }
        }
        throw new OrderOperationException("Chưa đủ hàng để xử lý toàn bộ giỏ. Vui lòng giảm số lượng hoặc thử lại sau.");
    }

    private boolean canFulfill(List<StoreStock> inventory, Map<Long, Integer> requested) {
        return requested.entrySet().stream().allMatch(entry -> inventory.stream().anyMatch(row -> row.isActive()
                && row.getProduct().getId().equals(entry.getKey()) && row.getQuantity() >= entry.getValue()));
    }

    private void deduct(List<StoreStock> inventory, Map<Long, Integer> requested) {
        for (var row : inventory) {
            row.setQuantity(row.getQuantity() - requested.get(row.getProduct().getId()));
        }
    }

    @Transactional
    public void cancel(String email, String code) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        if (!email.equalsIgnoreCase(order.getCustomerEmail())) throw notFound();
        if (order.getStatus() != OrderStatus.NEW)
            throw new OrderOperationException("Chỉ có thể tự hủy đơn đang chờ xác nhận.");
        transition(order, OrderStatus.CANCELLED, "Khách hàng đã hủy đơn trước khi xác nhận.");
    }
    @Transactional
    public void confirmReceived(String email, String code) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        if (!email.equalsIgnoreCase(order.getCustomerEmail())) throw notFound();
        if (order.getStatus() == OrderStatus.COMPLETED) return;
        if (order.getStatus() != OrderStatus.SHIPPING)
            throw new OrderOperationException("Chỉ xác nhận nhận hàng khi đơn đang được giao.");
        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentStatus() != PaymentStatus.PAID)
            throw new OrderOperationException("OneShop cần xác nhận thanh toán trước khi hoàn thành đơn.");
        transition(order, OrderStatus.COMPLETED, "Khách hàng đã xác nhận nhận được hàng. Đơn hàng đã giao và hoàn thành.");
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void updateStatus(String code, OrderStatus next) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        updateStatus(order, next);
    }
    private void updateStatus(Order order, OrderStatus next) {
        if (!order.getStatus().getNextStatuses().contains(next))
            throw new OrderOperationException("Không thể chuyển sang trạng thái này. Vui lòng tải lại đơn hàng.");
        if (next == OrderStatus.COMPLETED)
            throw new OrderOperationException("Khách hàng cần xác nhận đã nhận hàng để hoàn thành đơn.");
        if (next == OrderStatus.SHIPPING && (order.getShipperName() == null || order.getShipperName().isBlank()
                || order.getShipperPhone() == null || order.getShipperPhone().isBlank()))
            throw new OrderOperationException("Nhập tên và số điện thoại shipper sau khi chuẩn bị hàng, rồi mới bàn giao.");
        if (next != OrderStatus.CANCELLED && next != OrderStatus.CONFIRMED && order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                && order.getPaymentStatus() != PaymentStatus.PAID)
            throw new OrderOperationException("Đơn đã được xác nhận. Admin cần đối soát tiền chuyển khoản trước khi bắt đầu chuẩn bị hàng.");
        String message = switch (next) {
            case CONFIRMED -> "Cửa hàng đã xác nhận đơn hàng.";
            case PACKING -> "Sản phẩm đang được chuẩn bị và đóng gói.";
            case SHIPPING -> "Đơn hàng đã được bàn giao để vận chuyển.";
            case COMPLETED -> "Đơn hàng đã giao thành công. Cảm ơn bạn đã chọn OneShop!";
            case CANCELLED -> "Cửa hàng đã hủy đơn hàng và hoàn lại tồn kho.";
            default -> throw new OrderOperationException("Trạng thái không hợp lệ.");
        };
        transition(order, next, message);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void retryEmail(String code) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        if ("SENT".equals(order.getEmailStatus())) throw new OrderOperationException("Email xác nhận đã được gửi.");
        if (order.getStatus() == OrderStatus.CANCELLED) throw new OrderOperationException("Đơn đã hủy không gửi lại email đặt hàng thành công.");
        events.publishEvent(new Placed(code));
    }
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @Transactional(readOnly = true)
    public Page<OrderView> forBranch(OrderStatus status, int page) {
        Long scope = storeAccess.scope(null);
        if (scope == null) return (status == null ? orders.findAll(pageable(page)) : orders.findByStatus(status,pageable(page))).map(this::view);
        return orders.findBranchOrders(scope,status,pageable(page)).map(this::view);
    }
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @Transactional(readOnly = true)
    public OrderView branchDetail(String code) {
        Order order = orders.findByCode(code).orElseThrow(this::notFound);
        storeAccess.requireStore(order.getStore() == null ? null : order.getStore().getId()); return view(order);
    }
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @Transactional
    public void branchUpdate(String code, OrderStatus status) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        storeAccess.requireStore(order.getStore() == null ? null : order.getStore().getId()); updateStatus(order,status);
    }
    private void transition(Order order, OrderStatus next, String message) {
        if (next == OrderStatus.CANCELLED) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                message += " Đơn đã thanh toán; OneShop sẽ đối soát và xử lý hoàn tiền.";
            } else if (order.getPaymentStatus() == PaymentStatus.REPORTED) {
                message += " Khách đã báo chuyển khoản; OneShop cần kiểm tra giao dịch và hoàn tiền nếu đã nhận.";
            }
            Map<Long, Integer> quantities = new TreeMap<>();
            order.getItems().forEach(item -> quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum));
            if (!quantities.isEmpty() && order.getStore() != null) {
                var inventory = stocks.lockForOrder(order.getStore().getId(), quantities.keySet());
                for (var entry : quantities.entrySet()) {
                    var stock = inventory.stream().filter(row -> row.getProduct().getId().equals(entry.getKey()))
                            .findFirst().orElseThrow(() -> new OrderOperationException("Không tìm thấy kho của đơn hàng để hoàn tồn."));
                    stock.setQuantity(Math.addExact(stock.getQuantity(), entry.getValue()));
                }
            }
        }
        if (next == OrderStatus.COMPLETED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID); order.setPaidAt(java.time.LocalDateTime.now());
            message += " Đã xác nhận thu tiền COD.";
        }
        if (next == OrderStatus.COMPLETED) {
            String phone = supportPhone == null || supportPhone.isBlank()
                    ? (order.getStore() == null ? null : order.getStore().getPhone()) : supportPhone.trim();
            message += " Cảm ơn bạn đã mua sắm tại OneShop! Nếu muốn góp ý hoặc cần hỗ trợ, "
                    + (phone == null || phone.isBlank() ? "vui lòng xem thông tin liên hệ tại mục Cửa hàng trên website."
                    : "vui lòng liên hệ OneShop qua số điện thoại " + phone + ".");
        }
        order.setStatus(next); addEvent(order, message); orders.saveAndFlush(order);
        events.publishEvent(OrderUpdateMail.of(order, next.getLabel(), message));
    }
    private void addEvent(Order order, String message) {
        OrderEvent event = new OrderEvent(); event.setOrder(order); event.setStatus(order.getStatus()); event.setMessage(message);
        order.getEvents().add(event);
    }
    OrderView view(Order order) {
        var items = order.getItems().stream().map(i -> new OrderView.Item(i.getProductName(), i.getImageUrl(),
                i.getSelectedSize(), i.getSelectedShade(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal())).toList();
        var history = order.getEvents().stream().map(e -> new OrderView.Event(e.getStatus(), e.getMessage(), e.getCreatedAt())).toList();
        return new OrderView(order.getCode(), order.getStatus(), order.getCreatedAt(), order.getCustomerName(),
                order.getCustomerEmail(), order.getCustomerPhone(), order.getShippingAddress(),
                order.getStore() == null ? "OneShop" : order.getStore().getName(), order.getNote(),
                order.getTotalAmount().subtract(order.getShippingFee()), order.getShippingFee(), order.getTotalAmount(),
                items, history, order.getEmailStatus(), order.getEmailSentAt(),
                new OrderView.Payment(order.getPaymentMethod(), order.getPaymentStatus(), order.getPaidAt(), order.getPaymentReference(),
                        order.getBankName(), order.getBankAccountNumber(), order.getBankAccountHolder(), order.getBankQrImage(), order.getCode().replace("-", "")),
                new OrderView.Delivery(order.getCarrier(), order.getShipperName(), order.getShipperPhone(), order.getTrackingCode(),
                        order.getEstimatedDelivery(), order.getDeliveryNote()), order.getVersion() == null ? 0L : order.getVersion());
    }
    private Pageable pageable(int page) { return PageRequest.of(Math.max(0, page), 12, Sort.by("id").descending()); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."); }
    private OrderOperationException insufficientStock(Store store) {
        return new OrderOperationException(store.getName() + " chưa đủ hàng cho toàn bộ giỏ. Vui lòng chọn cửa hàng khác hoặc giảm số lượng.");
    }
}
