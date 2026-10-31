package com.oneshop.service;

import com.oneshop.dto.request.DeliveryForm;
import com.oneshop.dto.request.PaymentReviewForm;
import com.oneshop.entity.*;
import com.oneshop.exception.OrderOperationException;
import com.oneshop.repository.OrderRepository;
import com.oneshop.security.StoreAccess;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderFulfillmentService {
    public record Progress(long version, OrderStatus status, PaymentStatus paymentStatus) { }
    private final OrderRepository orders;
    private final StoreAccess access;
    private final org.springframework.context.ApplicationEventPublisher notifications;
    public OrderFulfillmentService(OrderRepository orders, StoreAccess access, org.springframework.context.ApplicationEventPublisher notifications) {
        this.orders = orders; this.access = access; this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Progress progress(String email, String code) {
        Order order = orders.findByCodeAndCustomerEmailIgnoreCase(code, email).orElseThrow(this::notFound);
        return new Progress(order.getVersion(), order.getStatus(), order.getPaymentStatus());
    }

    @Transactional
    public void reportTransfer(String email, String code) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        if (!email.equalsIgnoreCase(order.getCustomerEmail())) throw notFound();
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER || (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.CONFIRMED))
            throw new OrderOperationException("Đơn này không còn nhận thông báo chuyển khoản.");
        if (order.getPaymentStatus() == PaymentStatus.REPORTED || order.getPaymentStatus() == PaymentStatus.PAID) return;
        if (order.getPaymentStatus() != PaymentStatus.UNPAID) throw new OrderOperationException("Trạng thái thanh toán không hợp lệ.");
        order.setPaymentStatus(PaymentStatus.REPORTED);
        event(order, "Khách hàng thông báo đã chuyển khoản. OneShop đang kiểm tra giao dịch, chưa xác nhận nhận tiền.");
        orders.saveAndFlush(order);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void reviewPayment(String code, PaymentReviewForm form) {
        if (form.action() == null || form.reference() == null || form.reference().isBlank() || form.reference().length() > 120)
            throw new OrderOperationException("Nhập mã giao dịch hoặc lý do đối soát, tối đa 120 ký tự.");
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER)
            throw new OrderOperationException("Đơn COD được xác nhận thu tiền khi chuyển sang Đã giao hàng.");
        if (order.getStatus() == OrderStatus.COMPLETED)
            throw new OrderOperationException("Đơn đã hoàn tất; không thay đổi thanh toán tại bước này.");
        String reference = form.reference().trim();
        switch (form.action()) {
            case CONFIRM_RECEIPT -> {
                if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.REFUND_PENDING
                        || order.getPaymentStatus() == PaymentStatus.REFUNDED) return;
                order.setPaymentStatus(order.getStatus() == OrderStatus.CANCELLED ? PaymentStatus.REFUND_PENDING : PaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now()); order.setPaymentReference(reference);
                order.setPaymentCheckedBy(access.current().getEmail());
                event(order, "OneShop đã đối soát và xác nhận nhận đủ tiền chuyển khoản."
                        + (order.getStatus() == OrderStatus.CANCELLED ? " Đơn đã hủy, chờ xử lý hoàn tiền." : " Đơn đủ điều kiện tiếp tục xử lý."));
            }
            case REJECT_REPORT -> {
                if (order.getPaymentStatus() != PaymentStatus.REPORTED)
                    throw new OrderOperationException("Chỉ yêu cầu kiểm tra lại khi khách đã báo chuyển khoản và tiền chưa được xác nhận.");
                order.setPaymentStatus(PaymentStatus.UNPAID);
                event(order, "OneShop chưa xác minh được khoản thanh toán. " + reference);
            }
            case CONFIRM_REFUND -> {
                if (order.getPaymentStatus() == PaymentStatus.REFUNDED) return;
                if (order.getStatus() != OrderStatus.CANCELLED || order.getPaymentStatus() != PaymentStatus.REFUND_PENDING)
                    throw new OrderOperationException("Chỉ xác nhận hoàn tiền cho đơn đã hủy và đang chờ hoàn.");
                order.setPaymentStatus(PaymentStatus.REFUNDED); order.setRefundReference(reference);
                event(order, "OneShop đã xác nhận hoàn tiền chuyển khoản cho đơn đã hủy.");
            }
        }
        orders.saveAndFlush(order);
        notifications.publishEvent(OrderUpdateMail.of(order, "Cập nhật thanh toán", order.getEvents().get(order.getEvents().size() - 1).getMessage()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @Transactional
    public void updateDelivery(String code, DeliveryForm form) {
        Order order = orders.findForUpdate(code).orElseThrow(this::notFound);
        access.requireStore(order.getStore() == null ? null : order.getStore().getId());
        if (order.getStatus() != OrderStatus.PACKING && order.getStatus() != OrderStatus.SHIPPING)
            throw new OrderOperationException("Xác nhận đơn và bắt đầu chuẩn bị hàng trước khi phân công shipper.");
        if (!Objects.equals(order.getVersion(), form.expectedVersion()))
            throw new OrderOperationException("Đơn vừa có cập nhật. Vui lòng tải lại trang rồi lưu thông tin giao hàng.");
        String carrier = clean(form.carrier()), name = clean(form.shipperName()), phone = clean(form.shipperPhone());
        String tracking = clean(form.trackingCode()), note = clean(form.deliveryNote());
        if (!form.isShipperComplete() || (phone != null && !phone.matches("(?:0[0-9]{9}|\\+84[0-9]{9})"))
                || tooLong(carrier,100) || tooLong(name,100) || tooLong(tracking,100) || tooLong(note,300))
            throw new OrderOperationException("Kiểm tra tên, điện thoại shipper và độ dài thông tin giao hàng.");
        if (Objects.equals(carrier,order.getCarrier()) && Objects.equals(name,order.getShipperName())
                && Objects.equals(phone,order.getShipperPhone()) && Objects.equals(tracking,order.getTrackingCode())
                && Objects.equals(note,order.getDeliveryNote()) && Objects.equals(form.estimatedDelivery(),order.getEstimatedDelivery())) return;
        order.setCarrier(carrier); order.setShipperName(name); order.setShipperPhone(phone);
        order.setTrackingCode(tracking); order.setEstimatedDelivery(form.estimatedDelivery()); order.setDeliveryNote(note);
        event(order, "OneShop đã cập nhật thông tin vận chuyển" + (carrier == null ? "." : " qua " + carrier + ".")
                + (name == null ? "" : " Người giao hàng: " + name + "."));
        orders.saveAndFlush(order);
        notifications.publishEvent(OrderUpdateMail.of(order, "Cập nhật thông tin giao hàng", order.getEvents().get(order.getEvents().size() - 1).getMessage()));
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static boolean tooLong(String value,int max) { return value != null && value.length() > max; }
    private void event(Order order, String message) {
        OrderEvent event = new OrderEvent(); event.setOrder(order); event.setStatus(order.getStatus()); event.setMessage(message);
        order.getEvents().add(event);
    }
    private org.springframework.web.server.ResponseStatusException notFound() {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng.");
    }
}
