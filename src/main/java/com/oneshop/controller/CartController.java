package com.oneshop.controller;

import com.oneshop.dto.request.CartItemRequest;
import com.oneshop.dto.response.CartCountResponse;
import com.oneshop.dto.response.CartView;
import com.oneshop.exception.CartOperationException;
import com.oneshop.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String cart(Authentication authentication, HttpServletRequest request, Model model) {
        CartView cart = cartService.getCart(authenticatedEmail(authentication), request.getSession(false));
        model.addAttribute("cart", cart);
        return "pages/cart";
    }

    @GetMapping("/count")
    @ResponseBody
    public CartCountResponse count(Authentication authentication, HttpServletRequest request) {
        return new CartCountResponse(
                cartService.getItemCount(authenticatedEmail(authentication), request.getSession(false))
        );
    }

    @PostMapping("/items")
    public String addItem(@Valid @ModelAttribute CartItemRequest cartItemRequest,
                          BindingResult bindingResult,
                          Authentication authentication,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("cartError", "Số lượng hoặc sản phẩm không hợp lệ.");
            return "redirect:/cart";
        }

        try {
            cartService.addItem(
                    authenticatedEmail(authentication),
                    request.getSession(authenticatedEmail(authentication) == null),
                    cartItemRequest.productId(),
                    cartItemRequest.quantity()
            );
            redirectAttributes.addFlashAttribute("cartSuccess", "Đã thêm sản phẩm vào giỏ hàng.");
        } catch (CartOperationException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/items/{productId}")
    public String updateQuantity(@PathVariable Long productId,
                                 @RequestParam int quantity,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(
                    authenticatedEmail(authentication),
                    request.getSession(false),
                    productId,
                    quantity
            );
            redirectAttributes.addFlashAttribute("cartSuccess", "Đã cập nhật số lượng.");
        } catch (CartOperationException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/items/{productId}/remove")
    public String removeItem(@PathVariable Long productId,
                             Authentication authentication,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            cartService.removeItem(
                    authenticatedEmail(authentication),
                    request.getSession(false),
                    productId
            );
            redirectAttributes.addFlashAttribute("cartSuccess", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (CartOperationException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(Authentication authentication,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        cartService.clearCart(authenticatedEmail(authentication), request.getSession(false));
        redirectAttributes.addFlashAttribute("cartSuccess", "Đã xóa toàn bộ giỏ hàng.");
        return "redirect:/cart";
    }

    private String authenticatedEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
