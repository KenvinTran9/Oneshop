package com.oneshop.controller;

import com.oneshop.dto.request.CheckoutForm;
import com.oneshop.exception.OrderOperationException;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.CartService;
import com.oneshop.service.OrderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {
    private final OrderService orders;
    private final CartService carts;
    private final UserRepository users;
    public OrderController(OrderService orders, CartService carts, UserRepository users) {
        this.orders = orders; this.carts = carts; this.users = users;
    }
    @GetMapping("/checkout")
    public String checkout(Principal principal, Model model) {
        if (carts.getCart(principal.getName(), null).items().isEmpty()) return "redirect:/cart";
        CheckoutForm form = new CheckoutForm(); form.setCheckoutToken(UUID.randomUUID().toString());
        users.findByEmail(principal.getName()).ifPresent(user -> {
            form.setCustomerName(user.getFullName()); form.setCustomerPhone(user.getPhone());
        });
        model.addAttribute("checkoutForm", form); checkoutModel(principal, model);
        return "pages/checkout";
    }
    @PostMapping("/checkout")
    public String place(@Valid @ModelAttribute CheckoutForm checkoutForm, BindingResult errors,
            Principal principal, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                String code = orders.place(principal.getName(), checkoutForm);
                redirect.addFlashAttribute("orderSuccess", "Đặt hàng thành công! Bạn có thể theo dõi tiến trình xử lý ngay bên dưới.");
                return "redirect:/orders/" + code;
            } catch (OrderOperationException failure) { model.addAttribute("orderError", failure.getMessage()); }
        }
        checkoutModel(principal, model); return "pages/checkout";
    }
    @GetMapping("/orders")
    public String history(Principal principal, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orderPage", orders.forCustomer(principal.getName(), page)); return "pages/orders";
    }
    @GetMapping("/orders/{code}")
    public String detail(Principal principal, @PathVariable String code, Model model) {
        model.addAttribute("order", orders.forCustomer(principal.getName(), code)); return "pages/order-detail";
    }
    @PostMapping("/orders/{code}/cancel")
    public String cancel(Principal principal, @PathVariable String code, RedirectAttributes redirect) {
        try { orders.cancel(principal.getName(), code); redirect.addFlashAttribute("orderSuccess", "Đơn hàng đã được hủy."); }
        catch (OrderOperationException failure) { redirect.addFlashAttribute("orderError", failure.getMessage()); }
        return "redirect:/orders/" + code;
    }
    private void checkoutModel(Principal principal, Model model) {
        var cart = carts.getCart(principal.getName(), null);
        model.addAttribute("cart", cart);
        model.addAttribute("shippingFee", orders.getShippingFee());
        model.addAttribute("checkoutTotal", cart.subtotal().add(orders.getShippingFee()));
        model.addAttribute("customerEmail", principal.getName());
    }
}
