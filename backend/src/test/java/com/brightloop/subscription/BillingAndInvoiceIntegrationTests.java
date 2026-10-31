package com.brightloop.subscription;

import com.brightloop.subscription.domain.entity.Notification;
import com.brightloop.subscription.domain.enums.DiscountType;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.repository.InvoiceRepository;
import com.brightloop.subscription.repository.NotificationRepository;
import com.brightloop.subscription.repository.SubscriptionRepository;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BillingAndInvoiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String adminToken;
    private String staffToken;
    private Long customerId;

    @BeforeEach
    void setUp() throws Exception {
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

        // Register unique customer
        String email = "billing.client." + System.currentTimeMillis() + "@acme.com";
        RegisterRequest custReq = RegisterRequest.builder()
                .email(email)
                .password("BillingPass@2026")
                .firstName("Robert")
                .lastName("Fox")
                .companyName("Fox Logistics")
                .build();
        MvcResult custRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ApiResponse<AuthResponse> custAuth = objectMapper.readValue(
                custRes.getResponse().getContentAsString(), new TypeReference<>() {});
        this.customerId = custAuth.getData().getCustomerId();
    }

    @Test
    @DisplayName("Admin can configure tax rules and discounts with usage limit validation")
    void testTaxAndDiscountManagement() throws Exception {
        // 1. Create Tax Rule (e.g. 12% Special Tax)
        TaxRuleRequest taxReq = TaxRuleRequest.builder()
                .name("State Luxury Tax 12%")
                .code("LUX_12_" + System.currentTimeMillis())
                .ratePercent(new BigDecimal("12.0000"))
                .percentage(true)
                .applicability("ALL")
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/taxes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taxReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ratePercent").value(12.0000));

        // 2. Staff cannot create tax rule -> 403
        mockMvc.perform(post("/api/v1/taxes")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taxReq)))
                .andExpect(status().isForbidden());

        // 3. Admin creates discount rule with usage limit of 1
        String discCode = "LIMITED500_" + System.currentTimeMillis();
        DiscountRuleRequest discReq = DiscountRuleRequest.builder()
                .code(discCode)
                .name("One-time voucher ₹500")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("500.00"))
                .minPurchaseAmount(new BigDecimal("1500.00"))
                .maxUses(1)
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/discounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(discReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(discCode))
                .andExpect(jsonPath("$.data.remainingUses").value(1));

        // 4. Validate discount: Below min purchase -> ineligible
        DiscountValidationRequest invalidPurchase = DiscountValidationRequest.builder()
                .code(discCode)
                .purchaseAmount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/discounts/validate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPurchase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(false));

        // 5. Validate discount: Above min purchase -> eligible
        DiscountValidationRequest validPurchase = DiscountValidationRequest.builder()
                .code(discCode)
                .purchaseAmount(new BigDecimal("2000.00"))
                .build();

        mockMvc.perform(post("/api/v1/discounts/validate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPurchase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.calculatedDiscount").value(500.00));
    }

    @Test
    @DisplayName("Automated Recurring Billing Engine generates invoice, advances billing date, and prevents duplicate invoices")
    void testRecurringBillingEngine_IdempotentAndNoDuplicates() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Create an active subscription due today
        SubscriptionRequest subReq = SubscriptionRequest.builder()
                .customerId(customerId)
                .planId(1L) // Monthly Plan
                .startDate(today.minusMonths(1))
                .paymentTerms("NET_30")
                .items(List.of(
                        SubscriptionItemRequest.builder()
                                .productId(1L) // ERP base 2000
                                .taxRuleId(1L) // 18% GST
                                .quantity(1)
                                .build()
                ))
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

        // Manually set nextBillingDate to today to simulate billing cycle arrival
        var subEntity = subscriptionRepository.findById(subId).orElseThrow();
        subEntity.setNextBillingDate(today);
        subscriptionRepository.save(subEntity);

        long initialInvoiceCount = invoiceRepository.count();

        // 2. Trigger Billing Engine
        MvcResult billRes = mockMvc.perform(post("/api/v1/billing/trigger")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("asOfDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoicesGenerated").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andReturn();

        ApiResponse<BillingExecutionSummaryResponse> billSummary = objectMapper.readValue(
                billRes.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(invoiceRepository.count()).isEqualTo(initialInvoiceCount + 1);

        // 3. Verify Subscription billing state advanced
        var updatedSub = subscriptionRepository.findById(subId).orElseThrow();
        assertThat(updatedSub.getLastBilledDate()).isEqualTo(today);
        assertThat(updatedSub.getNextBillingDate()).isAfter(today);

        // 4. Verify Customer In-App Notification generated
        List<Notification> notifications = notificationRepository.findByCustomerIdAndReadFalse(customerId);
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getTitle()).isEqualTo("New Invoice Generated");

        // 5. CRITICAL TEST: Run billing engine AGAIN for the same date!
        // Should detect period already billed, SKIP it, and generate 0 new invoices!
        MvcResult duplicateRes = mockMvc.perform(post("/api/v1/billing/trigger")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("asOfDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoicesGenerated").value(0))
                .andReturn();

        // Total invoices in DB unchanged!
        assertThat(invoiceRepository.count()).isEqualTo(initialInvoiceCount + 1);
    }
}
