package com.brightloop.subscription.service;

import com.brightloop.subscription.domain.entity.DunningCase;

import java.util.List;

public interface DunningService {
    List<DunningCase> getAllActiveCases();
    int processOverdueInvoices();
}
