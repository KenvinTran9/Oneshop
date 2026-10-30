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

    public OrderService(OrderRepository orders, UserRepository users, ProductRepository products,
            StoreRepository stores, StoreStockRepository stocks, CartService carts,
            ApplicationEventPublisher events, @Value("${app.orders.shipping-fee:30000}") BigDecimal shippingFee) {
        this.orders = orders; this.users = users; this.products = products; this.stores = stores;
        this.stocks = stocks; this.carts = carts; this.events = events;
        if (shippingFee.signum() < 0) throw new IllegalStateException("Shipping fee cannot be negative");
        this.shippingFee = shippingFee;
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
        for (var line : cart.items()) {
            OrderItem item = new OrderItem(); item.setOrder(order);
            item.setProduct(products.getReferenceById(line.productId())); item.setProductName(line.name());
            item.setImageUrl(line.imageUrl()); item.setSelectedSize(line.size()); item.setSelectedShade(line.shade());
            item.setQuantity(line.quantity()); item.setUnitPrice(line.unitPrice()); item.setLineTotal(line.lineTotal());
            order.getItems().add(item);
        }
        addEvent(order, "OneShop đã nhận đơn hàng của bạn. Thanh toán khi nhận hàng (COD).");
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
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void updateStatus(String code, OrderStatus next) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        if (!order.getStatus().getNextStatuses().contains(next))
            throw new OrderOperationException("Không thể chuyển sang trạng thái này. Vui lòng tải lại đơn hàng.");
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
    private void transition(Order order, OrderStatus next, String message) {
        if (next == OrderStatus.CANCELLED) {
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
        order.setStatus(next); addEvent(order, message); orders.saveAndFlush(order);
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
                items, history, order.getEmailStatus(), order.getEmailSentAt());
    }
    private Pageable pageable(int page) { return PageRequest.of(Math.max(0, page), 12, Sort.by("id").descending()); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."); }
    private OrderOperationException insufficientStock(Store store) {
        return new OrderOperationException(store.getName() + " chưa đủ hàng cho toàn bộ giỏ. Vui lòng chọn cửa hàng khác hoặc giảm số lượng.");
    }
}
