package com.oneshop.security;

import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final com.oneshop.repository.PasswordResetAccountRepository resetAccounts;

    public CustomUserDetailsService(UserRepository userRepository, com.oneshop.repository.PasswordResetAccountRepository resetAccounts) {
        this.userRepository = userRepository;
        this.resetAccounts = resetAccounts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new AccountUserDetails(user.getEmail(), user.getPassword(), user.isActive(),
                List.of(new SimpleGrantedAuthority(toAuthority(user.getRole()))),
                resetAccounts.findCredentialVersion(user.getEmail()).orElse(0L));
    }

    private String toAuthority(Role role) {
        if (role == null || role.getName() == null || role.getName().isBlank()) {
            return "ROLE_CUSTOMER";
        }
        return role.getName().startsWith("ROLE_") ? role.getName() : "ROLE_" + role.getName();
    }
}
