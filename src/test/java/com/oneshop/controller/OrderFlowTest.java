package com.oneshop.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oneshop.MySqlIntegrationTestSupport;
import com.oneshop.dto.request.CheckoutForm;
import com.oneshop.dto.response.OrderView;
import com.oneshop.entity.*;
import com.oneshop.exception.OrderOperationException;
import com.oneshop.repository.*;
import com.oneshop.service.*;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.mail.enabled=true", "app.mail.from=shop@example.com",
        "spring.mail.username=shop@example.com", "spring.mail.password=test-only-no-network",
        "app.mail.base-url=http://localhost:8080", "app.orders.shipping-fee=30000"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowTest extends MySqlIntegrationTestSupport {
    static final String EMAIL = "buyer@example.com";
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;
    @Autowired OrderRepository repository;
    @Autowired CartService carts;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired StoreStockRepository stocks;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mail;
    Product product;
    Store store;

    @BeforeEach void fixture() {
        reset(mail);
        when(mail.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        addUser(EMAIL); addUser("other@example.com");
        store = new Store(); store.setCode("HCM-D1"); store.setName("OneShop thử nghiệm"); store.setAddress("Địa chỉ thử");
        store = stores.saveAndFlush(store);
        product = new Product(); product.setName("Son thử nghiệm"); product.setSku("ORDER-TEST"); product.setSlug("son-thu-nghiem");
        product.setPrice(new BigDecimal("100000")); product.setStatus(ProductStatus.ACTIVE);
        product.setSizes(new ArrayList<>(List.of("3 g")));
        ProductShade shade = new ProductShade("Hồng đất", "#a46572");
        product.setShades(new ArrayList<>(List.of(shade))); product = products.saveAndFlush(product);
        StoreStock stock = new StoreStock(); stock.setStore(store); stock.setProduct(product); stock.setQuantity(10); stocks.saveAndFlush(stock);
        carts.addItem(EMAIL, null, product.getId(), 2, "3 g", "Hồng đất");
    }
    void addUser(String email) {
        User u = new User(); u.setEmail(email); u.setPassword("not-a-login-password"); u.setFullName("Khách thử nghiệm"); users.saveAndFlush(u);
    }
    CheckoutForm form() {
        CheckoutForm f = new CheckoutForm(); f.setCustomerName("Khách thử nghiệm"); f.setCustomerPhone("0912345678");
        f.setShippingAddress("12 Đường thử nghiệm, TP. Hồ Chí Minh");
        f.setCheckoutToken(UUID.randomUUID().toString()); return f;
    }
    String place() { return orders.place(EMAIL, form()); }
    int stock() { return stocks.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity(); }

    @Test void checkoutPersistsSnapshotsDeductsStockClearsCartAndSendsOneConfirmation() throws Exception {
        CheckoutForm form = form(); form.setCustomerName("Khách <b>thử</b>");
        String code = orders.place(EMAIL, form);
        assertThat(orders.place(EMAIL, form)).isEqualTo(code);
        assertThat(repository.count()).isEqualTo(1); assertThat(stock()).isEqualTo(8);
        assertThat(carts.getCart(EMAIL,null).items()).isEmpty();
        var order = orders.forCustomer(EMAIL, code);
        assertThat(order.totalAmount()).isEqualByComparingTo("230000");
        assertThat(order.items()).singleElement().satisfies(i -> {
            assertThat(i.shade()).isEqualTo("Hồng đất"); assertThat(i.size()).isEqualTo("3 g"); assertThat(i.quantity()).isEqualTo(2);
        });
        assertThat(order.events()).singleElement().satisfies(e -> assertThat(e.status()).isEqualTo(OrderStatus.NEW));
        assertThat(order.emailStatus()).isEqualTo("SENT"); assertThat(order.emailSentAt()).isNotNull();
        var captured = ArgumentCaptor.forClass(MimeMessage.class); verify(mail, times(1)).send(captured.capture());
        MimeMessage message = captured.getValue();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo(EMAIL);
        assertThat(message.getSubject()).contains("Đặt hàng thành công", code);
        String html = contents(message);
        assertThat(html).contains("/orders/" + code, "Theo dõi đơn hàng", "Hồng đất", "230.000", "&lt;b&gt;thử&lt;/b&gt;");
        product.setName("Tên đã sửa"); product.setPrice(new BigDecimal("900000")); products.saveAndFlush(product);
        var snapshot = orders.forCustomer(EMAIL, code).items().getFirst();
        assertThat(snapshot.name()).isEqualTo("Son thử nghiệm"); assertThat(snapshot.unitPrice()).isEqualByComparingTo("100000");
    }
    static String contents(Part part) throws Exception {
        Object body = part.getContent(); if (body instanceof String text) return text;
        StringBuilder result = new StringBuilder();
        if (body instanceof Multipart multi) for (int i=0;i<multi.getCount();i++) result.append(contents(multi.getBodyPart(i)));
        return result.toString();
    }
    @Test void smtpFailureDoesNotRollbackOrderAndAdminCanRetry() throws Exception {
        doThrow(new MailSendException("test SMTP failure")).when(mail).send(any(MimeMessage.class));
        String code = place(); assertThat(stock()).isEqualTo(8); assertThat(carts.getCart(EMAIL,null).items()).isEmpty();
        assertThat(orders.forCustomer(EMAIL,code).emailStatus()).isEqualTo("FAILED");
        doNothing().when(mail).send(any(MimeMessage.class));
        mvc.perform(post("/admin/orders/{code}/email",code).with(user("admin@example.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(orders.forCustomer(EMAIL,code).emailStatus()).isEqualTo("SENT");
    }
    @Test void customerCannotReadOrCancelAnotherCustomersOrderOrEnterAdmin() throws Exception {
        String code = place();
        mvc.perform(get("/orders/{code}",code).with(user("other@example.com"))).andExpect(status().isNotFound());
        mvc.perform(post("/orders/{code}/cancel",code).with(user("other@example.com")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/admin/orders").with(user(EMAIL))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/orders/{code}/status",code).param("status","COMPLETED").with(user(EMAIL)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/orders")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/orders/{code}/cancel",code).with(user(EMAIL))).andExpect(status().isForbidden());
        assertThat(stock()).isEqualTo(8);
    }
    @Test void adminProgressAppearsInCustomerTimelineAndRejectsSkippingOrRewinding() throws Exception {
        String code = place();
        update(code,"COMPLETED"); assertThat(orders.forCustomer(EMAIL,code).status()).isEqualTo(OrderStatus.NEW);
        for (String state : List.of("CONFIRMED","PACKING","SHIPPING","COMPLETED")) update(code,state);
        OrderView result = orders.forCustomer(EMAIL,code);
        assertThat(result.events()).extracting(OrderView.Event::status).containsExactly(OrderStatus.NEW,OrderStatus.CONFIRMED,OrderStatus.PACKING,OrderStatus.SHIPPING,OrderStatus.COMPLETED);
        update(code,"CANCELLED"); assertThat(stock()).isEqualTo(8);
        mvc.perform(get("/orders/{code}",code).with(user(EMAIL))).andExpect(status().isOk())
                .andExpect(view().name("pages/order-detail"))
                .andExpect(model().attribute("order",result));
        mvc.perform(get("/admin/orders/{code}",code).with(user("admin@example.com").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("status","COMPLETED").with(user("admin@example.com").roles("ADMIN"))).andExpect(status().isOk());
    }
    void update(String code,String state) throws Exception {
        mvc.perform(post("/admin/orders/{code}/status",code).param("status",state).with(user("admin@example.com").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());
    }
    @Test void cancellationRestoresInventoryOnceAndStopsFurtherUpdates() throws Exception {
        String code = place();
        for(int i=0;i<2;i++) mvc.perform(post("/orders/{code}/cancel",code).with(user(EMAIL)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(stock()).isEqualTo(10);
        assertThat(orders.forCustomer(EMAIL,code).events()).hasSize(2);
        update(code,"CONFIRMED"); assertThat(orders.forCustomer(EMAIL,code).status()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test void adminCancellationDuringPackingRestoresStockButCustomerCannotCancelConfirmedOrder() throws Exception {
        String code=place(); update(code,"CONFIRMED");
        mvc.perform(post("/orders/{code}/cancel",code).with(user(EMAIL)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(orders.forCustomer(EMAIL,code).status()).isEqualTo(OrderStatus.CONFIRMED);
        update(code,"PACKING"); update(code,"CANCELLED"); assertThat(stock()).isEqualTo(10);
    }
    @Test void checkoutRejectsInvalidInputsAndUsesServerPriceInsteadOfPostedTotal() throws Exception {
        mvc.perform(get("/checkout").with(user(EMAIL))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"storeId\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Cửa hàng xử lý đơn"))));
        mvc.perform(post("/checkout").with(user(EMAIL)).with(csrf()).param("customerName","").param("customerPhone","abc"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("checkoutForm","customerName","customerPhone","shippingAddress","checkoutToken"));
        assertThat(repository.count()).isZero(); verify(mail,never()).send(any(MimeMessage.class));
        String token=UUID.randomUUID().toString();
        mvc.perform(post("/checkout").with(user(EMAIL)).with(csrf()).param("customerName","Khách thử")
                .param("customerPhone","0912345678").param("shippingAddress","12 Đường thử nghiệm, TP HCM")
                .param("storeId","-999").param("checkoutToken",token).param("totalAmount","1"))
                .andExpect(status().is3xxRedirection());
        assertThat(repository.findByCheckoutToken(token).orElseThrow().getTotalAmount()).isEqualByComparingTo("230000");
        assertThat(repository.findByCheckoutToken(token).orElseThrow().getStore().getId()).isEqualTo(store.getId());
        mvc.perform(get("/orders").with(user(EMAIL))).andExpect(status().isOk());
    }
    @Test void insufficientStoreStockRollsBackEverythingAndDoesNotSendMail() {
        var stock=stocks.findByStoreIdAndProductId(store.getId(),product.getId()).orElseThrow(); stock.setQuantity(1); stocks.saveAndFlush(stock);
        assertThatThrownBy(this::place).isInstanceOf(OrderOperationException.class);
        assertThat(repository.count()).isZero(); assertThat(stock()).isEqualTo(1);
        assertThat(carts.getCart(EMAIL,null).itemCount()).isEqualTo(2); verify(mail,never()).send(any(MimeMessage.class));
    }

    Store otherStore(int quantity) {
        Store branch = new Store(); branch.setCode("HCM-D2"); branch.setName("OneShop chi nhánh khác");
        branch.setAddress("Địa chỉ chi nhánh thử"); branch = stores.saveAndFlush(branch);
        StoreStock inventory = new StoreStock(); inventory.setStore(branch); inventory.setProduct(product);
        inventory.setQuantity(quantity); stocks.saveAndFlush(inventory);
        return branch;
    }
    int stock(Store branch) {
        return stocks.findByStoreIdAndProductId(branch.getId(), product.getId()).orElseThrow().getQuantity();
    }
    @Test void checkoutAutomaticallyFindsAnotherStoreWithoutDeductingInsufficientCandidate() throws Exception {
        var inventory = stocks.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow();
        inventory.setQuantity(1); stocks.saveAndFlush(inventory);
        Store branch = otherStore(10);
        String token = UUID.randomUUID().toString();
        mvc.perform(post("/checkout").with(user(EMAIL)).with(csrf()).param("customerName","Khách thử")
                .param("customerPhone","0912345678").param("shippingAddress","12 Đường thử nghiệm, TP HCM")
                .param("checkoutToken",token)).andExpect(status().is3xxRedirection());
        assertThat(repository.findByCheckoutToken(token).orElseThrow().getStore().getId()).isEqualTo(branch.getId());
        assertThat(stock()).isEqualTo(1); assertThat(stock(branch)).isEqualTo(8);
    }
    @Test void adminTransfersReservationOnceAndCancellationReturnsStockToNewStore() throws Exception {
        Store branch = otherStore(10); String code = place();
        mvc.perform(get("/admin/orders/{code}",code).with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(model().attributeExists("fulfillmentStores"));
        for (int i=0;i<2;i++) mvc.perform(post("/admin/orders/{code}/store",code).with(user("admin@example.com").roles("ADMIN"))
                .with(csrf()).param("storeId",branch.getId().toString())).andExpect(status().is3xxRedirection());
        assertThat(stock()).isEqualTo(10); assertThat(stock(branch)).isEqualTo(8);
        assertThat(repository.findByCode(code).orElseThrow().getStore().getId()).isEqualTo(branch.getId());
        assertThat(orders.forCustomer(EMAIL,code).events()).hasSize(2);
        orders.cancel(EMAIL,code);
        assertThat(stock()).isEqualTo(10); assertThat(stock(branch)).isEqualTo(10);
        verify(mail,times(1)).send(any(MimeMessage.class));
    }
    @Test void failedAdminAssignmentLeavesBothReservationsAndHistoryUnchanged() throws Exception {
        Store branch = otherStore(1); String code = place();
        mvc.perform(post("/admin/orders/{code}/store",code).with(user("admin@example.com").roles("ADMIN"))
                .with(csrf()).param("storeId",branch.getId().toString())).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("orderError"));
        assertThat(stock()).isEqualTo(8); assertThat(stock(branch)).isEqualTo(1);
        assertThat(repository.findByCode(code).orElseThrow().getStore().getId()).isEqualTo(store.getId());
        assertThat(orders.forCustomer(EMAIL,code).events()).hasSize(1);
    }
    @Test void storeAssignmentRequiresAdminAndCsrfAndOnlyAcceptsUnconfirmedOrders() throws Exception {
        Store branch = otherStore(10); String code = place();
        mvc.perform(post("/admin/orders/{code}/store",code).with(user(EMAIL)).with(csrf())
                .param("storeId",branch.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/orders/{code}/store",code).with(user("admin@example.com").roles("ADMIN"))
                .param("storeId",branch.getId().toString())).andExpect(status().isForbidden());
        update(code,"CONFIRMED");
        mvc.perform(post("/admin/orders/{code}/store",code).with(user("admin@example.com").roles("ADMIN")).with(csrf())
                .param("storeId",branch.getId().toString())).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("orderError"));
        assertThat(stock()).isEqualTo(8); assertThat(stock(branch)).isEqualTo(10);
        assertThat(repository.findByCode(code).orElseThrow().getStore().getId()).isEqualTo(store.getId());
    }

    @Test void realCheckoutHidesStoreChoiceAndOnlyAdminDetailRendersAssignmentForm() throws Exception {
        var role = new Role(); role.setName("ADMIN"); role = roles.saveAndFlush(role);
        var buyer = users.findByEmail(EMAIL).orElseThrow(); buyer.setRole(role);
        buyer.setPassword(encoder.encode("HttpCheckout!2026")); users.saveAndFlush(buyer);
        var client = java.net.http.HttpClient.newBuilder().cookieHandler(new java.net.CookieManager()).build();
        String base = "http://localhost:" + port;
        var page = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/login")).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        var csrf = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page.body());
        assertThat(csrf.find()).isTrue();
        var login = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "username=buyer%40example.com&password=HttpCheckout%212026&_csrf="
                                + java.net.URLEncoder.encode(csrf.group(1), java.nio.charset.StandardCharsets.UTF_8))).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(302);
        var checkout = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/checkout")).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(checkout.statusCode()).isEqualTo(200);
        assertThat(checkout.body()).contains("Thông tin nhận hàng", "checkoutToken")
                .doesNotContain("name=\"storeId\"", "Cửa hàng xử lý đơn", "Chọn cửa hàng");
        String code = place();
        var adminPage = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/admin/orders/" + code)).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(adminPage.statusCode()).isEqualTo(200);
        assertThat(adminPage.body()).contains("Phân công cửa hàng", "name=\"storeId\"", "/admin/orders/" + code + "/store");
    }
    @Test void concurrentCheckoutsCannotOversellTheLastUnits() throws Exception {
        carts.addItem("other@example.com",null,product.getId(),2,"3 g","Hồng đất");
        var stock=stocks.findByStoreIdAndProductId(store.getId(),product.getId()).orElseThrow(); stock.setQuantity(3); stocks.saveAndFlush(stock);
        try(var pool=Executors.newFixedThreadPool(2)) {
            CountDownLatch start=new CountDownLatch(1);
            List<Future<Boolean>> results=new ArrayList<>();
            for(String email:List.of(EMAIL,"other@example.com")) results.add(pool.submit(()->{
                start.await(); try { orders.place(email,form()); return true; } catch(OrderOperationException expected) { return false; }
            }));
            start.countDown(); int success=0; for(var result:results) if(result.get(20,TimeUnit.SECONDS)) success++;
            assertThat(success).isEqualTo(1);
        }
        assertThat(repository.count()).isEqualTo(1); assertThat(stock()).isEqualTo(1);
        verify(mail,times(1)).send(any(MimeMessage.class));
    }
}
