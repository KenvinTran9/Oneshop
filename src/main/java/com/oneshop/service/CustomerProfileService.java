package com.oneshop.service;

import com.oneshop.dto.request.*;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
@PreAuthorize("isAuthenticated()")
public class CustomerProfileService {
    private final UserRepository users;
    private final CustomerAddressRepository addresses;
    private final PasswordResetAccountRepository credentials;
    private final PasswordEncoder encoder;
    public CustomerProfileService(UserRepository users,CustomerAddressRepository addresses,PasswordResetAccountRepository credentials,PasswordEncoder encoder){
        this.users=users;this.addresses=addresses;this.credentials=credentials;this.encoder=encoder;
    }
    public User profile(String email){return users.findByEmail(email).filter(User::isActive).orElseThrow(()->new IllegalArgumentException("Tài khoản không còn hoạt động."));}
    public List<CustomerAddress> addresses(String email){return addresses.findByCustomerEmailOrderByIdDesc(email);}
    public CustomerAddress address(String email,Long id){return addresses.findByIdAndCustomerEmail(id,email).orElseThrow(()->new org.springframework.security.access.AccessDeniedException("Địa chỉ không thuộc tài khoản của bạn."));}
    @Transactional public void update(String email,ProfileForm form){
        User user=users.findForCartChange(email).filter(User::isActive).orElseThrow();
        user.setFullName(form.fullName().trim());user.setPhone(form.phone().trim());users.saveAndFlush(user);
    }
    @Transactional public void addAddress(String email,AddressForm form){
        User user=users.findForCartChange(email).filter(User::isActive).orElseThrow();
        if(addresses.countByCustomerId(user.getId())>=20)throw new IllegalArgumentException("Tối đa 20 địa chỉ; hãy xóa địa chỉ không còn sử dụng.");
        CustomerAddress a=new CustomerAddress();a.setCustomer(user);a.setLabel(form.label().trim());a.setRecipient(form.recipient().trim());a.setPhone(form.phone().trim());a.setAddress(form.address().trim());addresses.saveAndFlush(a);
    }
    @Transactional public void removeAddress(String email,Long id){
        users.findForCartChange(email).filter(User::isActive).orElseThrow();addresses.delete(address(email,id));
    }
    @Transactional public void changePassword(String email,ChangePasswordForm form){
        User user=users.findForCartChange(email).filter(User::isActive).orElseThrow();
        if(!encoder.matches(form.currentPassword(),user.getPassword()))throw new IllegalArgumentException("Mật khẩu hiện tại chưa đúng.");
        if(!form.isConfirmed() || form.newPassword().length()<8 || form.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Mật khẩu mới hoặc xác nhận chưa hợp lệ.");
        user.setPassword(encoder.encode(form.newPassword()));
        var state=credentials.findForUpdate(user.getId()).orElseGet(()->new PasswordResetAccount(user));
        state.setTokenHash(null);state.setExpiresAt(null);state.setCredentialVersion(Math.addExact(state.getCredentialVersion(),1));
        credentials.saveAndFlush(state);users.saveAndFlush(user);
    }
}
