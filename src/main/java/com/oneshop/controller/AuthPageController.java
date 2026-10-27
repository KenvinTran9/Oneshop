package com.oneshop.controller;

import com.oneshop.dto.request.RegisterRequest;
import com.oneshop.exception.DuplicateEmailException;
import com.oneshop.service.CustomerRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthPageController {

    private final CustomerRegistrationService customerRegistrationService;

    public AuthPageController(CustomerRegistrationService customerRegistrationService) {
        this.customerRegistrationService = customerRegistrationService;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        CsrfToken csrfToken =
                (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        model.addAttribute("_csrf", csrfToken);
        return "pages/login";
    }

    @GetMapping("/register")
    public String register(HttpServletRequest request, Model model) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        model.addAttribute("_csrf", csrfToken);
        model.addAttribute("registrationRequest", new RegisterRequest(null, null, null, null, null));
        return "pages/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "pages/register";
        }

        try {
            customerRegistrationService.register(request);
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "registration.email.duplicate", exception.getMessage());
            return "pages/register";
        }

        redirectAttributes.addFlashAttribute(
                "registrationSuccessMessage",
                "Đăng ký thành công. Vui lòng đăng nhập bằng tài khoản vừa tạo."
        );
        return "redirect:/login";
    }
}
