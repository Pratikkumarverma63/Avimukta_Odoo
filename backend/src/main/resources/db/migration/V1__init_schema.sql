-- Brightloop Software Subscription Management System
-- V1__init_schema.sql: Initial Database Schema

-- 1. Users & Authentication
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    billing_address TEXT,
    tax_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_code ON customers(customer_code);
CREATE INDEX idx_customers_email ON customers(email);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pwd_reset_token ON password_reset_tokens(token_hash);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Products & Variants
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    product_type VARCHAR(50) NOT NULL, -- SOFTWARE, ADD_ON, SERVICE
    sales_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    cost_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_code ON products(code);
CREATE INDEX idx_products_active ON products(active);

CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attribute_name VARCHAR(100) NOT NULL, -- e.g. "Brand", "Edition"
    variant_value VARCHAR(100) NOT NULL,  -- e.g. "Odoo", "Enterprise"
    price_adjustment NUMERIC(14,2) NOT NULL DEFAULT 0.00, -- e.g. 560.00
    sku VARCHAR(100) UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_variants_product ON product_variants(product_id);

-- 3. Taxes & Discounts
CREATE TABLE tax_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    rate_percent NUMERIC(7,4) NOT NULL DEFAULT 0.0000, -- e.g. 18.0000%
    is_percentage BOOLEAN NOT NULL DEFAULT TRUE,
    applicability VARCHAR(50) NOT NULL DEFAULT 'ALL',
    effective_start_date DATE,
    effective_end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tax_rules_code ON tax_rules(code);

