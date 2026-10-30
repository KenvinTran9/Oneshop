package com.oneshop.service;

import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.ProductRepository.HomeBrand;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HomeCatalogService {
    private static final int SUGGESTION_LIMIT = 8;
    private final ProductRepository products;

    public HomeCatalogService(ProductRepository products) {
        this.products = products;
    }

    public HomeCatalog load() {
        var status = ProductStatus.ACTIVE;
        var categorySummaries = products.summarizeHomeCategories(status);
        var categories = categorySummaries.stream()
                .map(c -> new Category(c.getName(), c.getSlug(), c.getProductCount()))
                .toList();
        // Rank in MySQL once, then fetch bounded candidates with their brand/category in one query.
        // This avoids one database round trip per category on a remote Aiven connection.
        var grouped = new LinkedHashMap<Long, List<Product>>();
        var ids = categorySummaries.isEmpty() ? List.<Long>of()
                : products.findHomeCategoryCandidateIds(status.name(), PageRequest.of(0, SUGGESTION_LIMIT * 2));
        if (!ids.isEmpty()) {
            var candidates = products.findHomeCandidates(ids, status).stream()
                    .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));
            for (Long id : ids) {
                Product product = candidates.get(id);
                if (product != null) grouped.computeIfAbsent(product.getCategory().getId(), ignored -> new ArrayList<>()).add(product);
            }
        }
        var pools = grouped.values();
        var selected = new LinkedHashMap<Long, Product>();
        for (int round = 0; round < 2; round++) {
            for (var pool : pools) {
                if (round < pool.size() && selected.size() < SUGGESTION_LIMIT) {
                    var product = pool.get(round);
                    selected.put(product.getId(), product);
                }
            }
        }
        if (selected.size() < SUGGESTION_LIMIT) {
            for (var product : products.findByStatusOrderByCreatedAtDescIdDesc(status, PageRequest.of(0, SUGGESTION_LIMIT))) {
                if (selected.size() == SUGGESTION_LIMIT) break;
                selected.putIfAbsent(product.getId(), product);
            }
        }
        var suggestions = List.copyOf(selected.values());
        // Load only the displayed products' options before the view loses its persistence context.
        suggestions.forEach(product -> {
            org.hibernate.Hibernate.initialize(product.getSizes());
            org.hibernate.Hibernate.initialize(product.getShades());
        });
        return new HomeCatalog(categories, suggestions, products.summarizeHomeBrands(status, PageRequest.of(0, 8)));
    }

    public record Category(String name, String slug, long productCount) {
        public String getImageUrl() {
            return "/images/home/" + switch (slug) {
                case "cham-soc-da" -> "editorial-skin-v1.webp";
                case "trang-diem" -> "editorial-lips-v1.webp";
                case "serum" -> "banner-serum-v1.webp";
                case "kem-duong" -> "banner-skincare-v1.webp";
                default -> "editorial-ritual-v1.webp";
            };
        }
    }

    public record HomeCatalog(List<Category> categories, List<Product> suggestions, List<HomeBrand> brands) { }
}
