package com.brightloop.subscription;

import com.brightloop.subscription.domain.entity.Customer;
import com.brightloop.subscription.domain.entity.Plan;
import com.brightloop.subscription.domain.entity.Product;
import com.brightloop.subscription.domain.entity.Subscription;
import com.brightloop.subscription.domain.entity.SubscriptionItem;
import com.brightloop.subscription.domain.enums.BillingFrequency;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.repository.CustomerRepository;
import com.brightloop.subscription.repository.PlanRepository;
import com.brightloop.subscription.repository.ProductRepository;
import com.brightloop.subscription.repository.SubscriptionRepository;
import com.brightloop.subscription.service.BillingEngineService;
import com.brightloop.subscription.service.DashboardService;
import com.brightloop.subscription.web.dto.BillingExecutionSummaryResponse;
import com.brightloop.subscription.web.dto.DashboardMetricsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class BenchmarkPerformanceTests {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private BillingEngineService billingEngineService;

    @Test
    @DisplayName("Performance Benchmark: High-scale aggregation under 2.0 seconds with 1,000 subscriptions")
    void testPerformanceBenchmarkUnderTwoSeconds() {
        Customer customer = customerRepository.findAll().stream().findFirst().orElseThrow();
        Plan plan = planRepository.findAll().stream().findFirst().orElseThrow();
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();

        // 1. Bulk generate 1,000 active subscriptions
        List<Subscription> bulkSubs = new ArrayList<>(1000);
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 1000; i++) {
            Subscription sub = Subscription.builder()
                    .subscriptionNumber(String.format("SUB-BENCH-%05d", i))
                    .customer(customer)
                    .plan(plan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startDate(today.minusMonths(1))
                    .nextBillingDate(today.plusDays(i % 30))
                    .paymentTerms("Net 30")
                    .subtotal(new BigDecimal("2000.00"))
                    .taxAmount(new BigDecimal("360.00"))
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("2360.00"))
                    .build();

            SubscriptionItem item = SubscriptionItem.builder()
                    .subscription(sub)
                    .product(product)
                    .quantity(1)
                    .unitPrice(new BigDecimal("2000.00"))
                    .taxAmount(new BigDecimal("360.00"))
                    .discountAmount(BigDecimal.ZERO)
                    .lineTotal(new BigDecimal("2360.00"))
                    .build();

            sub.setItems(new ArrayList<>(List.of(item)));
            bulkSubs.add(sub);
        }

        subscriptionRepository.saveAll(bulkSubs);

        // 2. Measure Dashboard aggregation time under load
        long startMetricTime = System.currentTimeMillis();
        DashboardMetricsResponse metrics = dashboardService.getMetrics(null, null, null);
        long metricDurationMs = System.currentTimeMillis() - startMetricTime;

        // Verify aggregation metrics and performance SLA (< 2000ms)
        assertThat(metrics).isNotNull();
        assertThat(metrics.getActiveSubscriptions()).isGreaterThanOrEqualTo(1000);
        assertThat(metricDurationMs)
                .withFailMessage("Dashboard aggregation took %d ms, which exceeds the 2000ms SLA", metricDurationMs)
                .isLessThan(2000);

        // 3. Test Billing Engine execution speed
        long startBillingTime = System.currentTimeMillis();
        BillingExecutionSummaryResponse billingSummary = billingEngineService.runBillingJob(today);
        long billingDurationMs = System.currentTimeMillis() - startBillingTime;

        assertThat(billingSummary).isNotNull();
        assertThat(billingDurationMs)
                .withFailMessage("Billing engine run took %d ms, which exceeds the 2000ms SLA", billingDurationMs)
                .isLessThan(2000);
    }
}
