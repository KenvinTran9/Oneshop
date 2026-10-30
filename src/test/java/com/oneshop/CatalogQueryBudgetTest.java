package com.oneshop;

import static org.assertj.core.api.Assertions.assertThat;
import com.oneshop.config.CatalogDataInitializer;
import com.oneshop.service.HomeCatalogService;
import com.oneshop.service.ProductService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
@ActiveProfiles({"dev", "test"})
class CatalogQueryBudgetTest extends MySqlIntegrationTestSupport {
    @Autowired CatalogDataInitializer initializer;
    @Autowired HomeCatalogService home;
    @Autowired ProductService products;
    @Autowired EntityManagerFactory factory;
    @Autowired com.oneshop.repository.UserRepository users;
    @Autowired com.oneshop.repository.CartRepository carts;
    @Autowired com.oneshop.service.CartService cartService;
    @Test void publicCatalogUsesBoundedDatabaseRoundTrips() throws Exception {
        initializer.run();
        var statistics = factory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(home.load().suggestions()).hasSize(8);
        long homeQueries = statistics.getPrepareStatementCount();
        statistics.clear();
        assertThat(products.searchCatalog("", "", PageRequest.of(0,12)).getContent()).hasSize(12);
        long catalogQueries = statistics.getPrepareStatementCount();
        System.out.println("Catalog SQL budgets: home=" + homeQueries + ", listing=" + catalogQueries);
        assertThat(homeQueries).isLessThanOrEqualTo(6);
        assertThat(catalogQueries).isLessThanOrEqualTo(4);
        var user = new com.oneshop.entity.User(); user.setEmail("counter@oneshop.test"); user.setFullName("Counter test");
        user.setPassword("test-only"); user = users.saveAndFlush(user);
        var cart = new com.oneshop.entity.Cart(); cart.setUser(user);
        var item = new com.oneshop.entity.CartItem(); item.setCart(cart); item.setQuantity(4);
        item.setProduct(products.findActiveProducts(PageRequest.of(0,1)).getContent().getFirst());
        cart.getItems().add(item); carts.saveAndFlush(cart);
        statistics.clear();
        assertThat(cartService.getItemCount("COUNTER@ONESHOP.TEST", null)).isEqualTo(4);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getEntityLoadCount()).isZero();
    }
}
