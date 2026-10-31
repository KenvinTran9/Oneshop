package com.oneshop.service;

import com.oneshop.entity.*;
import com.oneshop.repository.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
@PreAuthorize("hasRole('ADMIN')")
public class AccountManagementService {
    private final UserRepository users;
    private final StoreRepository stores;
    private final RoleRepository roles;
    private final PasswordResetAccountRepository credentials;
    public AccountManagementService(UserRepository users,StoreRepository stores,RoleRepository roles,PasswordResetAccountRepository credentials){
        this.users=users;this.stores=stores;this.roles=roles;this.credentials=credentials;
    }
    public Page<User> list(String q,int page){return users.search(q==null?"":q.trim(),PageRequest.of(Math.max(0,page),20));}
    public List<Store> stores(){return stores.findByActiveTrueOrderByNameAsc();}
    @Transactional
    public void update(Long id, String role,Long storeId,boolean active){
        if(!Set.of("CUSTOMER","BRANCH_MANAGER").contains(role))throw new IllegalArgumentException("Vai trò không hợp lệ.");
        String email=users.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy tài khoản.")).getEmail();
        User user=users.findForCartChange(email).orElseThrow();
        if(user.getRole()!=null && "ADMIN".equals(user.getRole().getName()))throw new IllegalArgumentException("Không thay đổi tài khoản quản trị tại màn hình này.");
        Store store="BRANCH_MANAGER".equals(role)?stores.findById(storeId==null?-1L:storeId).filter(Store::isActive)
                .orElseThrow(()->new IllegalArgumentException("Quản lý chi nhánh phải được gán cửa hàng đang hoạt động.")):null;
        boolean changed=user.isActive()!=active || user.getRole()==null || !role.equals(user.getRole().getName())
                || !Objects.equals(user.getStore()==null?null:user.getStore().getId(),store==null?null:store.getId());
        if(!changed)return;
        user.setRole(roles.findByName(role).orElseThrow());user.setStore(store);user.setActive(active);
        var state=credentials.findForUpdate(id).orElseGet(()->new PasswordResetAccount(user));
        state.setCredentialVersion(Math.addExact(state.getCredentialVersion(),1));state.setTokenHash(null);state.setExpiresAt(null);
        credentials.saveAndFlush(state);users.saveAndFlush(user);
    }
}
