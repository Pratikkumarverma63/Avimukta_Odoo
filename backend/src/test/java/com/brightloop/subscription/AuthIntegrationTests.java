package com.brightloop.subscription;

import com.brightloop.subscription.domain.enums.Role;
import com.brightloop.subscription.repository.UserRepository;
import com.brightloop.subscription.service.impl.MockEmailService;
import com.brightloop.subscription.web.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockEmailService emailService;

    @Test
    @DisplayName("Should successfully register a new customer with compliant password")
    void testCustomerRegistration_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newcustomer@test.com")
                .password("SecurePass@2026")
                .firstName("John")
                .lastName("Doe")
                .companyName("Doe Enterprises")
                .phone("+91-9123456789")
                .billingAddress("123 Tech Park, Hyderabad")
                .taxId("GSTIN36AAAAA0000A1Z5")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newcustomer@test.com"))
                .andExpect(jsonPath("$.data.role").value(Role.ROLE_CUSTOMER.name()))
                .andExpect(jsonPath("$.data.customerId").isNotEmpty())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject registration if password does not meet security criteria")
    void testCustomerRegistration_InvalidPassword() throws Exception {
        // <= 8 characters
        RegisterRequest shortPw = RegisterRequest.builder()
                .email("short@test.com")
                .password("Ab@123")
                .firstName("Short")
                .lastName("Test")
                .companyName("Short Corp")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPw)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Missing special character
        RegisterRequest noSpecial = RegisterRequest.builder()
                .email("nospecial@test.com")
                .password("Password1234")
                .firstName("No")
                .lastName("Special")
                .companyName("Corp")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noSpecial)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Missing uppercase
        RegisterRequest noUpper = RegisterRequest.builder()
                .email("noupper@test.com")
                .password("password@1234")
                .firstName("No")
                .lastName("Upper")
                .companyName("Corp")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noUpper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should successfully authenticate existing Admin user and return JWT")
    void testAdminLogin_Success() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@brightloop.com")
                .password("Admin@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value(Role.ROLE_ADMIN.name()))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject authentication with invalid password")
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@brightloop.com")
                .password("WrongPassword@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin can create Staff user; Non-Admin cannot create Staff")
    void testAdminCreateStaff_RBAC() throws Exception {
        // 1. Login as Admin
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@brightloop.com")
                .password("Admin@12345")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        String adminToken = authResponse.getData().getToken();

        // 2. Admin creates a new staff account
        StaffCreateRequest staffReq = StaffCreateRequest.builder()
                .email("agent.smith@brightloop.com")
                .password("Staff@Secure2026")
                .firstName("Agent")
                .lastName("Smith")
                .build();

        mockMvc.perform(post("/api/v1/admin/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value(Role.ROLE_STAFF.name()));

        // 3. Register a fresh customer who tries to create staff -> Forbidden (403)
        RegisterRequest customerRegister = RegisterRequest.builder()
                .email("rbac.customer@test.com")
                .password("Customer@12345")
                .firstName("RBAC")
                .lastName("Customer")
                .companyName("RBAC Ltd")
                .build();

        MvcResult custRegRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRegister)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<AuthResponse> custAuthRes = objectMapper.readValue(
                custRegRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        String custToken = custAuthRes.getData().getToken();

        StaffCreateRequest illegalStaffReq = StaffCreateRequest.builder()
                .email("rogue@brightloop.com")
                .password("Rogue@12345")
                .firstName("Rogue")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/admin/staff")
                        .header("Authorization", "Bearer " + custToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalStaffReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should successfully handle forgot password and reset token flow")
    void testForgotPasswordAndResetFlow() throws Exception {
        // Register a customer for reset testing
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("reset.flow@test.com")
                .password("OldPass@12345")
                .firstName("Reset")
                .lastName("Tester")
                .companyName("Reset Corp")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        ForgotPasswordRequest forgotRequest = ForgotPasswordRequest.builder()
                .email("reset.flow@test.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());

        String resetToken = emailService.getLastResetTokenForEmail("reset.flow@test.com");
        assertThat(resetToken).isNotNull();

        // Perform password reset
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(resetToken)
                .newPassword("NewCustomer@9999")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify login with new password succeeds
        LoginRequest newLogin = LoginRequest.builder()
                .email("reset.flow@test.com")
                .password("NewCustomer@9999")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk());

        // Token cannot be reused (single-use token)
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest());
    }
}
