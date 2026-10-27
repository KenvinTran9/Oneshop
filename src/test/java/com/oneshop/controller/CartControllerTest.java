package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.oneshop.entity.Cart;
import com.oneshop.entity.CartItem;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.entity.Role;
import com.oneshop.entity.Store;
import com.oneshop.entity.StoreStock;
import com.oneshop.entity.User;
import com.oneshop.repository.CartItemRepository;
import com.oneshop.repository.CartRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.StoreStockRepository;
import com.oneshop.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

    private static final String USER_EMAIL = "cart-customer@example.com";
    private static final String PASSWORD = "CartPassword!42";

    @Autowired private MockMvc mockMvc;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreStockRepository storeStockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Product product;
    private Store store;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        storeStockRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setName("CUSTOMER");
        role.setDescription("Customer");
        roleRepository.saveAndFlush(role);

        User account = new User();
        account.setEmail(USER_EMAIL);
        account.setFullName("Cart Customer");
        account.setPhone("0912345678");
        account.setPassword(passwordEncoder.encode(PASSWORD));
        account.setRole(role);
        userRepository.saveAndFlush(account);

        store = new Store();
        store.setCode("CART-TEST");
        store.setName("Cart Test Store");
        store.setAddress("1 Test Street");
        store = storeRepository.saveAndFlush(store);

        product = new Product();
        product.setName("Test Serum");
        product.setSlug("test-serum-cart");
        product.setSku("SERUM-CART-001");
        product.setPrice(new BigDecimal("100000.00"));
        product.setSalePrice(new BigDecimal("80000.00"));
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.saveAndFlush(product);
        setStock(10);
    }

    @AfterEach
    void cleanCartFixtures() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    void displaysCartPage() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/cart"));
    }

    @Test
    void guestCanAddSkuAndAddingSameSkuIncrementsQuantity() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addAsGuest(session, 1);
        addAsGuest(session, 2);

        var result = mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cart"))
                .andReturn();
        com.oneshop.dto.response.CartView cart = (com.oneshop.dto.response.CartView)
                result.getModelAndView().getModel().get("cart");
        assertThat(cart.itemCount()).isEqualTo(3L);
        assertThat(cart.items()).singleElement().extracting("unitPrice").isEqualTo(new BigDecimal("80000.00"));
        assertThat(session.getAttribute("oneshop.guestCart")).isNotNull();
    }

    @Test
    void authenticatedCustomerCanUpdateAndRemoveOwnSku() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .with(user(USER_EMAIL).roles("CUSTOMER"))
                        .with(csrf()).param("productId", product.getId().toString()).param("quantity", "2"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/cart/items/{id}", product.getId())
                        .with(user(USER_EMAIL).roles("CUSTOMER"))
                        .with(csrf()).param("quantity", "4"))
                .andExpect(status().is3xxRedirection());
        assertThat(cartRepository.findByUserEmailIgnoreCase(USER_EMAIL).orElseThrow().getItems())
                .singleElement().extracting("quantity").isEqualTo(4);

        mockMvc.perform(post("/cart/items/{id}/remove", product.getId())
                        .with(user(USER_EMAIL).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(cartItemRepository.count()).isZero();
    }

    @Test
    void rejectsOutOfStockAndDiscontinuedProducts() throws Exception {
        setStock(0);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/cart/items").session(session).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("cartError"));
        assertThat(session.getAttribute("oneshop.guestCart")).isNull();

        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.saveAndFlush(product);
        mockMvc.perform(post("/cart/items").session(new MockHttpSession()).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("cartError"));

        product.setStatus(ProductStatus.ACTIVE);
        productRepository.saveAndFlush(product);
        setStock(10);
        MockHttpSession existingCart = new MockHttpSession();
        addAsGuest(existingCart, 1);
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.saveAndFlush(product);
        var cartPage = mockMvc.perform(get("/cart").session(existingCart)).andExpect(status().isOk()).andReturn();
        com.oneshop.dto.response.CartView cart = (com.oneshop.dto.response.CartView)
                cartPage.getModelAndView().getModel().get("cart");
        assertThat(cart.items()).singleElement().extracting("warning")
                .asString().contains("ngừng kinh doanh");
    }

    @Test
    void loginMergesGuestQuantitiesAndClearsGuestCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addAsGuest(session, 2);
        seedAccountCart(1);

        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", USER_EMAIL).param("password", PASSWORD))
                .andExpect(status().is3xxRedirection());

        Cart cart = cartRepository.findByUserEmailIgnoreCase(USER_EMAIL).orElseThrow();
        assertThat(cart.getItems()).singleElement().extracting("quantity").isEqualTo(3);
        assertThat(session.getAttribute("oneshop.guestCart")).isNull();
    }

    @Test
    void clearCartRemovesEveryOwnedItem() throws Exception {
        mockMvc.perform(post("/cart/items").with(user(USER_EMAIL).roles("CUSTOMER")).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "2"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/cart/clear").with(user(USER_EMAIL).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(cartItemRepository.count()).isZero();
    }

    @Test
    void customerCannotModifyAnotherCustomersCart() throws Exception {
        mockMvc.perform(post("/cart/items").with(user(USER_EMAIL).roles("CUSTOMER")).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "2"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/cart/items/{id}/remove", product.getId())
                        .with(user("other@example.com").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(cartItemRepository.count()).isEqualTo(1);
        assertThat(cartRepository.findByUserEmailIgnoreCase(USER_EMAIL).orElseThrow().getItems())
                .singleElement().extracting("quantity").isEqualTo(2);
    }

    @Test
    void cartMutationsRequireValidCsrfToken() throws Exception {
        mockMvc.perform(post("/cart/items").param("productId", product.getId().toString()).param("quantity", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/cart/clear").with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        assertThat(cartRepository.count()).isZero();
    }

    private void addAsGuest(HttpSession session, int quantity) throws Exception {
        mockMvc.perform(post("/cart/items").session((MockHttpSession) session).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", Integer.toString(quantity)))
                .andExpect(status().is3xxRedirection());
    }

    private void setStock(int quantity) {
        StoreStock stock = storeStockRepository.findByStoreId(store.getId()).stream().findFirst()
                .orElseGet(StoreStock::new);
        stock.setStore(store);
        stock.setProduct(product);
        stock.setQuantity(quantity);
        storeStockRepository.saveAndFlush(stock);
    }

    private void seedAccountCart(int quantity) {
        Cart cart = new Cart();
        cart.setUser(userRepository.findByEmail(USER_EMAIL).orElseThrow());
        cart = cartRepository.saveAndFlush(cart);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        cartItemRepository.saveAndFlush(item);
    }
}
