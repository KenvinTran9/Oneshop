package com.oneshop.config;

import com.oneshop.entity.Role;
import com.oneshop.entity.Store;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               StoreRepository storeRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
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

            if (!userRepository.existsByEmail("admin@oneshop.vn")) {
                User admin = new User();
                admin.setEmail("admin@oneshop.vn");
                admin.setPassword(passwordEncoder.encode("123456"));
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

            if (!userRepository.existsByEmail("khachhang@oneshop.vn")) {
                User customer = new User();
                customer.setEmail("khachhang@oneshop.vn");
                customer.setPassword(passwordEncoder.encode("123456"));
                customer.setFullName("Khach hang mau");
                customer.setPhone("0911111111");
                customer.setRole(customerRole);
                userRepository.save(customer);
            }
        };
    }

}
