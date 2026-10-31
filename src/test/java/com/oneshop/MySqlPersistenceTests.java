package com.oneshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oneshop.config.CatalogDataInitializer;
import com.oneshop.config.DevStockInitializer;
import com.oneshop.config.RoleInitializer;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import com.oneshop.service.CartService;
import com.oneshop.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.dev.seed-stock=true")
@ActiveProfiles({"dev", "test"})
class MySqlPersistenceTests extends MySqlIntegrationTestSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductRepository products;
    @Autowired ProductService productService;
    @Autowired BrandRepository brands;
    @Autowired StoreRepository stores;
    @Autowired StoreStockRepository stocks;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired CartRepository carts;
    @Autowired CartService cartService;
    @Autowired CatalogDataInitializer catalog;
    @Autowired DevStockInitializer stockSeed;
    @Autowired RoleInitializer roleSeed;

    @BeforeEach
    void seedFixture() throws Exception {
        roleSeed.run();
        Store store = new Store(); store.setCode("HCM-D1"); store.setName("Local test shop");
        store.setAddress("Test address"); stores.save(store);
        catalog.run(); stockSeed.run();
    }

    @Test
    void usesMySqlDecimalForeignKeysAndSeparateOptionTables() {
        Product product = productService.findActiveProductBySlug(products.findBySku("CATALOG-016").orElseThrow().getSlug());
        assertThat(jdbc.queryForObject("select data_type from information_schema.columns where table_schema=database() and table_name='products' and column_name='price'", String.class))
                .isEqualTo("decimal");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.key_column_usage where table_schema=database() and table_name='products' and referenced_table_name is not null", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from product_shades where product_id=?", Integer.class, product.getId())).isEqualTo(3);
        assertThat(product.getBrand().getName()).isEqualTo("rom&nd");
        assertThat(product.getShades()).hasSize(3);
    }

    @Test
    void seedDoesNotResetExistingZeroStockOrDuplicateCatalog() throws Exception {
        assertThat(stocks.count()).isEqualTo(100);
        StoreStock stock = stocks.findByProductId(products.findBySku("CATALOG-016").orElseThrow().getId()).getFirst();
        stock.setQuantity(0); stocks.saveAndFlush(stock);
        catalog.run(); stockSeed.run();
        assertThat(products.count()).isEqualTo(100);
        assertThat(stocks.count()).isEqualTo(100);
        assertThat(stocks.findById(stock.getId()).orElseThrow().getQuantity()).isZero();
    }

    @Test
    void inactiveStoresDoNotContributeToAvailableStock() {
        Product product = products.findBySku("CATALOG-016").orElseThrow();
        Store store = stores.findByCode("HCM-D1").orElseThrow();
        store.setActive(false); stores.saveAndFlush(store);
        assertThat(stocks.sumAvailableQuantityByProductId(product.getId())).isZero();
    }

    @Test
    void uniqueConstraintRejectsCaseVariants() {
        Product product = new Product(); product.setName("Duplicate");
        product.setSku("catalog-001"); product.setSlug("another-unique-slug");
        assertThatThrownBy(() -> products.saveAndFlush(product)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(products.count()).isEqualTo(100);
    }

    @Test
    void cartSelectionsPersistAsRelatedRowsAndMergeInOneTransaction() {
        User user = new User(); user.setEmail("SqlCart@example.com"); user.setPassword("not-a-login-password");
        user.setFullName("SQL test"); user.setRole(roles.findByName("CUSTOMER").orElseThrow()); users.saveAndFlush(user);
        Product product = productService.findActiveProductBySlug(products.findBySku("CATALOG-016").orElseThrow().getSlug());
        String size = product.getSizes().getFirst();
        cartService.addItem("sqlcart@example.com", null, product.getId(), 1, size, "03 Bare Grape");
        cartService.addItem("sqlcart@example.com", null, product.getId(), 2, size, "05 Jujube");
        assertThat(jdbc.queryForObject("select count(*) from shopping_cart_items", Integer.class)).isEqualTo(2);
        assertThat(cartService.getCart("sqlcart@example.com", null).items()).hasSize(2);
        assertThat(cartService.getItemCount("sqlcart@example.com", null)).isEqualTo(3);
        cartService.removeItem("sqlcart@example.com", null, product.getId(), size, "03 Bare Grape");
        assertThat(jdbc.queryForObject("select count(*) from shopping_cart_items", Integer.class)).isEqualTo(1);
    }

    @Test
    void optimisticVersionRejectsStaleProductEdits() {
        Product first = products.findBySku("CATALOG-016").orElseThrow();
        Product stale = products.findBySku("CATALOG-016").orElseThrow();
        first.setPrice(first.getPrice().add(BigDecimal.ONE)); products.saveAndFlush(first);
        stale.setPrice(new BigDecimal("1"));
        assertThatThrownBy(() -> products.saveAndFlush(stale)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void queriesEscapeWildcardsAndSortPricesAsNumbers() {
        assertThat(products.searchCatalog(ProductStatus.ACTIVE, "", "%_", PageRequest.of(0, 12)).getTotalElements()).isZero();
        var page = products.searchCatalog(ProductStatus.ACTIVE, "", "Beauty of Joseon", PageRequest.of(0, 12, Sort.by("price")));
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getContent()).extracting(Product::getPrice).isSorted();
        assertThat(brands.count()).isEqualTo(59);
        roleSeed.run(); roleSeed.run();
        assertThat(roles.count()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select database()", String.class)).startsWith("oneshop_test_");
    }
}
