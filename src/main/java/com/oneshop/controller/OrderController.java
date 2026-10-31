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
    private final com.oneshop.service.CustomerProfileService profiles;
    private final com.oneshop.service.BankTransferSettings bankTransfer;
    private final com.oneshop.service.OrderFulfillmentService fulfillment;
    public OrderController(OrderService orders, CartService carts, UserRepository users, com.oneshop.service.CustomerProfileService profiles, com.oneshop.service.BankTransferSettings bankTransfer, com.oneshop.service.OrderFulfillmentService fulfillment) {
        this.orders = orders; this.carts = carts; this.users = users; this.profiles = profiles; this.bankTransfer = bankTransfer; this.fulfillment = fulfillment;
    }
    @GetMapping("/checkout")
    public String checkout(Principal principal, @RequestParam(required=false) Long addressId, Model model) {
        if (carts.getCart(principal.getName(), null).items().isEmpty()) return "redirect:/cart";
        CheckoutForm form = new CheckoutForm(); form.setCheckoutToken(UUID.randomUUID().toString());
        users.findByEmail(principal.getName()).ifPresent(user -> {
            form.setCustomerName(user.getFullName()); form.setCustomerPhone(user.getPhone());
        });
        if(addressId != null) {
            var address = profiles.address(principal.getName(),addressId);
            form.setCustomerName(address.getRecipient()); form.setCustomerPhone(address.getPhone()); form.setShippingAddress(address.getAddress());
        }
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
    @PostMapping("/orders/{code}/received")
    public String received(Principal principal, @PathVariable String code, RedirectAttributes redirect) {
        try {
            orders.confirmReceived(principal.getName(), code);
            redirect.addFlashAttribute("orderSuccess", "Cảm ơn bạn đã xác nhận nhận hàng! Đơn hàng đã giao và hoàn thành.");
        } catch (OrderOperationException failure) { redirect.addFlashAttribute("orderError", failure.getMessage()); }
        return "redirect:/orders/" + code;
    }
    @PostMapping("/orders/{code}/transfer")
    public String reportTransfer(Principal principal, @PathVariable String code, RedirectAttributes redirect) {
        try { fulfillment.reportTransfer(principal.getName(), code); redirect.addFlashAttribute("orderSuccess", "Đã ghi nhận thông báo chuyển khoản. OneShop sẽ đối soát và xác nhận tiền."); }
        catch (OrderOperationException failure) { redirect.addFlashAttribute("orderError", failure.getMessage()); }
        return "redirect:/orders/" + code;
    }
    @GetMapping("/orders/{code}/progress")
    @ResponseBody
    public org.springframework.http.ResponseEntity<com.oneshop.service.OrderFulfillmentService.Progress> progress(Principal principal, @PathVariable String code) {
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(fulfillment.progress(principal.getName(), code));
    }
    private void checkoutModel(Principal principal, Model model) {
        var cart = carts.getCart(principal.getName(), null);
        model.addAttribute("cart", cart);
        model.addAttribute("shippingFee", orders.getShippingFee());
        model.addAttribute("checkoutTotal", cart.subtotal().add(orders.getShippingFee()));
        model.addAttribute("customerEmail", principal.getName());
        model.addAttribute("bankTransfer", bankTransfer);
        model.addAttribute("savedAddresses", profiles.addresses(principal.getName()));
    }
}