CREATE TABLE discount_rules (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(50) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value NUMERIC(14,2) NOT NULL,
    min_purchase_amount NUMERIC(14,2) DEFAULT 0.00,
    min_quantity INT DEFAULT 1,
    start_date DATE,
    end_date DATE,
    max_uses INT DEFAULT 0, -- 0 means unlimited
    used_count INT NOT NULL DEFAULT 0,
    scope VARCHAR(50) NOT NULL DEFAULT 'SUBSCRIPTION_LEVEL', -- PRODUCT_SPECIFIC, SUBSCRIPTION_LEVEL
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_discounts_code ON discount_rules(code);

CREATE TABLE discount_rule_products (
    discount_rule_id BIGINT NOT NULL REFERENCES discount_rules(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    PRIMARY KEY (discount_rule_id, product_id)
);

-- 4. Subscription Plans
CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    price NUMERIC(14,2) NOT NULL,
    billing_frequency VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    min_quantity INT NOT NULL DEFAULT 1,
    availability_start_date DATE,
    availability_end_date DATE,
    auto_close_on_expiry BOOLEAN NOT NULL DEFAULT TRUE,
    allow_pause BOOLEAN NOT NULL DEFAULT TRUE,
    allow_early_closure BOOLEAN NOT NULL DEFAULT TRUE,
    allow_renewal BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plans_code ON plans(code);
CREATE INDEX idx_plans_product ON plans(product_id);

-- 5. Quotation Templates & Quotations
CREATE TABLE quotation_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    default_plan_id BIGINT REFERENCES plans(id) ON DELETE SET NULL,
    validity_days INT NOT NULL DEFAULT 30,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE quotation_template_items (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES quotation_templates(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    default_quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(14,2) NOT NULL DEFAULT 0.00
);

CREATE TABLE quotations (
    id BIGSERIAL PRIMARY KEY,
    quotation_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    plan_id BIGINT NOT NULL REFERENCES plans(id) ON DELETE RESTRICT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, QUOTATION, CONFIRMED, EXPIRED, CANCELLED
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    payment_terms VARCHAR(50) NOT NULL DEFAULT 'NET_30',
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quotations_customer ON quotations(customer_id);
CREATE INDEX idx_quotations_status ON quotations(status);

CREATE TABLE quotation_items (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    description VARCHAR(255),
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_rule_id BIGINT REFERENCES tax_rules(id) ON DELETE SET NULL,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    discount_rule_id BIGINT REFERENCES discount_rules(id) ON DELETE SET NULL,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(14,2) NOT NULL DEFAULT 0.00
);

CREATE INDEX idx_quotation_items_quote ON quotation_items(quotation_id);

-- 6. Subscriptions
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    subscription_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    plan_id BIGINT NOT NULL REFERENCES plans(id) ON DELETE RESTRICT,
    quotation_id BIGINT REFERENCES quotations(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, QUOTATION, CONFIRMED, ACTIVE, PAUSED, CLOSED
    start_date DATE NOT NULL,
    expiry_date DATE,
    next_billing_date DATE,
    last_billed_date DATE,
    payment_terms VARCHAR(50) NOT NULL DEFAULT 'NET_30',
    auto_close_on_expiry BOOLEAN NOT NULL DEFAULT TRUE,
    allow_pause BOOLEAN NOT NULL DEFAULT TRUE,
    allow_early_closure BOOLEAN NOT NULL DEFAULT TRUE,
    allow_renewal BOOLEAN NOT NULL DEFAULT TRUE,
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subs_customer ON subscriptions(customer_id);
CREATE INDEX idx_subs_status ON subscriptions(status);
CREATE INDEX idx_subs_next_billing ON subscriptions(next_billing_date);

CREATE TABLE subscription_items (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    description VARCHAR(255),
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_rule_id BIGINT REFERENCES tax_rules(id) ON DELETE SET NULL,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    discount_rule_id BIGINT REFERENCES discount_rules(id) ON DELETE SET NULL,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(14,2) NOT NULL DEFAULT 0.00
);

CREATE INDEX idx_sub_items_sub ON subscription_items(subscription_id);

-- 7. Invoices
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    subscription_id BIGINT REFERENCES subscriptions(id) ON DELETE SET NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, CONFIRMED, PAID, CANCELLED, OVERDUE
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    amount_paid NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    outstanding_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    billing_period_start DATE,
    billing_period_end DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_subscription ON invoices(subscription_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);

CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    description VARCHAR(255),
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_rule_id BIGINT REFERENCES tax_rules(id) ON DELETE SET NULL,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    discount_rule_id BIGINT REFERENCES discount_rules(id) ON DELETE SET NULL,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(14,2) NOT NULL DEFAULT 0.00
);

CREATE INDEX idx_invoice_items_inv ON invoice_items(invoice_id);

-- 8. Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    payment_method VARCHAR(50) NOT NULL, -- UPI, BANK_TRANSFER, CARD, CASH, OTHER
    amount NUMERIC(14,2) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transaction_reference VARCHAR(100),
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED', -- COMPLETED, REFUNDED
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_date ON payments(payment_date);

-- 9. Discount Usages Tracking
CREATE TABLE discount_usages (
    id BIGSERIAL PRIMARY KEY,
    discount_rule_id BIGINT NOT NULL REFERENCES discount_rules(id) ON DELETE CASCADE,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE SET NULL,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    discount_amount NUMERIC(14,2) NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_discount_usages_rule ON discount_usages(discount_rule_id);

-- 10. Billing Engine Execution Records (Guarantees zero duplicate invoices)
CREATE TABLE billing_records (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    execution_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL, -- SUCCESS, FAILED, SKIPPED
    generated_invoice_id BIGINT REFERENCES invoices(id) ON DELETE SET NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sub_billing_period UNIQUE (subscription_id, billing_period_start, billing_period_end)
);

CREATE INDEX idx_billing_records_sub ON billing_records(subscription_id);

-- 11. Customer Notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL, -- INVOICE_GENERATED, PAYMENT_RECEIVED, RENEWAL_DUE, SUBSCRIPTION_PAUSED, SUBSCRIPTION_CLOSED
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_customer ON notifications(customer_id);

-- 12. Audit Logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
