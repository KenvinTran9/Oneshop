package com.oneshop.web;

import com.oneshop.service.StoreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/stores")
    public String stores(Model model) {
        model.addAttribute("stores", storeService.findActiveStores());
        return "pages/stores";
    }
}
