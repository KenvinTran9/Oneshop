package com.oneshop.config;

import com.oneshop.entity.Role;
import com.oneshop.entity.Store;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed.enabled", matchIfMissing = true)
public class DataInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @org.springframework.core.annotation.Order(10)
    CommandLineRunner seedData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               StoreRepository storeRepository,
                               PasswordEncoder passwordEncoder,
                               Environment environment) {
        return args -> {
            String adminPassword = environment.getProperty("DEMO_ADMIN_PASSWORD");
            String customerPassword = environment.getProperty("DEMO_CUSTOMER_PASSWORD");

            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ADMIN");
                role.setDescription("Quan tri he thong OneShop");
                return roleRepository.save(role);
            });

            Role customerRole = roleRepository.findByName("CUSTOMER").orElseGet(() -> {
                Role role = new Role();
                role.setName("CUSTOMER");
                role.setDescription("Khach hang mua sam");
                return roleRepository.save(role);
            });

            if (!userRepository.existsByEmail("admin@oneshop.vn")
                    && hasDemoPassword(adminPassword, "DEMO_ADMIN_PASSWORD")) {
                User admin = new User();
                admin.setEmail("admin@oneshop.vn");
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setFullName("OneShop Admin");
                admin.setPhone("0900000000");
                admin.setRole(adminRole);
                userRepository.save(admin);
            }

            Store districtOne = storeRepository.findByCode("HCM-D1").orElseGet(() -> {
                Store store = new Store();
                store.setCode("HCM-D1");
                store.setName("OneShop Quan 1");
                store.setAddress("12 Nguyen Hue");
                store.setDistrict("Quan 1");
                store.setCity("TP. Ho Chi Minh");
                store.setPhone("028 1111 2222");
                return storeRepository.save(store);
            });

            Store thuDuc = storeRepository.findByCode("HCM-TD").orElseGet(() -> {
                Store store = new Store();
                store.setCode("HCM-TD");
                store.setName("OneShop Thu Duc");
                store.setAddress("45 Vo Van Ngan");
                store.setDistrict("Thu Duc");
                store.setCity("TP. Ho Chi Minh");
                store.setPhone("028 3333 4444");
                return storeRepository.save(store);
            });

            if (!userRepository.existsByEmail("khachhang@oneshop.vn")
                    && hasDemoPassword(customerPassword, "DEMO_CUSTOMER_PASSWORD")) {
                User customer = new User();
                customer.setEmail("khachhang@oneshop.vn");
                customer.setPassword(passwordEncoder.encode(customerPassword));
                customer.setFullName("Khach hang mau");
                customer.setPhone("0911111111");
                customer.setRole(customerRole);
                userRepository.save(customer);
            }
        };
    }

    private boolean hasDemoPassword(String password, String propertyName) {
        if (password != null && !password.isBlank()) {
            return true;
        }
        LOGGER.warn("Skipping demo account creation because {} is not configured", propertyName);
        return false;
    }

}
