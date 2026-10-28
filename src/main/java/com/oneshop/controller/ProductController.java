package com.oneshop.controller;

import com.oneshop.service.ProductService;
import com.oneshop.repository.CategoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductService productService, CategoryRepository categoryRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "") String category,
                           @RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "newest") String sort, Model model) {
        String selectedSort = java.util.Set.of("name", "price-asc", "price-desc").contains(sort) ? sort : "newest";
        Sort ordering = switch (selectedSort) {
            case "name" -> Sort.by("name").ascending().and(Sort.by("id"));
            case "price-asc" -> Sort.by("price").ascending().and(Sort.by("id"));
            case "price-desc" -> Sort.by("price").descending().and(Sort.by("id"));
            default -> Sort.by("id").descending();
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), 12, ordering);
        model.addAttribute("productPage", productService.searchCatalog(category, q, pageable));
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSort", selectedSort);
        model.addAttribute("searchQuery", q.trim());
        return "pages/products";
    }

    @GetMapping("/products/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        model.addAttribute("product", productService.findActiveProductBySlug(slug));
        return "pages/product-detail";
    }
}
