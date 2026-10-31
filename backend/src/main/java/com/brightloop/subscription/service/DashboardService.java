package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.DashboardMetricsResponse;
import com.brightloop.subscription.web.dto.RevenueTrendResponse;

import java.time.LocalDate;

public interface DashboardService {
    DashboardMetricsResponse getMetrics(LocalDate startDate, LocalDate endDate, Long customerId);
    RevenueTrendResponse getRevenueTrend();
}
