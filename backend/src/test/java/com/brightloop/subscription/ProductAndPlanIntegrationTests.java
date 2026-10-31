package com.brightloop.subscription;

import com.brightloop.subscription.domain.enums.BillingFrequency;
import com.brightloop.subscription.domain.enums.ProductType;
import com.brightloop.subscription.util.BillingDateCalculator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProductAndPlanIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        // Obtain Admin Token
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

        // Obtain Staff Token
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
    }

    @Test
    @DisplayName("Should create product and configurable variant with dynamic pricing adjustment")
    void testProductAndVariantCreation() throws Exception {
        // 1. Create Product
        ProductRequest productReq = ProductRequest.builder()
                .code("PROD-TEST-CRM")
                .name("Brightloop NextGen CRM")
                .productType(ProductType.SOFTWARE)
                .salesPrice(new BigDecimal("2000.00"))
                .costPrice(new BigDecimal("700.00"))
                .description("Cloud CRM platform")
                .active(true)
                .build();

        MvcResult prodResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("PROD-TEST-CRM"))
                .andExpect(jsonPath("$.data.salesPrice").value(2000.00))
                .andReturn();

        ApiResponse<ProductResponse> prodResponse = objectMapper.readValue(
                prodResult.getResponse().getContentAsString(), new TypeReference<>() {});
        Long productId = prodResponse.getData().getId();

        // 2. Add Odoo Brand Variant with +560.00 price adjustment
        ProductVariantRequest variantReq = ProductVariantRequest.builder()
                .attributeName("Brand")
                .variantValue("Odoo")
                .priceAdjustment(new BigDecimal("560.00"))
                .sku("CRM-ODOO")
                .active(true)
                .build();

        MvcResult varResult = mockMvc.perform(post("/api/v1/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variantReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.priceAdjustment").value(560.00))
                .andExpect(jsonPath("$.data.effectivePrice").value(2560.00))
                .andReturn();

        ApiResponse<ProductVariantResponse> varResponse = objectMapper.readValue(
                varResult.getResponse().getContentAsString(), new TypeReference<>() {});
        Long variantId = varResponse.getData().getId();

        // 3. Test Effective Price endpoint
        mockMvc.perform(get("/api/v1/products/" + productId + "/effective-price")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("variantId", variantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2560.00));

        // 4. Without variant -> base price 2000.00
        mockMvc.perform(get("/api/v1/products/" + productId + "/effective-price")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2000.00));
    }

    @Test
    @DisplayName("Should enforce RBAC: Staff cannot create product or plan")
    void testStaffCannotCreateProductOrPlan() throws Exception {
        ProductRequest productReq = ProductRequest.builder()
                .code("PROD-ILLEGAL")
                .name("Illegal Product")
                .productType(ProductType.SOFTWARE)
                .salesPrice(new BigDecimal("1000.00"))
                .costPrice(new BigDecimal("300.00"))
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should create subscription plan and calculate billing dates correctly")
    void testPlanCreationAndBillingDateCalculation() throws Exception {
        // Plan Request
        PlanRequest planReq = PlanRequest.builder()
                .productId(1L) // Seed ERP Product
                .name("ERP Test Monthly Pro")
                .code("PLAN-TEST-M")
                .price(new BigDecimal("2000.00"))
                .billingFrequency(BillingFrequency.MONTHLY)
                .minQuantity(1)
                .autoCloseOnExpiry(true)
                .allowPause(true)
                .allowEarlyClosure(true)
                .allowRenewal(true)
                .active(true)
                .build();

        MvcResult planRes = mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("PLAN-TEST-M"))
                .andExpect(jsonPath("$.data.billingFrequency").value("MONTHLY"))
                .andReturn();

        ApiResponse<PlanResponse> response = objectMapper.readValue(
                planRes.getResponse().getContentAsString(), new TypeReference<>() {});
        Long planId = response.getData().getId();

        // Test Next Billing Date calculation endpoint
        mockMvc.perform(get("/api/v1/plans/" + planId + "/next-billing-date")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("2026-02-15"));
    }

    @Test
    @DisplayName("Test accurate calendar math: month-end, leap-year, daily, weekly, yearly")
    void testBillingDateCalculator_EdgeCases() {
        // 1. Month-end Jan 31 -> Feb 28 in non-leap year (2025)
        LocalDate jan31_2025 = LocalDate.of(2025, 1, 31);
        LocalDate febNext = BillingDateCalculator.calculateNextBillingDate(jan31_2025, BillingFrequency.MONTHLY);
        assertThat(febNext).isEqualTo(LocalDate.of(2025, 2, 28));

        // 2. Month-end Jan 31 -> Feb 29 in leap year (2024)
        LocalDate jan31_2024 = LocalDate.of(2024, 1, 31);
        LocalDate febNextLeap = BillingDateCalculator.calculateNextBillingDate(jan31_2024, BillingFrequency.MONTHLY);
        assertThat(febNextLeap).isEqualTo(LocalDate.of(2024, 2, 29));

        // 3. Leap day Feb 29, 2024 -> Feb 28, 2025 for YEARLY frequency
        LocalDate feb29_2024 = LocalDate.of(2024, 2, 29);
        LocalDate nextYear = BillingDateCalculator.calculateNextBillingDate(feb29_2024, BillingFrequency.YEARLY);
        assertThat(nextYear).isEqualTo(LocalDate.of(2025, 2, 28));

        // 4. Weekly frequency adds exactly 7 days
        LocalDate weekStart = LocalDate.of(2026, 10, 1);
        LocalDate nextWeek = BillingDateCalculator.calculateNextBillingDate(weekStart, BillingFrequency.WEEKLY);
        assertThat(nextWeek).isEqualTo(LocalDate.of(2026, 10, 8));

        // 5. Daily frequency adds 1 day
        LocalDate nextDay = BillingDateCalculator.calculateNextBillingDate(weekStart, BillingFrequency.DAILY);
        assertThat(nextDay).isEqualTo(LocalDate.of(2026, 10, 2));

        // 6. Period end is 1 day before next billing date
        LocalDate periodEnd = BillingDateCalculator.calculatePeriodEnd(jan31_2025, BillingFrequency.MONTHLY);
        assertThat(periodEnd).isEqualTo(LocalDate.of(2025, 2, 27));
    }
}
