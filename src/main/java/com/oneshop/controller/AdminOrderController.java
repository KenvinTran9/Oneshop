package com.oneshop.controller;

import com.oneshop.entity.OrderStatus;
import com.oneshop.exception.OrderOperationException;
import com.oneshop.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private final OrderService orders;
    public AdminOrderController(OrderService orders) { this.orders = orders; }
    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orderPage", orders.forAdmin(status, page));
        model.addAttribute("statuses", OrderStatus.values()); model.addAttribute("selectedStatus", status);
        return "admin/orders";
    }
    @GetMapping("/{code}")
    public String detail(@PathVariable String code, Model model) {
        var order = orders.forAdmin(code);
        model.addAttribute("order", order);
        if (order.status() == OrderStatus.NEW) model.addAttribute("fulfillmentStores", orders.fulfillmentStores(code));
        return "admin/order-detail";
    }
    @PostMapping("/{code}/store")
    public String assign(@PathVariable String code, @RequestParam Long storeId, RedirectAttributes redirect) {
        try { orders.assignStore(code, storeId); redirect.addFlashAttribute("orderSuccess", "Đã cập nhật cửa hàng xử lý đơn."); }
        catch (OrderOperationException failure) { redirect.addFlashAttribute("orderError", failure.getMessage()); }
        return "redirect:/admin/orders/" + code;
    }
    @PostMapping("/{code}/status")
    public String update(@PathVariable String code, @RequestParam OrderStatus status, RedirectAttributes redirect) {
        try { orders.updateStatus(code, status); redirect.addFlashAttribute("orderSuccess", "Đã cập nhật trạng thái đơn hàng."); }
        catch (OrderOperationException failure) { redirect.addFlashAttribute("orderError", failure.getMessage()); }
        return "redirect:/admin/orders/" + code;
    }
    @PostMapping("/{code}/email")
    public String retry(@PathVariable String code, RedirectAttributes redirect) {
        try { orders.retryEmail(code); redirect.addFlashAttribute("orderSuccess", "Đã xử lý yêu cầu gửi email. Xem kết quả ở mục email xác nhận."); }
        catch (OrderOperationException failure) { redirect.addFlashAttribute("orderError", failure.getMessage()); }
        return "redirect:/admin/orders/" + code;
    }
}
