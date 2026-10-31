package com.oneshop.controller;

import static org.assertj.core.api.Assertions.*;
import com.oneshop.MySqlIntegrationTestSupport;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RememberMeHttpTest extends MySqlIntegrationTestSupport {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired PasswordResetAccountRepository credentials;
    @Autowired PasswordEncoder encoder;
    User account; CookieManager cookies; HttpClient client;
    String email="remember@oneshop.test";
    @BeforeEach void fixture(){
        account=new User();account.setEmail(email);account.setFullName("Remember test");account.setPassword(encoder.encode("Remember!2026"));users.saveAndFlush(account);
        cookies=new CookieManager();client=HttpClient.newBuilder().cookieHandler(cookies).build();
    }
    HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));}
    String csrf(String html){var matcher=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertThat(matcher.find()).isTrue();return matcher.group(1);}
    HttpResponse<String> post(String path,String form)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),HttpResponse.BodyHandlers.ofString());}
    HttpResponse<String> login(boolean remember)throws Exception{
        var page=get("/login");assertThat(page.body()).contains("remember-me","30 ngày");
        return post("/login","username="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password=Remember%212026&_csrf="+URLEncoder.encode(csrf(page.body()),StandardCharsets.UTF_8)+(remember?"&remember-me=true":""));
    }
    void discardSession(){for(var uri:new ArrayList<>(cookies.getCookieStore().getURIs()))for(var cookie:new ArrayList<>(cookies.getCookieStore().get(uri)))if("JSESSIONID".equals(cookie.getName()))cookies.getCookieStore().remove(uri,cookie);}
    @Test void optInRestoresLoginWithoutSessionAndCookieHasThirtyDayLifetime()throws Exception{
        var response=login(true);assertThat(response.statusCode()).isEqualTo(302);
        String cookie=response.headers().allValues("Set-Cookie").stream().filter(c->c.startsWith("ONESHOP_REMEMBER=")).findFirst().orElseThrow();
        assertThat(cookie).contains("Max-Age=2592000","HttpOnly","SameSite=Lax");
        discardSession();assertThat(get("/account/header").body()).contains("\"authenticated\":true",email);
        assertThat(get("/api/auth/me").statusCode()).isEqualTo(401);
    }
    @Test void uncheckedLoginHasNoPersistentCookie()throws Exception{
        login(false);assertThat(cookies.getCookieStore().getCookies()).noneMatch(c->"ONESHOP_REMEMBER".equals(c.getName()));
        discardSession();assertThat(get("/account/header").body()).contains("\"authenticated\":false");
    }
    @Test void logoutClearsPersistentLoginCookie()throws Exception{
        login(true);var page=get("/logout");post("/logout","_csrf="+URLEncoder.encode(csrf(page.body()),StandardCharsets.UTF_8));
        assertThat(cookies.getCookieStore().getCookies()).noneMatch(c->"ONESHOP_REMEMBER".equals(c.getName())&&!c.hasExpired());
        assertThat(get("/account/header").body()).contains("\"authenticated\":false");
    }
    @Test void passwordChangeRejectsOldRememberCookie()throws Exception{
        login(true);account.setPassword(encoder.encode("Changed!2026"));users.saveAndFlush(account);discardSession();
        assertThat(get("/account/header").body()).contains("\"authenticated\":false");
    }
    @Test void credentialVersionChangeRejectsRememberCookieEvenIfPasswordUnchanged()throws Exception{
        login(true);var reset=new PasswordResetAccount(account);reset.setCredentialVersion(1);credentials.saveAndFlush(reset);discardSession();
        assertThat(get("/account/header").body()).contains("\"authenticated\":false");
    }
    @Test void disabledAccountCannotRestoreLogin()throws Exception{
        login(true);account.setActive(false);users.saveAndFlush(account);discardSession();
        assertThat(get("/account/header").body()).contains("\"authenticated\":false");
    }
}
