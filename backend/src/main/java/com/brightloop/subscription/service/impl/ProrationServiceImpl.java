package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import com.brightloop.subscription.repository.*;
import com.brightloop.subscription.service.ProrationService;
import com.brightloop.subscription.web.dto.ProrationCalculationRequest;
import com.brightloop.subscription.web.dto.ProrationCalculationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class ProrationServiceImpl implements ProrationService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final ProductVariantRepository variantRepository;
    private final ProrationRecordRepository prorationRecordRepository;

    public ProrationServiceImpl(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            ProductVariantRepository variantRepository,
            ProrationRecordRepository prorationRecordRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.variantRepository = variantRepository;
        this.prorationRecordRepository = prorationRecordRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProrationCalculationResponse calculateProration(Long subscriptionId, ProrationCalculationRequest request) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        Plan newPlan = planRepository.findById(request.getNewPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getNewPlanId()));

        LocalDate effective = request.getEffectiveDate() != null ? request.getEffectiveDate() : LocalDate.now();
        LocalDate cycleStart = sub.getLastBilledDate() != null ? sub.getLastBilledDate() : sub.getStartDate();
        LocalDate cycleEnd = sub.getNextBillingDate() != null ? sub.getNextBillingDate() : cycleStart.plusMonths(1);

        int totalDays = (int) Math.max(1, ChronoUnit.DAYS.between(cycleStart, cycleEnd));
        int daysUsed = (int) Math.max(0, Math.min(totalDays, ChronoUnit.DAYS.between(cycleStart, effective)));
        int daysRemaining = Math.max(0, totalDays - daysUsed);

        BigDecimal currentPrice = sub.getTotalAmount();
        BigDecimal newPrice = newPlan.getPrice();
        if (request.getNewVariantId() != null) {
            ProductVariant variant = variantRepository.findById(request.getNewVariantId()).orElse(null);
            if (variant != null) {
                newPrice = newPrice.add(variant.getPriceAdjustment());
            }
        }

        // Daily rate calculation with high precision
        BigDecimal dailyOld = currentPrice.divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
        BigDecimal dailyNew = newPrice.divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);

        BigDecimal unutilizedCredit = dailyOld.multiply(BigDecimal.valueOf(daysRemaining)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal proratedCharge = dailyNew.multiply(BigDecimal.valueOf(daysRemaining)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAdjustment = proratedCharge.subtract(unutilizedCredit).setScale(2, RoundingMode.HALF_UP);

        String summary = netAdjustment.compareTo(BigDecimal.ZERO) >= 0
                ? String.format("Plan upgrade from %s to %s with %d days remaining in cycle requires net payment of ₹%s",
                sub.getPlan().getName(), newPlan.getName(), daysRemaining, netAdjustment)
                : String.format("Plan change provides customer credit balance of ₹%s towards next billing cycle",
                netAdjustment.abs());

        return ProrationCalculationResponse.builder()
                .subscriptionId(sub.getId())
                .subscriptionNumber(sub.getSubscriptionNumber())
                .currentPlanName(sub.getPlan().getName())
                .targetPlanName(newPlan.getName())
                .effectiveDate(effective)
                .daysUsed(daysUsed)
                .daysRemaining(daysRemaining)
                .totalCycleDays(totalDays)
                .currentPlanAmount(currentPrice)
                .targetPlanAmount(newPrice)
                .unutilizedCreditAmount(unutilizedCredit)
                .proratedChargeAmount(proratedCharge)
                .netAdjustmentAmount(netAdjustment)
                .summaryText(summary)
                .build();
    }

    @Override
    public ProrationCalculationResponse applyProration(Long subscriptionId, ProrationCalculationRequest request) {
        ProrationCalculationResponse calc = calculateProration(subscriptionId, request);
        Subscription sub = subscriptionRepository.findById(subscriptionId).orElseThrow();
        Plan newPlan = planRepository.findById(request.getNewPlanId()).orElseThrow();

        String changeType = calc.getNetAdjustmentAmount().compareTo(BigDecimal.ZERO) >= 0 ? "PLAN_UPGRADE" : "PLAN_DOWNGRADE";

        ProrationRecord record = ProrationRecord.builder()
                .subscription(sub)
                .customer(sub.getCustomer())
                .changeType(changeType)
                .effectiveDate(calc.getEffectiveDate())
                .oldPlanPrice(calc.getCurrentPlanAmount())
                .newPlanPrice(calc.getTargetPlanAmount())
                .daysUsed(calc.getDaysUsed())
                .daysInCycle(calc.getTotalCycleDays())
                .proratedCreditAmount(calc.getUnutilizedCreditAmount())
                .proratedChargeAmount(calc.getProratedChargeAmount())
                .netAdjustmentAmount(calc.getNetAdjustmentAmount())
                .build();

        prorationRecordRepository.save(record);

        // Update Subscription Plan
        sub.setPlan(newPlan);
        sub.setSubtotal(calc.getTargetPlanAmount());
        sub.setTotalAmount(calc.getTargetPlanAmount());
        subscriptionRepository.save(sub);

        return calc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProrationRecord> getProrationsForSubscription(Long subscriptionId) {
        return prorationRecordRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
    }
}
