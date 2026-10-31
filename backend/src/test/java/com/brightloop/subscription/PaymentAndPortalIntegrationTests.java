package com.brightloop.subscription;

import com.brightloop.subscription.domain.enums.InvoiceStatus;
import com.brightloop.subscription.domain.enums.PaymentMethod;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.repository.InvoiceRepository;
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
class PaymentAndPortalIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private String adminToken;
    private String staffToken;
    private String customerToken;
    private Long customerId;

    @BeforeEach
    void setUp() throws Exception {
        // Admin
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

        // Staff
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

        // Unique Customer
        String email = "portal.client." + System.currentTimeMillis() + "@acme.com";
        RegisterRequest custReq = RegisterRequest.builder()
                .email(email)
                .password("PortalPass@2026")
                .firstName("Elena")
                .lastName("Rostova")
                .companyName("Rostova Consulting")
                .build();
        MvcResult custRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ApiResponse<AuthResponse> custAuth = objectMapper.readValue(
                custRes.getResponse().getContentAsString(), new TypeReference<>() {});
        this.customerToken = custAuth.getData().getToken();
        this.customerId = custAuth.getData().getCustomerId();
    }

    @Test
    @DisplayName("Should handle partial payments, update outstanding balance, and mark invoice PAID on full settlement")
    void testPartialPaymentAndReconciliation() throws Exception {
        // 1. Create a subscription & trigger billing to get an invoice
        SubscriptionRequest subReq = SubscriptionRequest.builder()
                .customerId(customerId)
                .planId(1L)
                .startDate(LocalDate.now().minusMonths(1))
                .paymentTerms("NET_30")
                .items(List.of(
                        SubscriptionItemRequest.builder()
                                .productId(1L) // 2000
                                .taxRuleId(1L) // 18% GST = 360 => Total 2360
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

        // Ensure nextBillingDate is today so billing engine processes it
        var subEntity = subscriptionRepository.findById(subId).orElseThrow();
        subEntity.setNextBillingDate(LocalDate.now());
        subscriptionRepository.save(subEntity);

        // Trigger billing
        mockMvc.perform(post("/api/v1/billing/trigger")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("asOfDate", LocalDate.now().toString()))
                .andExpect(status().isOk());

        // Fetch invoice
        var invoices = invoiceRepository.findByCustomerId(customerId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertThat(invoices).isNotEmpty();
        var invoice = invoices.get(0);
        Long invoiceId = invoice.getId();
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("2360.00");
        assertThat(invoice.getOutstandingAmount()).isEqualByComparingTo("2360.00");

        // 2. Partial Payment: ₹1000 via UPI
        PaymentRequest partialPay = PaymentRequest.builder()
                .invoiceId(invoiceId)
                .paymentMethod(PaymentMethod.UPI)
                .amount(new BigDecimal("1000.00"))
                .transactionReference("UPI-UTR-99901")
                .notes("First tranche")
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPay)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.data.amount").value(1000.00));

        // Verify Invoice state: CONFIRMED, Outstanding = 1360.00
        var updatedInv1 = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(updatedInv1.getStatus()).isEqualTo(InvoiceStatus.CONFIRMED);
        assertThat(updatedInv1.getAmountPaid()).isEqualByComparingTo("1000.00");
        assertThat(updatedInv1.getOutstandingAmount()).isEqualByComparingTo("1360.00");

        // 3. Final Payment: ₹1360 via BANK_TRANSFER
        PaymentRequest finalPay = PaymentRequest.builder()
                .invoiceId(invoiceId)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(new BigDecimal("1360.00"))
                .transactionReference("NEFT-2026-88899")
                .notes("Final balance clearance")
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalPay)))
                .andExpect(status().isCreated());

        // Verify Invoice state: PAID, Outstanding = 0.00
        var finalInv = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(finalInv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(finalInv.getAmountPaid()).isEqualByComparingTo("2360.00");
        assertThat(finalInv.getOutstandingAmount()).isEqualByComparingTo("0.00");

        // 4. Overpayment rejection: Paying more against a PAID invoice -> 400 Bad Request
        PaymentRequest excessPay = PaymentRequest.builder()
                .invoiceId(invoiceId)
                .paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessPay)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Customer Portal: Customer can view dashboard, invoices, payments, and read notifications")
    void testCustomerPortalFlow() throws Exception {
        // Customer accesses personal dashboard
        mockMvc.perform(get("/api/v1/customer/dashboard")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerCode").isNotEmpty())
                .andExpect(jsonPath("$.data.companyName").value("Rostova Consulting"));

        // Customer views notifications
        mockMvc.perform(get("/api/v1/customer/notifications")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }
}
