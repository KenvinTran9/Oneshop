package com.oneshop;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oneshop.config.RoleInitializer;
import com.oneshop.dto.request.*;
import com.oneshop.entity.*;
import com.oneshop.exception.OrderOperationException;
import com.oneshop.repository.*;
import com.oneshop.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"app.payments.bank-name=Test Bank", "app.payments.account-number=123456789",
        "app.support.phone=09899999999", "app.payments.account-holder=SHOP TEST", "app.payments.qr-image=", "app.mail.enabled=false"})
@org.springframework.test.context.event.RecordApplicationEvents
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class PaymentDeliveryFlowTest extends MySqlIntegrationTestSupport {
    @Autowired org.springframework.test.context.event.ApplicationEvents published;
    @Autowired MockMvc mvc;
    @Autowired RoleInitializer seed;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired StoreStockRepository stocks;
    @Autowired OrderRepository orderRepository;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired OrderFulfillmentService fulfillment;
    @Autowired SalesReportService reports;
    @Autowired org.thymeleaf.spring6.SpringTemplateEngine templates;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    Store shop, other;
    Product product;
    String buyer = "buyer@payment.test";

    @BeforeEach void fixture() {
        seed.run(); shop = store("PAY-1"); other = store("PAY-2");
        account("admin@payment.test", "ADMIN", null); account(buyer, "CUSTOMER", null);
        account("manager@payment.test", "BRANCH_MANAGER", shop);
        product = new Product(); product.setName("Son thử chuyển khoản"); product.setSlug("son-payment");
        product.setSku("PAY-001"); product.setPrice(new BigDecimal("150000")); products.saveAndFlush(product);
        for (Store s : List.of(shop,other)) {
            StoreStock stock = new StoreStock(); stock.setProduct(product); stock.setStore(s); stock.setQuantity(10); stocks.saveAndFlush(stock);
        }
    }
    Store store(String code) { Store s=new Store(); s.setCode(code); s.setName(code); s.setAddress("123 Nguyễn Huệ, TP.HCM"); return stores.saveAndFlush(s); }
    void account(String email,String role,Store store) {
        User u=new User(); u.setEmail(email); u.setPassword("not-used-for-login"); u.setFullName("Khách thử");
        u.setPhone("0901234567"); u.setRole(roles.findByName(role).orElseThrow()); u.setStore(store); users.saveAndFlush(u);
    }
    void login(String email,String role) {
        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                email,"unused",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+role))));
    }
    CheckoutForm checkout(PaymentMethod method) {
        CheckoutForm f=new CheckoutForm(); f.setCustomerName("Khách thanh toán"); f.setCustomerPhone("0901234567");
        f.setShippingAddress("123 Nguyễn Huệ, Quận 1, TP.HCM"); f.setCheckoutToken(UUID.randomUUID().toString()); f.setPaymentMethod(method); return f;
    }
    String place(PaymentMethod method) { carts.addItem(buyer,null,product.getId(),2,null,null); return orders.place(buyer,checkout(method)); }
    PaymentReviewForm receipt() { return new PaymentReviewForm(PaymentReviewForm.Action.CONFIRM_RECEIPT,"BANK-REF-001"); }
    DeliveryForm delivery(long version) { return new DeliveryForm("OneShop giao hàng","Nguyễn Văn Giao","0901234567","TRACK-001",LocalDate.now().plusDays(1),"Gọi khách trước khi giao",version); }

    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void bankOrdersStoreAccountSnapshotAndCannotAdvanceBeforePayment() {
        String code=place(PaymentMethod.BANK_TRANSFER); var order=orders.forAdmin(code);
        assertThat(order.payment().accountNumber()).isEqualTo("123456789"); assertThat(order.payment().accountHolder()).isEqualTo("SHOP TEST");
        assertThat(order.payment().status()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(order.getNextStatuses()).containsExactly(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        orders.updateStatus(code,OrderStatus.CONFIRMED);
        assertThatThrownBy(()->orders.updateStatus(code,OrderStatus.PACKING)).isInstanceOf(OrderOperationException.class);
        assertThat(orders.forAdmin(code).status()).isEqualTo(OrderStatus.CONFIRMED);
        fulfillment.reportTransfer(buyer,code);
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.REPORTED);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void customerReportDoesNotMarkPaidAndDuplicateNotificationsAreIgnored() {
        String code=place(PaymentMethod.BANK_TRANSFER); long before=orders.forAdmin(code).events().size();
        fulfillment.reportTransfer(buyer,code); fulfillment.reportTransfer(buyer,code);
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.REPORTED);
        assertThat(orders.forAdmin(code).events()).hasSize((int)before+1);
        orders.updateStatus(code,OrderStatus.CONFIRMED);
        assertThatThrownBy(()->orders.updateStatus(code,OrderStatus.PACKING)).isInstanceOf(OrderOperationException.class);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void adminReceiptUnlocksFulfillmentAndIsIdempotent() {
        String code=place(PaymentMethod.BANK_TRANSFER); fulfillment.reportTransfer(buyer,code);
        fulfillment.reviewPayment(code,receipt()); var paid=orders.forAdmin(code);
        fulfillment.reviewPayment(code,receipt()); assertThat(orders.forAdmin(code).events()).hasSize(paid.events().size());
        assertThat(paid.payment().status()).isEqualTo(PaymentStatus.PAID); assertThat(paid.payment().paidAt()).isNotNull();
        assertThat(paid.getNextStatuses()).contains(OrderStatus.CONFIRMED);
        completeAsCustomer(code);
        assertThat(orders.forAdmin(code).payment().paidAt()).isEqualTo(paid.payment().paidAt());
        assertThat(reports.report(LocalDate.now(),LocalDate.now(),null).revenue()).isEqualByComparingTo("300000");
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void paidCancellationRequiresExplicitRefundAndRestoresStockOnce() {
        String code=place(PaymentMethod.BANK_TRANSFER); fulfillment.reviewPayment(code,receipt()); orders.cancel(buyer,code);
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(stocks.findByStoreIdAndProductId(shop.getId(),product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThatThrownBy(()->orders.cancel(buyer,code)).isInstanceOf(OrderOperationException.class);
        fulfillment.reviewPayment(code,new PaymentReviewForm(PaymentReviewForm.Action.CONFIRM_REFUND,"REFUND-001"));
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThatThrownBy(()->orders.updateStatus(code,OrderStatus.CONFIRMED)).isInstanceOf(OrderOperationException.class);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void lateTransferOnCancelledOrderRequiresRefundAndReportCanBeRejected() {
        String code=place(PaymentMethod.BANK_TRANSFER); fulfillment.reportTransfer(buyer,code);
        fulfillment.reviewPayment(code,new PaymentReviewForm(PaymentReviewForm.Action.REJECT_REPORT,"Chưa nhận được giao dịch"));
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.UNPAID);
        orders.cancel(buyer,code); fulfillment.reviewPayment(code,receipt());
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(orders.forAdmin(code).status()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void deliverySnapshotIsVisibleToCustomerAndStaleEditsAreRejected() {
        String code=place(PaymentMethod.COD); orders.updateStatus(code,OrderStatus.CONFIRMED); orders.updateStatus(code,OrderStatus.PACKING); long version=orders.forAdmin(code).version();
        fulfillment.updateDelivery(code,delivery(version)); var view=orders.forCustomer(buyer,code);
        assertThat(view.delivery().shipperName()).isEqualTo("Nguyễn Văn Giao");assertThat(view.delivery().shipperPhone()).isEqualTo("0901234567");
        assertThat(view.delivery().trackingCode()).isEqualTo("TRACK-001");assertThat(view.version()).isGreaterThan(version);
        assertThatThrownBy(()->fulfillment.updateDelivery(code,delivery(version))).isInstanceOf(OrderOperationException.class);
        assertThatThrownBy(()->fulfillment.updateDelivery(code,new DeliveryForm("", "A","invalid","",null,"",view.version()))).isInstanceOf(OrderOperationException.class);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void branchCannotReviewPaymentsOrChangeAnotherStoresDelivery() throws Exception {
        String code=place(PaymentMethod.BANK_TRANSFER);orders.assignStore(code,other.getId());
        mvc.perform(post("/admin/orders/"+code+"/payment").with(user("manager@payment.test").roles("BRANCH_MANAGER")).with(csrf())
                .param("action","CONFIRM_RECEIPT").param("reference","FAKE")).andExpect(status().isForbidden());
        mvc.perform(post("/branch/orders/"+code+"/delivery").with(user("manager@payment.test").roles("BRANCH_MANAGER")).with(csrf())
                .param("expectedVersion","0")).andExpect(status().isForbidden());
        login("admin@payment.test","ADMIN"); assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.UNPAID);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void otherCustomerCannotReportPaymentOrReadProgressAndPostNeedsCsrf() throws Exception {
        String code=place(PaymentMethod.BANK_TRANSFER);
        mvc.perform(post("/orders/"+code+"/transfer").with(user("someone@payment.test")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/orders/"+code+"/progress").with(user("someone@payment.test"))).andExpect(status().isNotFound());
        mvc.perform(post("/orders/"+code+"/transfer").with(user(buyer))).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void progressAndBothOrderPagesRenderPaymentAndShipperDetails() throws Exception {
        String code=place(PaymentMethod.BANK_TRANSFER); orders.updateStatus(code,OrderStatus.CONFIRMED); fulfillment.reviewPayment(code,receipt()); orders.updateStatus(code,OrderStatus.PACKING); fulfillment.updateDelivery(code,delivery(orders.forAdmin(code).version()));
        mvc.perform(get("/orders/"+code).with(user(buyer))).andExpect(status().isOk())
                .andExpect(view().name("pages/order-detail")).andExpect(model().attributeExists("order"));
        mvc.perform(get("/orders/"+code+"/progress").with(user(buyer))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.paymentStatus").value("PAID"));
        mvc.perform(get("/admin/orders/"+code).with(user("admin@payment.test").roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void codRemainsAvailableAndCollectsPaymentAtDeliveryCompletion() {
        String code=place(PaymentMethod.COD);
        assertThatThrownBy(()->fulfillment.reviewPayment(code,receipt())).isInstanceOf(OrderOperationException.class);
        completeAsCustomer(code);
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.PAID);
        assertThatThrownBy(()->fulfillment.updateDelivery(code,delivery(orders.forAdmin(code).version()))).isInstanceOf(OrderOperationException.class);
        jdbc.update("update orders set payment_method=null,payment_status=null where code=?",code);
        assertThat(orders.forAdmin(code).payment().method()).isEqualTo(PaymentMethod.COD);
        assertThat(orders.forAdmin(code).payment().status()).isEqualTo(PaymentStatus.PAID);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void transferEmailIncludesCorrectMethodAccountAndOrderReference() {
        String code=place(PaymentMethod.BANK_TRANSFER);var context=new org.thymeleaf.context.Context();
        context.setVariable("order",orders.forAdmin(code));context.setVariable("trackingUrl","http://localhost/orders/"+code);
        String text=templates.process("mail/order-confirmation",context);
        assertThat(text).contains("Chuyển khoản ngân hàng","123456789",code.replace("-",""),"đối soát").doesNotContain("Thanh toán khi nhận hàng (COD)");
    }
    @Test void productionMigrationIsRepeatable() {
        jdbc.execute("alter table orders drop column payment_method, drop column payment_status, drop column shipper_name, drop column delivery_note");
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection->{
            var resource=new org.springframework.core.io.ClassPathResource("db/order-payment-delivery-mysql.sql");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,resource);
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,resource);return null;
        });
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name='orders' and column_name in ('payment_method','payment_status','shipper_name','delivery_note')",Integer.class)).isEqualTo(4);
    }
    @Test void eachRealAdminUpdatePublishesOneEmailWithItsOwnSnapshot() {
        String code = place(PaymentMethod.BANK_TRANSFER);
        login("admin@payment.test", "ADMIN");
        fulfillment.reviewPayment(code, receipt());
        fulfillment.reviewPayment(code, receipt());
        orders.updateStatus(code, OrderStatus.CONFIRMED);
        orders.updateStatus(code, OrderStatus.PACKING);
        var delivery = new DeliveryForm("Giao hàng thử", "Nguyễn An", "0901234567", "TRACK-01",
                LocalDate.now().plusDays(2), "Gọi trước khi giao", orders.forAdmin(code).version());
        fulfillment.updateDelivery(code, delivery);
        var unchanged = new DeliveryForm(delivery.carrier(), delivery.shipperName(), delivery.shipperPhone(),
                delivery.trackingCode(), delivery.estimatedDelivery(), delivery.deliveryNote(), orders.forAdmin(code).version());
        fulfillment.updateDelivery(code, unchanged);
        orders.updateStatus(code, OrderStatus.SHIPPING);
        orders.confirmReceived(buyer, code);
        var mails = published.stream(OrderUpdateMail.class).filter(e -> code.equals(e.code())).toList();
        assertThat(mails).hasSize(6);
        assertThat(mails).extracting(OrderUpdateMail::deliveryId).doesNotHaveDuplicates();
        assertThat(mails.get(1).status()).isEqualTo("Đã xác nhận");
        assertThat(mails.get(1).shipperName()).isNull();
        var shipping = mails.get(4);
        assertThat(shipping.shipperName()).isEqualTo("Nguyễn An");
        assertThat(shipping.shipperPhone()).isEqualTo("0901234567");
        var context = new org.thymeleaf.context.Context();
        context.setVariable("update", shipping); context.setVariable("trackingUrl", "http://localhost:8080/orders/" + code);
        assertThat(templates.process("mail/order-update", context)).contains("TRACK-01", "0901234567", "Đang giao hàng", code);
    }
    @Test void invalidTransitionPublishesNoUpdateEmail() {
        String code = place(PaymentMethod.BANK_TRANSFER);
        login("admin@payment.test", "ADMIN");
        assertThatThrownBy(() -> orders.updateStatus(code, OrderStatus.SHIPPING)).isInstanceOf(OrderOperationException.class);
        assertThat(published.stream(OrderUpdateMail.class).filter(e -> code.equals(e.code()))).isEmpty();
    }

    @Test void customerReceiptCompletesOrderCountsRevenueOnceAndSendsFeedbackMail() throws Exception {
        String code = place(PaymentMethod.COD);
        login("admin@payment.test", "ADMIN");
        orders.updateStatus(code, OrderStatus.CONFIRMED);
        orders.updateStatus(code, OrderStatus.PACKING);
        fulfillment.updateDelivery(code, delivery(orders.forAdmin(code).version()));
        orders.updateStatus(code, OrderStatus.SHIPPING);
        assertThat(reports.report(LocalDate.now(),LocalDate.now(),null).completed()).isZero();
        mvc.perform(post("/orders/{code}/received", code).with(user(buyer)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("orderSuccess"));
        login("admin@payment.test", "ADMIN");
        var complete = orders.forAdmin(code);
        assertThat(complete.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(complete.payment().status()).isEqualTo(PaymentStatus.PAID);
        var before = reports.report(LocalDate.now(),LocalDate.now(),null);
        assertThat(before.completed()).isEqualTo(1);
        assertThat(before.revenue()).isEqualByComparingTo(complete.subtotal());
        orders.confirmReceived(buyer,code);
        var after = reports.report(LocalDate.now(),LocalDate.now(),null);
        assertThat(after.completed()).isEqualTo(1);
        assertThat(after.revenue()).isEqualByComparingTo(before.revenue());
        var completionMails = published.stream(OrderUpdateMail.class)
                .filter(e -> code.equals(e.code()) && e.status().contains("Đã hoàn thành")).toList();
        assertThat(completionMails).singleElement().satisfies(mail -> {
            assertThat(mail.email()).isEqualTo(buyer);
            assertThat(mail.message()).contains("Khách hàng đã xác nhận", "09899999999", "góp ý");
        });
    }
    @Test void receiptRequiresOwnerCsrfAndShippingState() throws Exception {
        String code = place(PaymentMethod.COD);
        mvc.perform(post("/orders/{code}/received", code).with(user(buyer))).andExpect(status().isForbidden());
        mvc.perform(post("/orders/{code}/received", code).with(user("other@payment.test")).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/orders/{code}/received", code).with(user(buyer)).with(csrf()))
                .andExpect(flash().attributeExists("orderError"));
        assertThat(orders.forCustomer(buyer,code).status()).isEqualTo(OrderStatus.NEW);
        assertThat(published.stream(OrderUpdateMail.class).filter(e -> code.equals(e.code()))).isEmpty();
    }

    void completeAsCustomer(String code) {
        orders.updateStatus(code, OrderStatus.CONFIRMED);
        orders.updateStatus(code, OrderStatus.PACKING);
        fulfillment.updateDelivery(code,delivery(orders.forAdmin(code).version()));
        orders.updateStatus(code, OrderStatus.SHIPPING);
        orders.confirmReceived(buyer,code);
    }
    @Test @WithMockUser(username="admin@payment.test",roles="ADMIN")
    void strictWorkflowRejectsEarlyShipperAndAdminCompletion() {
        String code=place(PaymentMethod.COD);
        assertThatThrownBy(() -> fulfillment.updateDelivery(code,delivery(orders.forAdmin(code).version()))).isInstanceOf(OrderOperationException.class);
        assertThatThrownBy(() -> orders.updateStatus(code,OrderStatus.PACKING)).isInstanceOf(OrderOperationException.class);
        orders.updateStatus(code,OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> fulfillment.updateDelivery(code,delivery(orders.forAdmin(code).version()))).isInstanceOf(OrderOperationException.class);
        orders.updateStatus(code,OrderStatus.PACKING);
        assertThatThrownBy(() -> orders.updateStatus(code,OrderStatus.SHIPPING)).isInstanceOf(OrderOperationException.class);
        fulfillment.updateDelivery(code,delivery(orders.forAdmin(code).version()));
        orders.updateStatus(code,OrderStatus.SHIPPING);
        assertThat(orders.forAdmin(code).getNextStatuses()).isEmpty();
        assertThatThrownBy(() -> orders.updateStatus(code,OrderStatus.COMPLETED)).isInstanceOf(OrderOperationException.class);
        assertThatThrownBy(() -> orders.branchUpdate(code,OrderStatus.COMPLETED)).isInstanceOf(OrderOperationException.class);
        assertThat(reports.report(LocalDate.now(),LocalDate.now(),null).revenue()).isEqualByComparingTo("0");
        orders.confirmReceived(buyer,code);
        assertThat(reports.report(LocalDate.now(),LocalDate.now(),null).revenue()).isEqualByComparingTo("300000");
    }

}
