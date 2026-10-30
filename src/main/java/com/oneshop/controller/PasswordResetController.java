package com.oneshop.controller;

import com.oneshop.dto.request.*;
import com.oneshop.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {
    private final PasswordResetService resets;
    public PasswordResetController(PasswordResetService resets) { this.resets = resets; }
    @GetMapping("/forgot-password")
    public String forgot(Model model) {
        model.addAttribute("forgotForm", new ForgotPasswordRequest(null)); return "pages/forgot-password";
    }
    @PostMapping("/forgot-password")
    public String request(@Valid @ModelAttribute("forgotForm") ForgotPasswordRequest form, BindingResult errors,
                          RedirectAttributes redirect) {
        if (errors.hasErrors()) return "pages/forgot-password";
        resets.request(form.email());
        redirect.addFlashAttribute("resetRequested", true);
        return "redirect:/forgot-password";
    }
    @GetMapping("/reset-password")
    public String reset(@RequestParam(defaultValue = "") String token, Model model) {
        model.addAttribute("resetForm", new ResetPasswordRequest(token, null, null));
        model.addAttribute("validLink", resets.isUsable(token)); return "pages/reset-password";
    }
    @PostMapping("/reset-password")
    public String complete(@Valid @ModelAttribute("resetForm") ResetPasswordRequest form, BindingResult errors,
                           Model model, HttpServletRequest request, RedirectAttributes redirect) {
        if (!java.util.Objects.equals(form.password(), form.confirmPassword()))
            errors.rejectValue("confirmPassword", "reset.confirmation", "Mật khẩu xác nhận chưa khớp.");
        boolean valid = resets.isUsable(form.token());
        model.addAttribute("validLink", valid);
        if (!valid || errors.hasErrors()) return "pages/reset-password";
        try { resets.reset(form.token(), form.password()); }
        catch (IllegalArgumentException failure) {
            model.addAttribute("validLink", false); return "pages/reset-password";
        }
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        redirect.addFlashAttribute("passwordResetSuccess", true);
        return "redirect:/login";
    }
}
