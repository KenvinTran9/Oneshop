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
import com.oneshop.entity.ProductShade;
import com.oneshop.dto.response.CartView;
import java.util.List;
import java.util.Map;
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
    @Autowired private org.springframework.web.context.WebApplicationContext webContext;
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

    @Test
    void guestKeepsEachSizeAndShadeSeparateAndMergesOnlyAnExactSelection() throws Exception {
        enableChoices();
        MockHttpSession session = new MockHttpSession();
        addSelection(session, "3.5 g", "03 Bare Grape", 1, false);
        addSelection(session, "3.5 g", "05 Jujube", 2, false);
        addSelection(session, "7 g", "03 Bare Grape", 1, false);
        addSelection(session, "3.5 g", "03 Bare Grape", 2, false);
        CartView cart = cartView(session, false);
        assertThat(cart.items()).hasSize(3);
        assertThat(cart.itemCount()).isEqualTo(6);
        assertThat(cart.subtotal()).isEqualByComparingTo("480000");
        assertThat(cart.items().getFirst().quantity()).isEqualTo(3);
        assertThat(cart.items().getFirst().imageUrl()).endsWith("catalog-016-03-bare-grape.webp");
        assertThat(cart.items().get(1).imageUrl()).endsWith("catalog-016-05-jujube.webp");
        assertThat(cart.items().get(2).size()).isEqualTo("7 g");
    }

    @Test
    void guestUpdatesAndRemovesOnlyTheSpecifiedSelection() throws Exception {
        enableChoices();
        MockHttpSession session = new MockHttpSession();
        addSelection(session, "3.5 g", "03 Bare Grape", 1, false);
        addSelection(session, "3.5 g", "05 Jujube", 1, false);
        mockMvc.perform(post("/cart/items/{id}", product.getId()).session(session).with(csrf())
                        .param("quantity", "3").param("size", "3.5 g").param("shade", "03 Bare Grape"))
                .andExpect(flash().attributeExists("cartSuccess"));
        assertThat(cartView(session, false).items()).extracting("quantity").containsExactly(3, 1);
        mockMvc.perform(post("/cart/items/{id}/remove", product.getId()).session(session).with(csrf())
                        .param("size", "3.5 g").param("shade", "03 Bare Grape"))
                .andExpect(flash().attributeExists("cartSuccess"));
        assertThat(cartView(session, false).items()).singleElement().extracting("shade").isEqualTo("05 Jujube");
    }

    @Test
    void accountPersistsChoicesAndCannotRemoveAnotherAccountsSelection() throws Exception {
        enableChoices();
        MockHttpSession session = new MockHttpSession();
        addSelection(session, "3.5 g", "03 Bare Grape", 1, true);
        addSelection(session, "7 g", "05 Jujube", 2, true);
        mockMvc.perform(post("/cart/items/{id}/remove", product.getId()).with(csrf())
                        .with(user("other@example.com").roles("CUSTOMER"))
                        .param("size", "3.5 g").param("shade", "03 Bare Grape"))
                .andExpect(flash().attributeExists("cartError"));
        assertThat(cartView(session, true).items()).hasSize(2);
        mockMvc.perform(post("/cart/items/{id}", product.getId()).with(csrf())
                        .with(user(USER_EMAIL).roles("CUSTOMER"))
                        .param("size", "7 g").param("shade", "05 Jujube").param("quantity", "4"))
                .andExpect(flash().attributeExists("cartSuccess"));
        mockMvc.perform(post("/cart/items/{id}/remove", product.getId()).with(csrf())
                        .with(user(USER_EMAIL).roles("CUSTOMER"))
                        .param("size", "3.5 g").param("shade", "03 Bare Grape"))
                .andExpect(flash().attributeExists("cartSuccess"));
        assertThat(cartView(session, true).items()).singleElement().satisfies(item -> {
            assertThat(item.size()).isEqualTo("7 g");
            assertThat(item.shade()).isEqualTo("05 Jujube");
            assertThat(item.quantity()).isEqualTo(4);
        });
    }

    @Test
    void loginMergesMatchingChoicesAndPreservesOtherColors() throws Exception {
        enableChoices();
        MockHttpSession session = new MockHttpSession();
        addSelection(new MockHttpSession(), "3.5 g", "03 Bare Grape", 2, true);
        addSelection(session, "3.5 g", "03 Bare Grape", 1, false);
        addSelection(session, "3.5 g", "05 Jujube", 2, false);
        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", USER_EMAIL).param("password", PASSWORD))
                .andExpect(status().is3xxRedirection());
        CartView cart = cartView(session, true);
        assertThat(cart.items()).hasSize(2);
        assertThat(cart.items()).extracting("quantity").containsExactly(3, 2);
        assertThat(cart.items()).extracting("shade").containsExactly("03 Bare Grape", "05 Jujube");
        assertThat(session.getAttribute("oneshop.guestCart")).isNull();
    }

    @Test
    void stockChecksIncludeOtherColorsForGuestsAndAccounts() throws Exception {
        enableChoices();
        setStock(3);
        for (boolean account : List.of(false, true)) {
            MockHttpSession session = new MockHttpSession();
            addSelection(session, "3.5 g", "03 Bare Grape", 2, account);
            var request = post("/cart/items").session(session).with(csrf()).param("productId", product.getId().toString())
                    .param("quantity", "2").param("size", "3.5 g").param("shade", "05 Jujube");
            if (account) request.with(user(USER_EMAIL).roles("CUSTOMER"));
            mockMvc.perform(request).andExpect(flash().attributeExists("cartError"));
            addSelection(session, "3.5 g", "05 Jujube", 1, account);
            var update = post("/cart/items/{id}", product.getId()).session(session).with(csrf())
                    .param("quantity", "2").param("size", "3.5 g").param("shade", "05 Jujube");
            if (account) update.with(user(USER_EMAIL).roles("CUSTOMER"));
            mockMvc.perform(update).andExpect(flash().attributeExists("cartError"));
            assertThat(cartView(session, account).itemCount()).isEqualTo(3);
        }
    }

    @Test
    void rejectsMissingUnknownAndOversizedChoicesWithoutAddingALine() throws Exception {
        enableChoices();
        MockHttpSession session = new MockHttpSession();
        for (var fields : List.of(new String[]{"3.5 g", ""}, new String[]{"", "03 Bare Grape"},
                new String[]{"3.5 g", "not-a-color"}, new String[]{"unknown-size", "05 Jujube"},
                new String[]{"3.5 g", "x".repeat(101)})) {
            mockMvc.perform(post("/cart/items").session(session).with(csrf())
                            .param("productId", product.getId().toString()).param("quantity", "1")
                            .param("size", fields[0]).param("shade", fields[1]))
                    .andExpect(flash().attributeExists("cartError"));
        }
        assertThat(cartView(session, false).items()).isEmpty();
    }

    @Test
    void usesServerPriceAndImageAndFallsBackForSharedCharts() throws Exception {
        enableChoices();
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/cart/items").session(session).with(csrf())
                        .param("productId", product.getId().toString()).param("quantity", "1")
                        .param("size", "3.5 g").param("shade", "03 Bare Grape")
                        .param("price", "1").param("imageUrl", "https://example.com/fake.jpg"))
                .andExpect(flash().attributeExists("cartSuccess"));
        assertThat(cartView(session, false).items()).singleElement().satisfies(item -> {
            assertThat(item.unitPrice()).isEqualByComparingTo("80000");
            assertThat(item.imageUrl()).endsWith("catalog-016-03-bare-grape.webp");
        });
        product.setImageUrl("/images/catalog/catalog-016.png");
        product.setSalePrice(null);
        product.setPrice(new BigDecimal("123000"));
        product.setShades(List.of(new ProductShade("03 Bare Grape", "#a06175", "/images/catalog/shades/catalog-066-shade-chart.png", "Chart", "chart")));
        productRepository.saveAndFlush(product);
        assertThat(cartView(session, false).items()).singleElement().satisfies(item -> {
            assertThat(item.imageUrl()).isEqualTo("/images/catalog/catalog-016.png");
            assertThat(item.unitPrice()).isEqualByComparingTo("123000");
        });
    }

    @Test
    void retiredColorAndReducedSharedStockShowWarningsButCanBeRemoved() throws Exception {
        enableChoices();
        MockHttpSession session = new MockHttpSession();
        addSelection(session, "3.5 g", "03 Bare Grape", 2, false);
        addSelection(session, "3.5 g", "05 Jujube", 2, false);
        setStock(3);
        assertThat(cartView(session, false).items()).allSatisfy(item -> assertThat(item.warning()).contains("vượt tồn"));
        product.setShades(List.of(new ProductShade("05 Jujube", "#be635c")));
        productRepository.saveAndFlush(product);
        assertThat(cartView(session, false).items().getFirst().warning()).contains("chọn lại");
        mockMvc.perform(post("/cart/items/{id}/remove", product.getId()).session(session).with(csrf())
                        .param("size", "3.5 g").param("shade", "03 Bare Grape"))
                .andExpect(flash().attributeExists("cartSuccess"));
        assertThat(cartView(session, false).items()).hasSize(1);
    }

    @Test
    void readsPreviousProductOnlyGuestSessionsAndRequiresCsrfForVariantMutations() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("oneshop.guestCart", Map.of(product.getId(), 2));
        assertThat(cartView(session, false).itemCount()).isEqualTo(2);
        mockMvc.perform(post("/cart/items/{id}", product.getId()).session(session)
                        .param("quantity", "3").param("size", "").param("shade", ""))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/cart/items/{id}/remove", product.getId()).session(session))
                .andExpect(status().isForbidden());
        assertThat(cartView(session, false).itemCount()).isEqualTo(2);
    }

    @Test
    void rendersCorrectPhotosChoicesAndDetailLinksForTwoColors() throws Exception {
        enableChoices();
        product.setName("The Juicy Lasting Tint");
        product.setPrice(new BigDecimal("155000"));
        product.setSalePrice(null);
        productRepository.saveAndFlush(product);
        MockHttpSession session = new MockHttpSession();
        addSelection(session, "3.5 g", "03 Bare Grape", 1, false);
        addSelection(session, "3.5 g", "05 Jujube", 2, false);
        // Keep Spring Security but omit the container-only SiteMesh buffering filter for template rendering.
        MockMvc renderMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        String html = renderMvc.perform(get("/cart").session(session)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("03 Bare Grape", "05 Jujube", "465.000", "/products/test-serum-cart",
                "catalog-016-03-bare-grape.webp", "catalog-016-05-jujube.webp", "name=\"shade\"", "/css/cart.css", "/js/cart-page.js")
                .doesNotContain("SKU:");
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/cart-preview"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/cart-preview/fragment.html"), html);
    }

    private void enableChoices() {
        product.setSizes(List.of("3.5 g", "7 g"));
        product.setShades(List.of(
                new ProductShade("03 Bare Grape", "#a06175", "/images/catalog/shades/catalog-016-03-bare-grape.webp", "Bare Grape", "photo"),
                new ProductShade("05 Jujube", "#be635c", "/images/catalog/shades/catalog-016-05-jujube.webp", "Jujube", "photo")));
        product = productRepository.saveAndFlush(product);
    }

    private void addSelection(MockHttpSession session, String size, String shade, int count, boolean account) throws Exception {
        var request = post("/cart/items").session(session).with(csrf()).param("productId", product.getId().toString())
                .param("quantity", String.valueOf(count)).param("size", size).param("shade", shade);
        if (account) request.with(user(USER_EMAIL).roles("CUSTOMER"));
        mockMvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("cartSuccess"));
    }

    private CartView cartView(MockHttpSession session, boolean account) throws Exception {
        var request = get("/cart").session(session);
        if (account) request.with(user(USER_EMAIL).roles("CUSTOMER"));
        return (CartView) mockMvc.perform(request).andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("cart");
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
