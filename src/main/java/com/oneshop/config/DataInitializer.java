package com.oneshop.config;

import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.entity.Role;
import com.oneshop.entity.Store;
import com.oneshop.entity.StoreStock;
import com.oneshop.entity.User;
import com.oneshop.repository.BrandRepository;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.StoreStockRepository;
import com.oneshop.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
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
                               CategoryRepository categoryRepository,
                               BrandRepository brandRepository,
                               StoreRepository storeRepository,
                               ProductRepository productRepository,
                               StoreStockRepository storeStockRepository,
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

            Category skincare = categoryRepository.findBySlug("cham-soc-da").orElseGet(() -> {
                Category category = new Category();
                category.setName("Cham soc da");
                category.setSlug("cham-soc-da");
                category.setDescription("San pham lam sach, duong am va phuc hoi da");
                return categoryRepository.save(category);
            });

            Category makeup = categoryRepository.findBySlug("trang-diem").orElseGet(() -> {
                Category category = new Category();
                category.setName("Trang diem");
                category.setSlug("trang-diem");
                category.setDescription("My pham trang diem moi ngay");
                return categoryRepository.save(category);
            });

            Brand oneskin = brandRepository.findBySlug("oneskin").orElseGet(() -> {
                Brand brand = new Brand();
                brand.setName("OneSkin Lab");
                brand.setSlug("oneskin");
                brand.setDescription("Thuong hieu cham soc da doc quyen cua OneShop");
                return brandRepository.save(brand);
            });

            Brand glow = brandRepository.findBySlug("glow-beauty").orElseGet(() -> {
                Brand brand = new Brand();
                brand.setName("Glow Beauty");
                brand.setSlug("glow-beauty");
                brand.setDescription("Dong trang diem nhe nhe cho khach hang tre");
                return brandRepository.save(brand);
            });

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

            if (productRepository.count() == 0) {
                Product serum = product("Serum cap am Hyaluronic",
                        "serum-cap-am-hyaluronic",
                        "OS-SERUM-001",
                        "Tinh chat cap am mong nhe cho moi loai da.",
                        new BigDecimal("329000"),
                        new BigDecimal("289000"),
                        "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&w=900&q=80",
                        skincare,
                        oneskin);

                Product cleanser = product("Sua rua mat diu nhe",
                        "sua-rua-mat-diu-nhe",
                        "OS-CLEAN-002",
                        "Lam sach bui ban va dau thua ma khong gay kho da.",
                        new BigDecimal("189000"),
                        null,
                        "https://images.unsplash.com/photo-1556229010-6c3f2c9ca5f8?auto=format&fit=crop&w=900&q=80",
                        skincare,
                        oneskin);

                Product lipstick = product("Son kem min Velvet Rose",
                        "son-kem-min-velvet-rose",
                        "GB-LIP-003",
                        "Chat son mem, len mau tu nhien cho moi ngay.",
                        new BigDecimal("249000"),
                        new BigDecimal("219000"),
                        "https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=900&q=80",
                        makeup,
                        glow);

                List<Product> products = productRepository.saveAll(List.of(serum, cleanser, lipstick));
                createStock(storeStockRepository, districtOne, products.get(0), 24);
                createStock(storeStockRepository, districtOne, products.get(1), 18);
                createStock(storeStockRepository, thuDuc, products.get(0), 12);
                createStock(storeStockRepository, thuDuc, products.get(2), 30);
            }

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

    private Product product(String name,
                            String slug,
                            String sku,
                            String shortDescription,
                            BigDecimal price,
                            BigDecimal salePrice,
                            String imageUrl,
                            Category category,
                            Brand brand) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setSku(sku);
        product.setShortDescription(shortDescription);
        product.setDescription(shortDescription + " Phu hop lam san pham mau cho khung suon OneShop.");
        product.setPrice(price);
        product.setSalePrice(salePrice);
        product.setImageUrl(imageUrl);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        product.setBrand(brand);
        return product;
    }

    private void createStock(StoreStockRepository repository, Store store, Product product, int quantity) {
        StoreStock stock = new StoreStock();
        stock.setStore(store);
        stock.setProduct(product);
        stock.setQuantity(quantity);
        repository.save(stock);
    }
}
