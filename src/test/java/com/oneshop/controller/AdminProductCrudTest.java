package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Order;
import com.oneshop.entity.OrderItem;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.repository.BrandRepository;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.CartItemRepository;
import com.oneshop.repository.CartRepository;
import com.oneshop.repository.OrderItemRepository;
import com.oneshop.repository.OrderRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.StoreStockRepository;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.CloudinaryService;
import com.oneshop.service.AdminService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductCrudTest {

    private static final String ADMIN = "admin@oneshop.test";

    @Autowired private MockMvc mockMvc;
    @Autowired private AdminService adminService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private StoreStockRepository storeStockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private RoleRepository roleRepository;
    @MockBean private CloudinaryService cloudinaryService;

    private Category category;
    private Brand brand;

    @BeforeEach
    void setUp() {
        reset(cloudinaryService);
        clearFixtures();

        category = new Category();
        category.setName("Chăm sóc da");
        category.setSlug("cham-soc-da-admin-test");
        category = categoryRepository.saveAndFlush(category);

        brand = new Brand();
        brand.setName("One Beauty");
        brand.setSlug("one-beauty-admin-test");
        brand = brandRepository.saveAndFlush(brand);
    }

    @AfterEach
    void cleanFixtures() {
        clearFixtures();
    }

    @Test
    void onlyAdminCanOpenProductManagement() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/products").with(user("customer@test.local").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/products").with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"));
        mockMvc.perform(get("/admin/products/new").with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeExists("productForm", "categories", "brands"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void serviceAlsoRejectsNonAdminAccess() {
        assertThatThrownBy(() -> adminService.findProducts(PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createsProductWithValidatedFormAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/products").with(user(ADMIN).roles("ADMIN")).with(csrf())
                        .param("name", "  Serum mới  ").param("slug", "SERUM-MOI")
                        .param("sku", "serum-new-001").param("price", "125000.50")
                        .param("categoryId", category.getId().toString())
                        .param("brandId", brand.getId().toString()).param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        Product saved = productRepository.findAll().getFirst();
        assertThat(saved.getName()).isEqualTo("Serum mới");
        assertThat(saved.getSlug()).isEqualTo("serum-moi");
        assertThat(saved.getSku()).isEqualTo("SERUM-NEW-001");
        assertThat(saved.getPrice()).isEqualByComparingTo("125000.50");
        assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        assertThat(saved.getBrand().getId()).isEqualTo(brand.getId());
    }

    @Test
    void rejectsDuplicateSkuAndSlug() throws Exception {
        persistProduct("existing-product", "EXISTING-SKU");

        mockMvc.perform(post("/admin/products").with(user(ADMIN).roles("ADMIN")).with(csrf())
                        .param("name", "SKU trùng").param("slug", "sku-trung")
                        .param("sku", "existing-sku").param("price", "10000")
                        .param("categoryId", category.getId().toString()).param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "sku"));

        mockMvc.perform(post("/admin/products").with(user(ADMIN).roles("ADMIN")).with(csrf())
                        .param("name", "Slug trùng").param("slug", "EXISTING-PRODUCT")
                        .param("sku", "NEW-SKU").param("price", "10000")
                        .param("categoryId", category.getId().toString()).param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "slug"));
    }

    @Test
    void rejectsInvalidPriceAndMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/admin/products").with(user(ADMIN).roles("ADMIN")).with(csrf())
                        .param("name", "").param("slug", "Invalid Slug")
                        .param("sku", "SKU bad").param("price", "-10")
                        .param("categoryId", "").param("status", ""))
                .andExpect(status().isOk()).andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "name", "slug", "sku", "price", "categoryId", "status"));
        assertThat(productRepository.count()).isZero();
    }

    @Test
    void updatesProductWithoutBindingSystemFields() throws Exception {
        Product product = persistProduct("old-name", "OLD-SKU");
        LocalDateTime originalCreatedAt = product.getCreatedAt();
        mockMvc.perform(get("/admin/products/{id}", product.getId()).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/product-detail"));
        mockMvc.perform(get("/admin/products/{id}/edit", product.getId()).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/product-form"));
        mockMvc.perform(post("/admin/products/{id}", product.getId()).with(user(ADMIN).roles("ADMIN")).with(csrf())
                        .param("name", "Tên mới").param("slug", "ten-moi")
                        .param("sku", "NEW-SKU").param("price", "200000")
                        .param("categoryId", category.getId().toString()).param("status", "DRAFT")
                        .param("active", "true").param("createdAt", "2000-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products/" + product.getId()));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Tên mới");
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    void canDisableAndReenableBusinessStatus() throws Exception {
        Product product = persistProduct("toggle-product", "TOGGLE-SKU");
        mockMvc.perform(post("/admin/products/{id}/status", product.getId())
                        .with(user(ADMIN).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        Product disabled = productRepository.findById(product.getId()).orElseThrow();
        assertThat(disabled.isActive()).isFalse();
        assertThat(disabled.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);

        mockMvc.perform(post("/admin/products/{id}/status", product.getId())
                        .with(user(ADMIN).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void deactivationNeverHardDeletesProductAlreadyReferencedByAnOrder() throws Exception {
        Product product = persistProduct("ordered-product", "ORDERED-SKU");
        Order order = new Order();
        order.setCode("TEST-ORDER-001");
        order.setCustomerName("Test Customer");
        order.setCustomerPhone("0912345678");
        order.setShippingAddress("Test address");
        order.setTotalAmount(new BigDecimal("50000"));
        order = orderRepository.saveAndFlush(order);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("50000"));
        item.setLineTotal(new BigDecimal("50000"));
        orderItemRepository.saveAndFlush(item);

        mockMvc.perform(post("/admin/products/{id}/deactivate", product.getId())
                        .with(user(ADMIN).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        Product archived = productRepository.findById(product.getId()).orElseThrow();
        assertThat(archived.isActive()).isFalse();
        assertThat(archived.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        assertThat(orderItemRepository.findByOrderId(order.getId())).singleElement()
                .extracting(orderItem -> orderItem.getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    void uploadsImageThroughCloudinaryServiceAndPreservesExistingDataWhenUploadFails() throws Exception {
        Product product = persistProduct("image-old-name", "IMAGE-OLD-SKU");
        product.setImageUrl("https://images.example.test/original.jpg");
        productRepository.saveAndFlush(product);
        MockMultipartFile image = pngImage();

        when(cloudinaryService.uploadProductImage(any())).thenReturn("https://images.example.test/new.png");
        mockMvc.perform(multipart("/admin/products/{id}", product.getId()).file(image)
                        .with(user(ADMIN).roles("ADMIN")).with(csrf())
                        .param("name", "Ảnh đã đổi").param("slug", "anh-da-doi")
                        .param("sku", "IMAGE-NEW-SKU").param("price", "75000")
                        .param("categoryId", category.getId().toString()).param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection());
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getImageUrl()).isEqualTo("https://images.example.test/new.png");

        doThrow(new IOException("simulated upload failure"))
                .when(cloudinaryService).uploadProductImage(any());
        mockMvc.perform(multipart("/admin/products/{id}", product.getId()).file(pngImage())
                        .with(user(ADMIN).roles("ADMIN")).with(csrf())
                        .param("name", "Không được lưu").param("slug", "khong-duoc-luu")
                        .param("sku", "IMAGE-FAILED-SKU").param("price", "99000")
                        .param("categoryId", category.getId().toString()).param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "image"));

        Product unchanged = productRepository.findById(product.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Ảnh đã đổi");
        assertThat(unchanged.getImageUrl()).isEqualTo("https://images.example.test/new.png");
    }

    @Test
    void postActionsRequireCsrf() throws Exception {
        mockMvc.perform(post("/admin/products").with(user(ADMIN).roles("ADMIN"))
                        .param("name", "No csrf").param("slug", "no-csrf").param("sku", "NO-CSRF")
                        .param("price", "10000").param("categoryId", category.getId().toString()).param("status", "ACTIVE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/products").with(user(ADMIN).roles("ADMIN")).with(csrf().useInvalidToken())
                        .param("name", "Invalid csrf").param("slug", "invalid-csrf").param("sku", "INVALID-CSRF")
                        .param("price", "10000").param("categoryId", category.getId().toString()).param("status", "ACTIVE"))
                .andExpect(status().isForbidden());
        assertThat(productRepository.count()).isZero();
    }

    private Product persistProduct(String slug, String sku) {
        Product product = new Product();
        product.setName("Test Product");
        product.setSlug(slug);
        product.setSku(sku);
        product.setPrice(new BigDecimal("50000"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        product.setBrand(brand);
        return productRepository.saveAndFlush(product);
    }

    private MockMultipartFile pngImage() {
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        return new MockMultipartFile("image", "product.png", "image/png", pngSignature);
    }

    private void clearFixtures() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        storeStockRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();
        roleRepository.deleteAll();
    }
}
