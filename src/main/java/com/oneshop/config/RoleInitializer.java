package com.oneshop.config;

import com.oneshop.entity.Role;
import com.oneshop.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Required authorization roles are application data; demo accounts remain dev-only. */
@Component
@Order(0)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed.enabled", matchIfMissing = true)
public class RoleInitializer implements CommandLineRunner {
    private final RoleRepository roles;
    public RoleInitializer(RoleRepository roles) { this.roles = roles; }

    public void run(String... args) {
        for (String name : java.util.List.of("ADMIN", "CUSTOMER", "BRANCH_MANAGER")) {
            if (roles.findByName(name).isPresent()) continue;
            Role role = new Role(); role.setName(name); role.setDescription(name);
            try { roles.save(role); }
            catch (DataIntegrityViolationException alreadyCreated) {
                if (roles.findByName(name).isEmpty()) throw alreadyCreated;
            }
        }
    }
}
