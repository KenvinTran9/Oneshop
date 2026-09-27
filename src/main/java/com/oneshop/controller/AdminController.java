package com.oneshop.controller;

import com.oneshop.dto.response.AdminDashboardStatsResponse;
import com.oneshop.service.AdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String dashboard(Model model) {
        AdminDashboardStatsResponse stats = adminService.getDashboardStats();
        model.addAttribute("productCount", stats.productCount());
        model.addAttribute("storeCount", stats.storeCount());
        model.addAttribute("orderCount", stats.orderCount());
        model.addAttribute("customerCount", stats.customerCount());
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("productPage", adminService.findProducts(PageRequest.of(0, 20)));
        return "admin/products";
    }
}
