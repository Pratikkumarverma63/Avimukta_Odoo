package com.brightloop.subscription.service;

import com.brightloop.subscription.domain.entity.DiscountRule;
import com.brightloop.subscription.domain.entity.Product;
import com.brightloop.subscription.domain.entity.ProductVariant;
import com.brightloop.subscription.domain.entity.TaxRule;
import com.brightloop.subscription.domain.enums.DiscountType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class PricingCalculationService {

    public record LineCalculationResult(
            BigDecimal unitPrice,
            BigDecimal lineSubtotal,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal lineTotal
    ) {}

    public BigDecimal resolveUnitPrice(Product product, ProductVariant variant, BigDecimal overridePrice) {
        if (overridePrice != null && overridePrice.compareTo(BigDecimal.ZERO) > 0) {
            return overridePrice.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal price = product.getSalesPrice();
        if (variant != null && variant.getPriceAdjustment() != null) {
            price = price.add(variant.getPriceAdjustment());
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public LineCalculationResult calculateLineItem(
            Product product,
            ProductVariant variant,
            int quantity,
            BigDecimal overrideUnitPrice,
            TaxRule taxRule,
            DiscountRule discountRule,
            LocalDate calculationDate) {

        BigDecimal unitPrice = resolveUnitPrice(product, variant, overrideUnitPrice);
        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal lineSubtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountRule != null && discountRule.isEligible(lineSubtotal, quantity, calculationDate)) {
            if (discountRule.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = lineSubtotal.multiply(discountRule.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                discountAmount = discountRule.getDiscountValue().min(lineSubtotal);
            }
        }
        discountAmount = discountAmount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxableAmount = lineSubtotal.subtract(discountAmount).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (taxRule != null && taxRule.isActive()) {
            if (taxRule.isPercentage()) {
                taxAmount = taxableAmount.multiply(taxRule.getRatePercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                taxAmount = taxRule.getRatePercent().setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal lineTotal = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        return new LineCalculationResult(
                unitPrice,
                lineSubtotal,
                discountAmount,
                taxableAmount,
                taxAmount,
                lineTotal
        );
    }
}
