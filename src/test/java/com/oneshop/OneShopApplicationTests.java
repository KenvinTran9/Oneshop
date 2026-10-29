package com.oneshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OneShopApplicationTests extends com.oneshop.MySqlIntegrationTestSupport {
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext context;

    @Test
    void contextLoads() {
        org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(com.oneshop.config.DevStockInitializer.class)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(com.oneshop.config.CatalogDataInitializer.class)).isEmpty();
    }
}
