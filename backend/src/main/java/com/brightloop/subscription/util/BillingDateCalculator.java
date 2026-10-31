package com.brightloop.subscription.util;

import com.brightloop.subscription.domain.enums.BillingFrequency;

import java.time.LocalDate;

public final class BillingDateCalculator {

    private BillingDateCalculator() {}

    /**
     * Calculates the subsequent billing date based on billing frequency,
     * correctly handling month-end boundaries and leap year days.
     */
    public static LocalDate calculateNextBillingDate(LocalDate fromDate, BillingFrequency frequency) {
        if (fromDate == null || frequency == null) {
            throw new IllegalArgumentException("From date and billing frequency must not be null");
        }

        return switch (frequency) {
            case DAILY -> fromDate.plusDays(1);
            case WEEKLY -> fromDate.plusWeeks(1);
            case MONTHLY -> fromDate.plusMonths(1);
            case YEARLY -> fromDate.plusYears(1);
        };
    }

    /**
     * Calculates the period end date for a given billing cycle.
     * Period end is typically 1 day before the next billing date.
     */
    public static LocalDate calculatePeriodEnd(LocalDate periodStart, BillingFrequency frequency) {
        LocalDate nextBilling = calculateNextBillingDate(periodStart, frequency);
        return nextBilling.minusDays(1);
    }
}
