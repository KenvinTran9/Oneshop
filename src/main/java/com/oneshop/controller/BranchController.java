package com.oneshop.controller;

import com.oneshop.entity.OrderStatus;
import com.oneshop.exception.OrderOperationException;
import com.oneshop.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/branch")
public class BranchController {
    private final OrderService orders;
    private final com.oneshop.service.OrderFulfillmentService fulfillment;
    public BranchController(OrderService orders, com.oneshop.service.OrderFulfillmentService fulfillment){this.orders=orders;this.fulfillment=fulfillment;}
    @GetMapping public String home(){return "redirect:/branch/orders";}
    @GetMapping("/orders") public String list(@RequestParam(required=false) OrderStatus status,
            @RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("orderBase","/branch/orders");model.addAttribute("orderPage",orders.forBranch(status,page));
        model.addAttribute("statuses",OrderStatus.values());model.addAttribute("selectedStatus",status);return "admin/orders";
    }
    @GetMapping("/orders/{code}") public String detail(@PathVariable String code,Model model){
        model.addAttribute("orderBase","/branch/orders");model.addAttribute("order",orders.branchDetail(code));return "admin/order-detail";
    }
    @PostMapping("/orders/{code}/status") public String update(@PathVariable String code,@RequestParam OrderStatus status,RedirectAttributes redirect){
        try{orders.branchUpdate(code,status);redirect.addFlashAttribute("orderSuccess","Đã cập nhật trạng thái đơn.");}
        catch(OrderOperationException e){redirect.addFlashAttribute("orderError",e.getMessage());}
        return "redirect:/branch/orders/"+code;
    }
    @PostMapping("/orders/{code}/delivery")
    public String delivery(@PathVariable String code, @jakarta.validation.Valid @ModelAttribute com.oneshop.dto.request.DeliveryForm form,
            org.springframework.validation.BindingResult errors, RedirectAttributes redirect) {
        try {
            if (errors.hasErrors()) throw new OrderOperationException("Kiểm tra thông tin giao hàng: tên và điện thoại shipper, mã vận đơn và ngày dự kiến.");
            fulfillment.updateDelivery(code,form); redirect.addFlashAttribute("orderSuccess", "Đã cập nhật thông tin giao hàng cho khách.");
        } catch (OrderOperationException failure) { redirect.addFlashAttribute("orderError",failure.getMessage()); }
        return "redirect:/branch/orders/" + code;
    }
}
