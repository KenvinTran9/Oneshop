package com.oneshop.service;

import com.oneshop.dto.request.RegisterRequest;
import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.exception.DuplicateEmailException;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRegistrationService {

    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerRegistrationService(UserRepository userRepository,
                                       RoleRepository roleRepository,
                                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        Role customerRole = roleRepository.findByName(CUSTOMER_ROLE)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not configured"));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(request.phone().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(customerRole);
        user.setActive(true);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // The database unique constraint handles simultaneous registrations for the same email.
            throw new DuplicateEmailException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
