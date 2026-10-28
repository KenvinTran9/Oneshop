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

@Service
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
                .map(c -> new Category(c.getName(), c.getSlug(), c.getProductCount(), icon(c.getSlug()), teaser(c.getSlug())))
                .toList();
        // Take one from each category before taking a second; never load the full catalog.
        var pools = new ArrayList<List<Product>>();
        for (var category : categorySummaries) {
            if (pools.size() == SUGGESTION_LIMIT) break;
            pools.add(products.findByStatusAndCategory_IdOrderByCreatedAtDescIdDesc(
                    status, category.getId(), PageRequest.of(0, 2)));
        }
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
        var counts = products.summarizeHomeCounts(status);
        var suggestions = List.copyOf(selected.values());
        return new HomeCatalog(categories, suggestions,
                products.summarizeHomeBrands(status, PageRequest.of(0, 8)),
                counts.getProductCount(), counts.getBrandCount(), suggestions.isEmpty() ? null : suggestions.getFirst());
    }

    private static String icon(String slug) {
        return switch (slug) {
            case "cham-soc-da" -> "bi-droplet";
            case "trang-diem" -> "bi-brush";
            case "serum" -> "bi-eyedropper";
            case "kem-duong" -> "bi-flower1";
            case "sua-rua-mat" -> "bi-water";
            default -> "bi-bag-heart";
        };
    }

    private static String teaser(String slug) {
        return switch (slug) {
            case "cham-soc-da" -> "Chăm chút mỗi ngày";
            case "trang-diem" -> "Sắc màu của riêng bạn";
            case "serum" -> "Thêm một bước nâng niu";
            case "kem-duong" -> "Dưỡng ẩm dịu dàng";
            case "sua-rua-mat" -> "Khởi đầu tươi mới";
            default -> "Khám phá điều yêu thích";
        };
    }

    public record Category(String name, String slug, long productCount, String icon, String teaser) { }

    public record HomeCatalog(List<Category> categories, List<Product> suggestions, List<HomeBrand> brands,
                              long productCount, long brandCount, Product heroProduct) { }
}
