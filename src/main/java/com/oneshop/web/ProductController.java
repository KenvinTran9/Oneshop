package com.oneshop.web;

import com.oneshop.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, 12);
        model.addAttribute("productPage", productService.findActiveProducts(pageable));
        return "pages/products";
    }

    @GetMapping("/products/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        model.addAttribute("product", productService.findActiveProductBySlug(slug));
        return "pages/product-detail";
    }
}
