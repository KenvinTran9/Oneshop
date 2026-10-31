package com.oneshop.service;

import com.oneshop.dto.request.StoreForm;
import com.oneshop.entity.Store;
import com.oneshop.repository.StoreRepository;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
@PreAuthorize("hasRole('ADMIN')")
public class StoreManagementService {
    private final StoreRepository stores;
    public StoreManagementService(StoreRepository stores){this.stores=stores;}
    public List<Store> list(){return stores.findAll(org.springframework.data.domain.Sort.by("name"));}
    public StoreForm form(Long id){
        StoreForm f=new StoreForm(); f.setVersion(-1L);
        if(id==null)return f;
        Store s=stores.findById(id).orElseThrow(()->new IllegalArgumentException("Cửa hàng không tồn tại."));
        f.setCode(s.getCode()); f.setName(s.getName()); f.setAddress(s.getAddress()); f.setDistrict(s.getDistrict());
        f.setCity(s.getCity()); f.setPhone(s.getPhone()); f.setActive(s.isActive()); f.setVersion(s.getVersion()); return f;
    }
    @Transactional
    public void save(Long id,StoreForm f){
        String code=f.getCode().trim().toUpperCase(Locale.ROOT);
        if(stores.existsByCodeIgnoreCaseAndIdNot(code,id==null ? -1L:id))throw new IllegalArgumentException("Mã cửa hàng đã được sử dụng.");
        Store s=id==null?new Store():stores.lockById(id).orElseThrow(()->new IllegalArgumentException("Cửa hàng không tồn tại."));
        if(id!=null && !Objects.equals(s.getVersion(),f.getVersion()))throw new IllegalArgumentException("Thông tin vừa được thay đổi. Hãy tải lại trang trước khi sửa.");
        s.setCode(code); s.setName(f.getName().trim()); s.setAddress(f.getAddress().trim()); s.setDistrict(f.getDistrict());
        s.setCity(f.getCity()); s.setPhone(f.getPhone()); s.setActive(f.isActive()); stores.saveAndFlush(s);
    }
}
