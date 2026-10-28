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

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(toAuthority(user.getRole()))))
                .disabled(!user.isActive())
                .build();
    }

    private String toAuthority(Role role) {
        if (role == null || role.getName() == null || role.getName().isBlank()) {
            return "ROLE_CUSTOMER";
        }
        return role.getName().startsWith("ROLE_") ? role.getName() : "ROLE_" + role.getName();
    }
}
