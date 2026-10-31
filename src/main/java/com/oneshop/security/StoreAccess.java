package com.oneshop.security;

import com.oneshop.entity.User;
import com.oneshop.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves authorization from current database state, never from a submitted store id. */
@Component
public class StoreAccess {
    private final UserRepository users;
    public StoreAccess(UserRepository users) { this.users = users; }
    public User current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Vui lòng đăng nhập.");
        return users.findByEmail(auth.getName()).filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không còn hoạt động."));
    }
    public boolean isAdmin(User user) { return user.getRole() != null && "ADMIN".equals(user.getRole().getName()); }
    public Long scope(Long requestedStore) {
        User user = current();
        if (isAdmin(user)) return requestedStore;
        if (user.getRole() == null || !"BRANCH_MANAGER".equals(user.getRole().getName())
                || user.getStore() == null || !user.getStore().isActive())
            throw new AccessDeniedException("Bạn không có quyền quản lý cửa hàng.");
        Long own = user.getStore().getId();
        if (requestedStore != null && !own.equals(requestedStore))
            throw new AccessDeniedException("Bạn chỉ được thao tác tại cửa hàng được phân công.");
        return own;
    }
    public void requireStore(Long storeId) {
        if (storeId == null) throw new AccessDeniedException("Chưa xác định cửa hàng.");
        scope(storeId);
    }
}
