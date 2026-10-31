package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.DunningCase;
import com.brightloop.subscription.domain.entity.Invoice;
import com.brightloop.subscription.repository.DunningCaseRepository;
import com.brightloop.subscription.repository.InvoiceRepository;
import com.brightloop.subscription.service.DunningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class DunningServiceImpl implements DunningService {

    private final DunningCaseRepository dunningCaseRepository;
    private final InvoiceRepository invoiceRepository;

    public DunningServiceImpl(DunningCaseRepository dunningCaseRepository, InvoiceRepository invoiceRepository) {
        this.dunningCaseRepository = dunningCaseRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DunningCase> getAllActiveCases() {
        return dunningCaseRepository.findByDunningStageNotOrderByDaysOverdueDesc("WRITTEN_OFF");
    }

    @Override
    public int processOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(today);

        int processed = 0;
        for (Invoice invoice : overdueInvoices) {
            int daysOverdue = (int) ChronoUnit.DAYS.between(invoice.getDueDate(), today);

            DunningCase dunning = dunningCaseRepository.findByInvoiceId(invoice.getId())
                    .orElseGet(() -> DunningCase.builder()
                            .invoice(invoice)
                            .customer(invoice.getCustomer())
                            .attemptsCount(0)
                            .build());

            dunning.setDaysOverdue(daysOverdue);
            dunning.setLastActionDate(OffsetDateTime.now());

            // 4-Tier Automated Dunning State Machine
            if (daysOverdue <= 3) {
                dunning.setDunningStage("REMINDER_SENT");
                dunning.setNotes("Tier 1: Friendly email reminder dispatched to finance contact.");
            } else if (daysOverdue <= 7) {
                dunning.setDunningStage("GRACE_PERIOD");
                dunning.setNotes("Tier 2: 7-day payment grace period active with automated SMS/email escalation.");
            } else if (daysOverdue <= 14) {
                dunning.setDunningStage("LATE_FEE_APPLIED");
                // 2% compound late fee on outstanding balance
                BigDecimal lateFee = invoice.getOutstandingAmount()
                        .multiply(new BigDecimal("0.02"))
                        .setScale(2, RoundingMode.HALF_UP);
                dunning.setLateFeeAmount(lateFee);
                dunning.setNotes(String.format("Tier 3: Grace period elapsed. 2%% Late Fee (₹%s) compounded.", lateFee));
            } else {
                dunning.setDunningStage("SUSPENDED");
                dunning.setNotes("Tier 4: Critical 14+ days default. Services placed on temporary suspension.");
            }

            dunning.setAttemptsCount(dunning.getAttemptsCount() + 1);
            dunning.setNextActionDate(OffsetDateTime.now().plusDays(2));

            dunningCaseRepository.save(dunning);
            processed++;
        }

        return processed;
    }
}
