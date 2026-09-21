package com.oneshop.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthPageController {

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        CsrfToken csrfToken =
                (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        model.addAttribute("_csrf", csrfToken);
        return "pages/login";
    }

    @GetMapping("/register")
    public String register() {
        return "pages/register";
    }
}