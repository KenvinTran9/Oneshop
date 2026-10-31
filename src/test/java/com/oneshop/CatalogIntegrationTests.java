package com.oneshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneshop.config.CatalogDataInitializer;
import com.oneshop.config.CatalogDataInitializer.CatalogEntry;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.repository.BrandRepository;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.service.HomeCatalogService;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"dev", "test"})
@Transactional
class CatalogIntegrationTests extends com.oneshop.MySqlIntegrationTestSupport {
    @Autowired ProductRepository products;
    @Autowired com.oneshop.repository.StoreRepository stores;
    @Autowired com.oneshop.repository.StoreStockRepository stocks;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired CatalogDataInitializer initializer;
    @Autowired ObjectMapper objectMapper;
    @Autowired MockMvc mvc;
    @Autowired HomeCatalogService homeCatalog;

    @org.junit.jupiter.api.BeforeEach
    void restoreCatalogFixture() throws Exception {
        initializer.run();
        var store=new com.oneshop.entity.Store();store.setCode("CATALOG-TEST");store.setName("Catalog test");store.setAddress("Địa chỉ thử");stores.saveAndFlush(store);
        for(var p:products.findAll()){
            var row=new com.oneshop.entity.StoreStock();row.setStore(store);row.setProduct(p);row.setQuantity(10);stocks.save(row);
        }
        stocks.flush();
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void homepageBalancesActiveProductsAndUsesActualCatalogCounts() throws Exception {
        var home = homeCatalog.load();
        assertThat(home.categories()).hasSize(5).allSatisfy(c -> assertThat(c.productCount()).isEqualTo(20));
        assertThat(home.suggestions()).hasSize(8).extracting(Product::getId).doesNotHaveDuplicates();
        assertThat(home.suggestions()).extracting(p -> p.getCategory().getSlug()).containsAll(
                home.categories().stream().map(HomeCatalogService.Category::slug).toList());
        assertThat(home.brands()).hasSize(8).allSatisfy(b -> assertThat(b.getProductCount()).isPositive());
        String page = mvc.perform(get("/")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(page).contains("Chăm chút làn da.", "Gợi ý OneShop", "Tìm cái tên bạn yêu thích",
                "action=\"/cart/items\"", "name=\"productId\"", "/products?category=", "/products?q=", "/css/home.css")
                .doesNotContain("images.unsplash.com", "Sản phẩm mới nhất", "home-catalog-stats");
        assertThat(page.split("data-product-card", -1)).hasSize(9);
    }

    @Test
    void homepageFillsSuggestionsFromASparseCatalogWithoutDuplicates() {
        var keep = products.findBySku("CATALOG-001").orElseThrow().getCategory().getId();
        var catalog = products.findAll();
        catalog.stream().filter(p -> !p.getCategory().getId().equals(keep)).forEach(p -> p.setStatus(ProductStatus.DRAFT));
        products.saveAllAndFlush(catalog);
        var home = homeCatalog.load();
        assertThat(home.categories()).hasSize(1);
        assertThat(home.suggestions()).hasSize(8).extracting(Product::getId).doesNotHaveDuplicates();
        assertThat(home.suggestions()).allSatisfy(p -> {
            assertThat(p.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(p.getCategory().getId()).isEqualTo(keep);
        });
    }

    @Test
    void homepageRendersGracefullyWithoutActiveProducts() throws Exception {
        var catalog = products.findAll();
        catalog.forEach(p -> p.setStatus(ProductStatus.DRAFT));
        products.saveAllAndFlush(catalog);
        var home = homeCatalog.load();
        assertThat(home.suggestions()).isEmpty();
        assertThat(home.categories()).isEmpty();
        assertThat(home.brands()).isEmpty();
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Danh mục đang được cập nhật")));
    }

    @Test
    void importsAllSourceFieldsAndGroupsWithoutDemoProducts() throws Exception {
        List<CatalogEntry> entries;
        try (var input = new ClassPathResource("catalog/products.json").getInputStream()) {
            entries = objectMapper.readValue(input, new TypeReference<List<CatalogEntry>>() { });
        }
        assertThat(entries).hasSize(100);
        assertThat(products.count()).isEqualTo(100);
        assertThat(categories.count()).isEqualTo(5);
        assertThat(brands.count()).isEqualTo(59);
        for (CatalogEntry source : entries) {
            Product actual = products.findBySku(source.sku()).orElseThrow();
            assertThat(actual).extracting("name", "slug", "brandCountry", "packageSize", "ingredients",
                            "description", "suitableFor", "directions", "precautions", "sourceUrl", "imageSearchUrl")
                    .containsExactly(source.name(), source.slug(), source.brandCountry(), source.packageSize(),
                            source.ingredients(), source.description(), source.suitableFor(), source.directions(),
                            source.precautions(), source.sourceUrl(), source.imageSearchUrl());
            assertThat(actual.getBrand().getName()).isEqualTo(source.brand());
            assertThat(actual.getCategory().getName()).isEqualTo(source.category());
            assertThat(actual.getPrice()).isEqualByComparingTo(source.price());
            assertThat(actual.getImageUrl()).isEqualTo(source.imageUrl());
            assertThat(actual.getPriceSourceUrl()).isEqualTo(source.priceSourceUrl());
            assertThat(actual.getPriceCurrency()).isEqualTo(source.priceCurrency());
            assertThat(actual.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(actual.getPriceCurrency()).isEqualTo("VND");
            assertThat(actual.isPriceFrom()).isFalse();
            assertThat(actual.getSizes()).containsExactlyElementsOf(source.sizes());
            assertThat(actual.getShades()).hasSize(source.shades().size());
            assertThat(actual.getShades()).allSatisfy(shade -> {
                assertThat(shade.getLabel()).isNotBlank();
                assertThat(shade.getColor()).matches("#[0-9a-fA-F]{6}");
                assertThat(shade.getImageUrl()).startsWith("/images/catalog/shades/");
                assertThat(shade.getImageAlt()).contains(actual.getName());
                assertThat(shade.getImageType()).isIn("photo", "chart");
                assertThat(new ClassPathResource("static" + shade.getImageUrl()).exists()).isTrue();
            });
            assertThat(new ClassPathResource("static" + actual.getImageUrl()).exists()).isTrue();
        }
        assertThat(products.findBySku("CATALOG-044").orElseThrow().getDisplayPrice()).isEqualTo("316.000 ₫");
        assertThat(products.findBySku("CATALOG-016").orElseThrow().getDisplayPrice()).isEqualTo("155.000 ₫");
    }

    @Test
    void repeatedImportDoesNotDuplicateOrOverwriteAdministratorChanges() throws Exception {
        Product product = products.findBySku("CATALOG-001").orElseThrow();
        product.setPrice(new BigDecimal("123456"));
        products.saveAndFlush(product);
        initializer.run();
        assertThat(products.count()).isEqualTo(100);
        assertThat(products.findBySku("CATALOG-001").orElseThrow().getPrice()).isEqualByComparingTo("123456");
        assertThat(categories.count()).isEqualTo(5);
        assertThat(brands.count()).isEqualTo(59);
    }

    @Test
    @SuppressWarnings("unchecked")
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void paginationExposesEveryProductAndCategoryFiltersHaveTwentyProducts() throws Exception {
        var slugs = new HashSet<String>();
        for (int page = 0; page < 9; page++) {
            var result = mvc.perform(get("/products").param("page", String.valueOf(page)))
                    .andExpect(status().isOk()).andReturn();
            Page<Product> listed = (Page<Product>) result.getModelAndView().getModel().get("productPage");
            assertThat(listed.getTotalElements()).isEqualTo(100);
            listed.forEach(product -> assertThat(slugs.add(product.getSlug())).isTrue());
        }
        assertThat(slugs).hasSize(100);
        for (var category : categories.findAll()) {
            var result = mvc.perform(get("/products").param("category", category.getSlug()).param("sort", "name"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("selectedCategory", category.getSlug())).andReturn();
            Page<Product> listed = (Page<Product>) result.getModelAndView().getModel().get("productPage");
            assertThat(listed.getTotalElements()).isEqualTo(20);
            assertThat(listed.getContent()).allSatisfy(product ->
                    assertThat(product.getCategory().getSlug()).isEqualTo(category.getSlug()));
        }
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void storefrontHasFixedPricesAndPurchaseOptionsWithoutSourceLinks() throws Exception {
        Product product = products.findBySku("CATALOG-063").orElseThrow();
        String page = mvc.perform(get("/products/" + product.getSlug())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(page).contains("Thành phần nổi bật", "Thương hiệu từ", "675.000 ₫",
                "name=\"shade\"", "Hope", "name=\"quantity\"", "action=\"/cart/items\"", "data-product-form",
                "data-shade-image=\"/images/catalog/shades/", "data-gallery-image", "data-gallery-original", "data-gallery-shade")
                .doesNotContain("Giá tham khảo", "Xem giá tại nguồn", product.getPriceSourceUrl(), product.getSourceUrl());
        mvc.perform(get("/cart")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler()
                        .handlerType(com.oneshop.controller.CartController.class))
                .andExpect(model().attributeExists("cart"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Giỏ hàng đang trống")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchCombinesBrandCategoryAndPriceOrdering() throws Exception {
        var result = mvc.perform(get("/products").param("q", "  beauty of joseon  ")
                .param("sort", "price-asc")).andExpect(status().isOk()).andReturn();
        Page<Product> matches = (Page<Product>) result.getModelAndView().getModel().get("productPage");
        assertThat(matches.getTotalElements()).isEqualTo(7);
        assertThat(matches.getContent()).extracting(Product::getSellingPrice).isSorted();
        assertThat(matches.getContent()).allSatisfy(product -> assertThat(product.getBrand().getName()).isEqualTo("Beauty of Joseon"));
        var empty = mvc.perform(get("/products").param("q", "no-such-product-xyz")).andExpect(status().isOk()).andReturn();
        assertThat(((Page<Product>) empty.getModelAndView().getModel().get("productPage")).getTotalElements()).isZero();
        var combined = mvc.perform(get("/products").param("q", "beauty of joseon").param("category", "trang-diem"))
                .andExpect(status().isOk()).andReturn();
        assertThat(((Page<Product>) combined.getModelAndView().getModel().get("productPage")).getTotalElements()).isZero();
    }
}

