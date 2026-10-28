package com.oneshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oneshop.config.CatalogDataInitializer;
import com.oneshop.config.DevStockInitializer;
import com.oneshop.config.RoleInitializer;
import com.oneshop.entity.Brand;
import com.oneshop.entity.Cart;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.entity.Store;
import com.oneshop.entity.User;
import com.oneshop.repository.*;
import com.oneshop.service.CartService;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.dev.seed-stock=true")
@ActiveProfiles({"dev", "test"})
class MongoPersistenceTests extends MongoIntegrationTestSupport {
    @Autowired MongoTemplate mongo;
    @Autowired ProductRepository products;
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
    void seedLocalFixture() throws Exception {
        roleSeed.run();
        Store store = new Store(); store.setCode("HCM-D1"); store.setName("Local test shop");
        store.setAddress("Test address"); stores.save(store);
        catalog.run(); stockSeed.run();
    }

    @Test
    void storesNativeDecimalPricesAndNumericReferences() {
        Product product = products.findBySku("CATALOG-016").orElseThrow();
        Document raw = mongo.getCollection("products").find(new Document("_id", product.getId())).first();
        assertThat(raw.get("price")).isInstanceOf(Decimal128.class);
        assertThat(raw.get("brand")).isInstanceOf(Number.class);
        assertThat(raw.get("category")).isInstanceOf(Number.class);
        assertThat(raw.getList("shades", Document.class)).hasSize(3);
        assertThat(new MongoTemplate(mongo.getMongoDatabaseFactory(), mongo.getConverter())
                .findById(product.getId(), Product.class).getBrand().getName()).isEqualTo("rom&nd");
    }

    @Test
    void stockSeedIsIdempotentAndKeepsAdministratorQuantitiesIncludingZero() throws Exception {
        assertThat(stocks.count()).isEqualTo(100);
        var stock = stocks.findByProductId(products.findBySku("CATALOG-016").orElseThrow().getId()).getFirst();
        stock.setQuantity(0); stocks.save(stock);
        Long originalId = stock.getId();
        catalog.run(); stockSeed.run();
        assertThat(products.count()).isEqualTo(100);
        assertThat(stocks.count()).isEqualTo(100);
        assertThat(stocks.findById(originalId).orElseThrow().getQuantity()).isZero();
        assertThat(stocks.sumAvailableQuantityByProductId(products.findBySku("CATALOG-001").orElseThrow().getId())).isEqualTo(20);
    }

    @Test
    void inactiveStoresDoNotContributeToAvailableStock() {
        Product product = products.findBySku("CATALOG-016").orElseThrow();
        Store store = stores.findByCode("HCM-D1").orElseThrow();
        store.setActive(false); stores.save(store);
        assertThat(stocks.sumAvailableQuantityByProductId(product.getId())).isZero();
    }

    @Test
    void uniqueIndexesRejectConcurrentStyleCaseVariants() {
        Product product = new Product(); product.setName("Duplicate"); product.setSku("catalog-001"); product.setSlug("another-unique-slug");
        assertThatThrownBy(() -> products.save(product)).isInstanceOf(DuplicateKeyException.class);
        assertThat(products.count()).isEqualTo(100);
    }

    @Test
    void cartChoicesAreEmbeddedInOneVersionedDocumentAndSurviveReloads() {
        User user = new User(); user.setEmail("MongoCart@example.com"); user.setPassword("not-a-login-password");
        user.setFullName("Mongo test"); user.setRole(roles.findByName("CUSTOMER").orElseThrow()); users.save(user);
        Product product = products.findBySku("CATALOG-016").orElseThrow();
        String size = product.getSizes().getFirst();
        cartService.addItem("mongocart@example.com", null, product.getId(), 1, size, "03 Bare Grape");
        cartService.addItem("mongocart@example.com", null, product.getId(), 2, size, "05 Jujube");
        Document raw = mongo.getCollection("shopping_carts").find(new Document("user", user.getId())).first();
        assertThat(raw.getList("items", Document.class)).hasSize(2);
        assertThat(raw.get("version")).isInstanceOf(Number.class);
        assertThat(mongo.collectionExists("shopping_cart_items")).isFalse();
        Cart first = carts.findByUserEmailIgnoreCase(user.getEmail()).orElseThrow();
        Cart stale = carts.findByUserEmailIgnoreCase(user.getEmail()).orElseThrow();
        first.getItems().getFirst().setQuantity(3); carts.save(first);
        stale.getItems().clear();
        assertThatThrownBy(() -> carts.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(cartService.getCart(user.getEmail(), null).items()).hasSize(2);
        assertThat(cartService.getItemCount(user.getEmail(), null)).isEqualTo(5);
    }

    @Test
    void nativeQueriesEscapeSearchInputAndPreserveNumericSorting() {
        assertThat(products.searchCatalog(ProductStatus.ACTIVE, "", ".*", PageRequest.of(0, 12)).getTotalElements()).isZero();
        var page = products.searchCatalog(ProductStatus.ACTIVE, "", "Beauty of Joseon", PageRequest.of(0, 12, Sort.by("price")));
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getContent()).extracting(Product::getPrice).isSorted();
        assertThat(products.summarizeHomeCounts(ProductStatus.ACTIVE).getBrandCount()).isEqualTo(59);
    }

    @Test
    void identifiersContinueAcrossRepositoryInstancesAndRolesAreNotDuplicated() {
        Brand a = new Brand(); a.setName("A"); a.setSlug("sequence-a"); brands.save(a);
        Brand b = new Brand(); b.setName("B"); b.setSlug("sequence-b"); brands.save(b);
        assertThat(b.getId()).isGreaterThan(a.getId());
        roleSeed.run(); roleSeed.run();
        assertThat(roles.count()).isEqualTo(2);
        assertThat(mongo.getDb().getName()).startsWith("oneshop_test_");
    }
}
