package com.oneshop.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.entity.ProductShade;
import com.oneshop.repository.BrandRepository;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class CatalogDataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogDataInitializer.class);
    private final ObjectMapper objectMapper;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;

    public CatalogDataInitializer(ObjectMapper objectMapper, ProductRepository products,
                                  CategoryRepository categories, BrandRepository brands) {
        this.objectMapper = objectMapper;
        this.products = products;
        this.categories = categories;
        this.brands = brands;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        List<CatalogEntry> entries;
        try (var input = new ClassPathResource("catalog/products.json").getInputStream()) {
            entries = objectMapper.readValue(input, new TypeReference<List<CatalogEntry>>() { });
        }
        Map<String, Category> categoryCache = new HashMap<>();
        Map<String, Brand> brandCache = new HashMap<>();
        int imported = 0;
        for (CatalogEntry entry : entries) {
            // Keep existing catalog records and subsequent administrator edits intact.
            if (products.findBySku(entry.sku()).isPresent()) {
                continue;
            }
            Category category = categoryCache.computeIfAbsent(entry.categorySlug(), slug ->
                    categories.findBySlug(slug).orElseGet(() -> {
                        Category created = new Category();
                        created.setName(entry.category());
                        created.setSlug(slug);
                        return categories.save(created);
                    }));
            Brand brand = brandCache.computeIfAbsent(entry.brandSlug(), slug ->
                    brands.findBySlug(slug).orElseGet(() -> {
                        Brand created = new Brand();
                        created.setName(entry.brand());
                        created.setSlug(slug);
                        return brands.save(created);
                    }));
            Product product = new Product();
            product.setSku(entry.sku());
            product.setSlug(entry.slug());
            product.setName(entry.name());
            product.setCategory(category);
            product.setBrand(brand);
            product.setShortDescription(entry.description());
            product.setDescription(entry.description());
            product.setBrandCountry(entry.brandCountry());
            product.setPackageSize(entry.packageSize());
            product.setIngredients(entry.ingredients());
            product.setSuitableFor(entry.suitableFor());
            product.setDirections(entry.directions());
            product.setPrecautions(entry.precautions());
            product.setSourceUrl(entry.sourceUrl());
            product.setImageSearchUrl(entry.imageSearchUrl());
            product.setPrice(entry.price());
            product.setPriceFrom(entry.priceFrom());
            product.setPriceCurrency(entry.priceCurrency() == null ? "VND" : entry.priceCurrency());
            product.setPriceVariant(entry.priceVariant());
            product.setPriceSourceUrl(entry.priceSourceUrl());
            product.setPriceCheckedOn(entry.priceCheckedOn());
            product.setImageUrl(entry.imageUrl());
            product.setImageSourceUrl(entry.imageSourceUrl());
            product.setSizes(entry.sizes() == null ? List.of(entry.packageSize()) : entry.sizes());
            product.setShades(entry.shades() == null ? List.of() : entry.shades().stream()
                    .map(shade -> new ProductShade(shade.label(), shade.color(), shade.imageUrl(), shade.imageAlt(), shade.imageType())).toList());
            product.setStatus(ProductStatus.ACTIVE);
            products.save(product);
            imported++;
        }
        LOGGER.info("Imported {} new products from the bundled catalog", imported);
    }

    public record CatalogEntry(String sku, String name, String slug, String category,
                               String categorySlug, String brand, String brandSlug,
                               String brandCountry, String packageSize, String ingredients,
                               String description, String suitableFor, String directions,
                               String precautions, String sourceUrl, String imageSearchUrl,
                               BigDecimal price, String priceVariant, String priceSourceUrl,
                               String priceCheckedOn, String imageUrl, String imageSourceUrl,
                               boolean priceFrom, String priceCurrency,
                               List<String> sizes, List<ShadeEntry> shades) { }

    public record ShadeEntry(String label, String color, String imageUrl, String imageAlt, String imageType) { }
}
