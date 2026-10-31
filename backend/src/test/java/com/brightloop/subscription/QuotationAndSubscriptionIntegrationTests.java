package com.brightloop.subscription;

import com.brightloop.subscription.domain.enums.QuotationStatus;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.web.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class QuotationAndSubscriptionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String staffToken;
    private String customer1Token;
    private Long customer1Id;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Admin login
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@brightloop.com")
                .password("Admin@12345")
                .build();
        MvcResult adminRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> adminAuth = objectMapper.readValue(
                adminRes.getResponse().getContentAsString(), new TypeReference<>() {});
        this.adminToken = adminAuth.getData().getToken();

        // 2. Staff login
        LoginRequest staffLogin = LoginRequest.builder()
                .email("staff@brightloop.com")
                .password("Staff@12345")
                .build();
        MvcResult staffRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffLogin)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> staffAuth = objectMapper.readValue(
                staffRes.getResponse().getContentAsString(), new TypeReference<>() {});
        this.staffToken = staffAuth.getData().getToken();

        // 3. Register Customer 1
        String uniqueEmail = "client1." + System.currentTimeMillis() + "@acme.com";
        RegisterRequest custReq = RegisterRequest.builder()
                .email(uniqueEmail)
                .password("ClientPass@2026")
                .firstName("Alice")
                .lastName("Walker")
                .companyName("Walker Tech")
                .build();
        MvcResult custRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ApiResponse<AuthResponse> custAuth = objectMapper.readValue(
                custRes.getResponse().getContentAsString(), new TypeReference<>() {});
        this.customer1Token = custAuth.getData().getToken();
        this.customer1Id = custAuth.getData().getCustomerId();
    }

    @Test
    @DisplayName("Should instantiate quotation from template, dispatch it, and convert to active subscription")
    void testQuoteFromTemplateAndConvertToSubscription() throws Exception {
        // 1. Staff creates quotation from Seed Template #1 (Standard Business Starter)
        MvcResult quoteRes = mockMvc.perform(post("/api/v1/quotations/from-template/1")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("customerId", customer1Id.toString())
                        .param("paymentTerms", "NET_30"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items").isNotEmpty())
                .andReturn();

        ApiResponse<QuotationResponse> quoteResponse = objectMapper.readValue(
                quoteRes.getResponse().getContentAsString(), new TypeReference<>() {});
        Long quotationId = quoteResponse.getData().getId();

        // 2. Send quotation to customer
        mockMvc.perform(post("/api/v1/quotations/" + quotationId + "/send")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUOTATION"));

        // 3. Confirm quotation -> automatically transitions to Active Subscription!
        MvcResult confirmRes = mockMvc.perform(post("/api/v1/quotations/" + quotationId + "/confirm")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.nextBillingDate").isNotEmpty())
                .andReturn();

        ApiResponse<SubscriptionResponse> subResponse = objectMapper.readValue(
                confirmRes.getResponse().getContentAsString(), new TypeReference<>() {});
        Long subscriptionId = subResponse.getData().getId();
        assertThat(subscriptionId).isNotNull();

        // 4. Verify Quotation is now CONFIRMED
        mockMvc.perform(get("/api/v1/quotations/" + quotationId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Should enforce Subscription lifecycle state transitions: Pause, Resume, Close, Renew")
    void testSubscriptionLifecycleTransitions() throws Exception {
        // 1. Direct subscription creation
        SubscriptionRequest subReq = SubscriptionRequest.builder()
                .customerId(customer1Id)
                .planId(1L) // Seed Monthly Plan
                .paymentTerms("NET_15")
                .allowPause(true)
                .allowEarlyClosure(true)
                .allowRenewal(true)
                .items(List.of(
                        SubscriptionItemRequest.builder()
                                .productId(1L)
                                .variantId(2L) // Odoo Variant (+560)
                                .quantity(2)
                                .build()
                ))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/subscriptions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value(SubscriptionStatus.ACTIVE.name()))
                .andReturn();

        ApiResponse<SubscriptionResponse> subRes = objectMapper.readValue(
                createRes.getResponse().getContentAsString(), new TypeReference<>() {});
        Long subId = subRes.getData().getId();

        // 2. Pause Subscription -> PAUSED
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAUSED"));

        // Cannot pause already paused subscription -> 400 Bad Request
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // 3. Resume Subscription -> ACTIVE
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/resume")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 4. Close Subscription (early closure) -> CLOSED
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // Cannot pause closed subscription
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // 5. Renew Subscription -> ACTIVE with extended expiry
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/renew")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.expiryDate").isNotEmpty());
    }

    @Test
    @DisplayName("Customer Data Isolation: Customer B cannot access Customer A's subscription")
    void testCustomerDataIsolation() throws Exception {
        // Create subscription for Customer 1
        SubscriptionRequest subReq = SubscriptionRequest.builder()
                .customerId(customer1Id)
                .planId(1L)
                .items(List.of(SubscriptionItemRequest.builder().productId(1L).quantity(1).build()))
                .build();

        MvcResult subRes = mockMvc.perform(post("/api/v1/subscriptions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<SubscriptionResponse> subObj = objectMapper.readValue(
                subRes.getResponse().getContentAsString(), new TypeReference<>() {});
        Long subId = subObj.getData().getId();

        // Register a distinct Customer 2
        String email2 = "client2." + System.currentTimeMillis() + "@acme.com";
        RegisterRequest cust2Req = RegisterRequest.builder()
                .email(email2)
                .password("Client2Pass@2026")
                .firstName("Bob")
                .lastName("Miller")
                .companyName("Miller Global")
                .build();

        MvcResult cust2Res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cust2Req)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<AuthResponse> cust2Auth = objectMapper.readValue(
                cust2Res.getResponse().getContentAsString(), new TypeReference<>() {});
        String customer2Token = cust2Auth.getData().getToken();

        // Customer 1 can access own subscription
        mockMvc.perform(get("/api/v1/subscriptions/" + subId)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        // Customer 2 tries to access Customer 1's subscription -> 403 Forbidden!
        mockMvc.perform(get("/api/v1/subscriptions/" + subId)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }
}
