package com.oneshop.service;

import com.oneshop.entity.Store;
import com.oneshop.repository.StoreRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<Store> findActiveStores() {
        return storeRepository.findByActiveTrueOrderByNameAsc();
    }
}
