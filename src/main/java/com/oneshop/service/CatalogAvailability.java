package com.oneshop.service;

import com.oneshop.entity.Product;
import com.oneshop.repository.StoreStockRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class CatalogAvailability {
    private final StoreStockRepository stocks;
    public CatalogAvailability(StoreStockRepository stocks) { this.stocks = stocks; }
    public void load(List<Product> products) {
        if (products.isEmpty()) return;
        var quantities = new HashMap<Long,Long>();
        stocks.availability(products.stream().map(Product::getId).toList()).forEach(row -> quantities.put(row.getProductId(), row.getQuantity()));
        products.forEach(p -> p.setAvailableQuantity(quantities.getOrDefault(p.getId(),0L)));
    }
}
