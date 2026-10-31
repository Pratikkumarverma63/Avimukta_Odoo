package com.brightloop.subscription;

import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.AuthResponse;
import com.brightloop.subscription.web.dto.DashboardMetricsResponse;
import com.brightloop.subscription.web.dto.LoginRequest;
import com.brightloop.subscription.web.dto.RevenueTrendResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DashboardIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String staffToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Admin Login
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

        // Staff Login
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

        // Customer Login
        LoginRequest custLogin = LoginRequest.builder()
                .email("customer@brightloop.com")
                .password("Customer@12345")
                .build();
        MvcResult custRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custLogin)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> custAuth = objectMapper.readValue(
                custRes.getResponse().getContentAsString(), new TypeReference<>() {});
        this.customerToken = custAuth.getData().getToken();
    }

    @Test
    @DisplayName("Admin retrieves financial and subscription metrics successfully")
    void testGetMetricsAsAdmin() throws Exception {
        long startTime = System.currentTimeMillis();

        MvcResult res = mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCustomers").isNumber())
                .andExpect(jsonPath("$.data.monthlyRecurringRevenue").isNumber())
                .andExpect(jsonPath("$.data.annualRunRate").isNumber())
                .andExpect(jsonPath("$.data.totalInvoicedRevenue").isNumber())
                .andExpect(jsonPath("$.data.subscriptionsByStatus").isMap())
                .andExpect(jsonPath("$.data.invoicesByStatus").isMap())
                .andReturn();

        long elapsed = System.currentTimeMillis() - startTime;
        assertThat(elapsed).isLessThan(500); // Sub-second performance

        ApiResponse<DashboardMetricsResponse> metrics = objectMapper.readValue(
                res.getResponse().getContentAsString(), new TypeReference<>() {});
        assertThat(metrics.getData()).isNotNull();
        assertThat(metrics.getData().getTotalCustomers()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Staff can also access executive metrics dashboard")
    void testGetMetricsAsStaff() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCustomers").isNumber());
    }

    @Test
    @DisplayName("Customer is forbidden (403) from accessing admin/staff dashboard metrics")
    void testGetMetricsAsCustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Revenue trend returns 6 months of historical aggregated points")
    void testGetRevenueTrend() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/dashboard/revenue-trend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyPoints").isArray())
                .andReturn();

        ApiResponse<RevenueTrendResponse> trend = objectMapper.readValue(
                res.getResponse().getContentAsString(), new TypeReference<>() {});
        assertThat(trend.getData().getMonthlyPoints()).hasSize(6);
    }
}
