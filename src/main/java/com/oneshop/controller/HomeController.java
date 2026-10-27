package com.oneshop.controller;

import com.oneshop.service.HomeCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HomeCatalogService homeCatalog;

    public HomeController(HomeCatalogService homeCatalog) {
        this.homeCatalog = homeCatalog;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("home", homeCatalog.load());
        return "pages/home";
    }
}
