package com.oneshop.service;

import com.oneshop.dto.request.InventoryChangeForm;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import com.oneshop.security.StoreAccess;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
public class InventoryService {
    private final StoreRepository stores;
    private final ProductRepository products;
    private final StoreStockRepository stocks;
    private final StockMovementRepository movements;
    private final StoreAccess access;
    public InventoryService(StoreRepository stores, ProductRepository products, StoreStockRepository stocks,
                            StockMovementRepository movements, StoreAccess access) {
        this.stores = stores; this.products = products; this.stocks = stocks; this.movements = movements; this.access = access;
    }
    public List<Store> allowedStores() {
        Long scope = access.scope(null);
        return scope == null ? stores.findAll(org.springframework.data.domain.Sort.by("name")) : List.of(stores.findById(scope).orElseThrow());
    }
    public Page<StoreStock> inventory(Long storeId, String query, boolean low, int page) {
        access.requireStore(storeId);
        return stocks.search(storeId, query == null ? "" : query.trim(), low, PageRequest.of(Math.max(0,page),20));
    }
    public List<Product> products() { return products.findAll(org.springframework.data.domain.Sort.by("name")); }
    public Page<StockMovement> history(Long storeId, int page) {
        access.requireStore(storeId);
        return movements.findByStoreId(storeId, PageRequest.of(Math.max(0,page),20,Sort.by("id").descending()));
    }
    @Transactional
    public void change(InventoryChangeForm form) {
        access.requireStore(form.getStoreId());
        if (form.getDelta() == null || form.getDelta() == 0 || Math.abs((long)form.getDelta()) > 100000
                || form.getReason() == null || form.getReason().isBlank() || form.getReason().length() > 300
                || form.getRequestToken() == null || !form.getRequestToken().matches("[a-fA-F0-9-]{36}"))
            throw new IllegalArgumentException("Số lượng thay đổi phải khác 0, tối đa 100.000; cần nhập lý do hợp lệ.");
        // Lock the parent to serialize creation of a missing stock row; checkout locks the existing stock row.
        Store store = stores.lockById(form.getStoreId()).filter(Store::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Cửa hàng đã ngừng hoạt động."));
        var previous = movements.findByRequestToken(form.getRequestToken());
        if (previous.isPresent()) {
            var p = previous.get();
            if (!p.getStore().getId().equals(store.getId()) || !p.getProduct().getId().equals(form.getProductId())
                    || p.getDelta() != form.getDelta() || !p.getActorEmail().equals(access.current().getEmail()))
                throw new IllegalArgumentException("Phiếu đã được sử dụng. Vui lòng tải lại trang.");
            return;
        }
        Product product = products.findById(form.getProductId()).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
        var rows = stocks.lockForOrder(store.getId(), List.of(product.getId()));
        StoreStock stock = rows.isEmpty() ? new StoreStock() : rows.getFirst();
        stock.setStore(store); stock.setProduct(product);
        int before = stock.getQuantity();
        int after = Math.addExact(before, form.getDelta());
        if (after < 0 || after > 1000000) throw new IllegalArgumentException("Tồn kho sau thay đổi phải từ 0 đến 1.000.000.");
        stock.setQuantity(after); stock.setActive(true); stocks.saveAndFlush(stock);
        StockMovement movement = new StockMovement(); movement.setStore(store); movement.setProduct(product);
        movement.setActorEmail(access.current().getEmail()); movement.setReason(form.getReason().trim());
        movement.setRequestToken(form.getRequestToken()); movement.setDelta(form.getDelta());
        movement.setQuantityBefore(before); movement.setQuantityAfter(after); movements.saveAndFlush(movement);
    }
}
