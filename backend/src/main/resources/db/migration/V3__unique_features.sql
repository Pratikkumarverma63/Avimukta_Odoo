-- Brightloop Software Subscription Management System
-- V3__unique_features.sql: Advanced Database Architectural Extensions
-- Proration Credit Engine & Smart Dunning Revenue Recovery

-- 1. Proration Ledger for Mid-cycle Upgrades/Downgrades
CREATE TABLE IF NOT EXISTS proration_records (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    change_type VARCHAR(50) NOT NULL,
    effective_date DATE NOT NULL,
    old_plan_price NUMERIC(14,2) NOT NULL,
    new_plan_price NUMERIC(14,2) NOT NULL,
    days_used INT NOT NULL,
    days_in_cycle INT NOT NULL,
    prorated_credit_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    prorated_charge_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    net_adjustment_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_proration_sub ON proration_records(subscription_id);
CREATE INDEX IF NOT EXISTS idx_proration_cust ON proration_records(customer_id);

-- 2. Smart Dunning & Automated Revenue Recovery Engine
CREATE TABLE IF NOT EXISTS dunning_cases (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    dunning_stage VARCHAR(50) NOT NULL DEFAULT 'HEALTHY',
    days_overdue INT NOT NULL DEFAULT 0,
    late_fee_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    last_action_date TIMESTAMP WITH TIME ZONE,
    next_action_date TIMESTAMP WITH TIME ZONE,
    attempts_count INT NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dunning_invoice ON dunning_cases(invoice_id);
CREATE INDEX IF NOT EXISTS idx_dunning_stage ON dunning_cases(dunning_stage);
