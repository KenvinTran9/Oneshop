package com.oneshop.web;

import com.oneshop.service.ProductService;
import com.oneshop.service.StoreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductService productService;
    private final StoreService storeService;

    public HomeController(ProductService productService, StoreService storeService) {
        this.productService = productService;
        this.storeService = storeService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.findFeaturedProducts());
        model.addAttribute("stores", storeService.findActiveStores());
        return "pages/home";
    }
}
