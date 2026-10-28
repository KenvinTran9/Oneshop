package com.oneshop.config;

import com.oneshop.entity.StoreStock;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.StoreStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@Order(30)
@ConditionalOnProperty(name = "app.dev.seed-stock", havingValue = "true")
public class DevStockInitializer implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DevStockInitializer.class);
    private final ProductRepository products;
    private final StoreRepository stores;
    private final StoreStockRepository stocks;

    public DevStockInitializer(ProductRepository products, StoreRepository stores, StoreStockRepository stocks) {
        this.products = products; this.stores = stores; this.stocks = stocks;
    }

    public void run(String... args) {
        var store = stores.findByCode("HCM-D1");
        if (store.isEmpty()) return;
        int added = 0;
        for (int number = 1; number <= 100; number++) {
            var product = products.findBySku("CATALOG-%03d".formatted(number));
            if (product.isEmpty() || stocks.findByStoreIdAndProductId(store.get().getId(), product.get().getId()).isPresent()) continue;
            StoreStock stock = new StoreStock();
            stock.setStore(store.get()); stock.setProduct(product.get()); stock.setQuantity(20);
            stocks.save(stock);
            added++;
        }
        LOG.info("Created {} local demo stock records (20 units each); existing quantities kept", added);
    }
}
