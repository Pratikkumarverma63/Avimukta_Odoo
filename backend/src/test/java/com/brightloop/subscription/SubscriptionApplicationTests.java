package com.brightloop.subscription;

import com.brightloop.subscription.domain.entity.Product;
import com.brightloop.subscription.domain.entity.TaxRule;
import com.brightloop.subscription.domain.entity.Plan;
import com.brightloop.subscription.repository.ProductRepository;
import com.brightloop.subscription.repository.TaxRuleRepository;
import com.brightloop.subscription.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class SubscriptionApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TaxRuleRepository taxRuleRepository;

    @Autowired
    private PlanRepository planRepository;

    @Test
    @DisplayName("Context loads, Flyway executes V1 and V2 migrations successfully")
    void contextLoadsAndFlywaySeedDataPresent() {
        List<Product> products = productRepository.findAll();
        assertThat(products).isNotEmpty();
        assertThat(products).anyMatch(p -> p.getCode().equals("PROD-ERP"));

        List<TaxRule> taxRules = taxRuleRepository.findAll();
        assertThat(taxRules).isNotEmpty();
        assertThat(taxRules).anyMatch(t -> t.getCode().equals("GST_18"));

        List<Plan> plans = planRepository.findAll();
        assertThat(plans).isNotEmpty();
        assertThat(plans).anyMatch(p -> p.getCode().equals("PLAN-ERP-M"));
    }
}
