package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerRegistrationControllerTest extends com.oneshop.MySqlIntegrationTestSupport {

    private static final String PASSWORD = "TestPassword!42";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole.setDescription("Khach hang");
        customerRole = roleRepository.saveAndFlush(customerRole);
    }

    @Test
    void showsRegistrationPageWithCsrfField() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/register"))
                .andExpect(model().attributeExists("registrationRequest", "_csrf"));
    }

    @Test
    void registersCustomerAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "  Nguyen Van A  ")
                        .param("email", "  Customer@Example.com  ")
                        .param("phone", "0912345678")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("registrationSuccessMessage",
                        "Đăng ký thành công. Vui lòng đăng nhập bằng tài khoản vừa tạo."));

        User registeredUser = userRepository.findByEmail("customer@example.com").orElseThrow();
        assertThat(registeredUser.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(registeredUser.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, registeredUser.getPassword())).isTrue();
        assertThat(registeredUser.getRole().getName()).isEqualTo("CUSTOMER");
    }

    @Test
    void rejectsAnEmailThatAlreadyExistsWithoutIncludingItInTheErrorMessage() throws Exception {
        saveExistingUser("already@example.com");

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Another User")
                        .param("email", "ALREADY@example.com")
                        .param("phone", "0912345678")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/register"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "email"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void reportsPasswordConfirmationMismatchOnTheConfirmationField() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Customer")
                        .param("email", "new@example.com")
                        .param("phone", "0912345678")
                        .param("password", PASSWORD)
                        .param("confirmPassword", "DifferentPassword!42"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/register"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "confirmPassword"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void rejectsInvalidEmailPhoneAndShortPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Customer")
                        .param("email", "not-an-email")
                        .param("phone", "12345")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/register"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "email", "phone", "password"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void rejectsPasswordsThatExceedBcryptByteLimit() throws Exception {
        String tooLongUtf8Password = "長".repeat(25);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Customer")
                        .param("email", "long-password@example.com")
                        .param("phone", "0912345678")
                        .param("password", tooLongUtf8Password)
                        .param("confirmPassword", tooLongUtf8Password))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/register"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "password"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void ignoresClientSuppliedAdminRoleAndAlwaysCreatesCustomer() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Customer")
                        .param("email", "role-test@example.com")
                        .param("phone", "0912345678")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD)
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User registeredUser = userRepository.findByEmail("role-test@example.com").orElseThrow();
        assertThat(registeredUser.getRole().getName()).isEqualTo("CUSTOMER");
    }

    @Test
    void rejectsRegistrationWhenCsrfTokenIsMissing() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Customer")
                        .param("email", "csrf-missing@example.com")
                        .param("phone", "0912345678")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isForbidden());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void rejectsRegistrationWhenCsrfTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf().useInvalidToken())
                        .param("fullName", "Customer")
                        .param("email", "csrf-invalid@example.com")
                        .param("phone", "0912345678")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isForbidden());

        assertThat(userRepository.count()).isZero();
    }

    private User saveExistingUser(String email) {
        User user = new User();
        user.setFullName("Existing User");
        user.setEmail(email);
        user.setPhone("0987654321");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(customerRole);
        return userRepository.saveAndFlush(user);
    }
}
