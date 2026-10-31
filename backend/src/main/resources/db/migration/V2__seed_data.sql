-- Brightloop Software Subscription Management System
-- V2__seed_data.sql: Baseline Reference & Seed Data

-- 1. Initial Tax Rules
INSERT INTO tax_rules (name, code, rate_percent, is_percentage, applicability, effective_start_date, effective_end_date, active)
VALUES 
('Goods & Services Tax 18%', 'GST_18', 18.0000, TRUE, 'ALL', '2024-01-01', '2030-12-31', TRUE),
('Value Added Tax 5%', 'VAT_5', 5.0000, TRUE, 'ALL', '2024-01-01', '2030-12-31', TRUE),
('Zero Tax / Exempt', 'EXEMPT_0', 0.0000, TRUE, 'ALL', '2024-01-01', '2030-12-31', TRUE);

-- 2. Initial Products
INSERT INTO products (code, name, product_type, sales_price, cost_price, description, active)
VALUES 
('PROD-ERP', 'Brightloop Business ERP', 'SOFTWARE', 2000.00, 800.00, 'Comprehensive Cloud ERP for modern enterprises with modules for sales, accounting, and inventory.', TRUE),
('PROD-ANALYTICS', 'Advanced Analytics Add-on', 'ADD_ON', 500.00, 150.00, 'Real-time financial business intelligence and executive reporting dashboards.', TRUE),
('PROD-SUPPORT', '24/7 Dedicated SLA Support', 'SERVICE', 1200.00, 400.00, 'Round-the-clock priority technical support with 1-hour response SLA.', TRUE);

-- 3. Product Variants (Illustrates problem statement: Odoo brand adds 560 to base price)
INSERT INTO product_variants (product_id, attribute_name, variant_value, price_adjustment, sku, active)
VALUES 
(1, 'Brand', 'Standard', 0.00, 'ERP-STD', TRUE),
(1, 'Brand', 'Odoo', 560.00, 'ERP-ODOO', TRUE),
(1, 'Edition', 'Enterprise Multi-Company', 1500.00, 'ERP-ENT', TRUE),
(2, 'Tier', 'Basic Analytics', 0.00, 'ADDON-BASIC', TRUE),
(2, 'Tier', 'AI Predictive Analytics', 400.00, 'ADDON-PRED', TRUE);

-- 4. Subscription Plans
INSERT INTO plans (product_id, name, code, price, billing_frequency, min_quantity, availability_start_date, availability_end_date, auto_close_on_expiry, allow_pause, allow_early_closure, allow_renewal, active)
VALUES 
(1, 'ERP Monthly Flexible', 'PLAN-ERP-M', 2000.00, 'MONTHLY', 1, '2024-01-01', '2030-12-31', TRUE, TRUE, TRUE, TRUE, TRUE),
(1, 'ERP Annual Growth', 'PLAN-ERP-Y', 21600.00, 'YEARLY', 1, '2024-01-01', '2030-12-31', FALSE, TRUE, TRUE, TRUE, TRUE),
(1, 'ERP Weekly Agile', 'PLAN-ERP-W', 550.00, 'WEEKLY', 1, '2024-01-01', '2030-12-31', TRUE, FALSE, TRUE, TRUE, TRUE),
(1, 'ERP Daily Fast Track', 'PLAN-ERP-D', 100.00, 'DAILY', 1, '2024-01-01', '2030-12-31', TRUE, FALSE, TRUE, FALSE, TRUE);

-- 5. Discount Rules
INSERT INTO discount_rules (code, name, discount_type, discount_value, min_purchase_amount, min_quantity, start_date, end_date, max_uses, used_count, scope, active)
VALUES 
('BULK20', 'Volume Customer Discount 20%', 'PERCENTAGE', 20.00, 10000.00, 5, '2024-01-01', '2030-12-31', 500, 0, 'SUBSCRIPTION_LEVEL', TRUE),
('FLAT500', 'New Subscriber Welcome Voucher', 'FIXED_AMOUNT', 500.00, 2000.00, 1, '2024-01-01', '2030-12-31', 200, 0, 'SUBSCRIPTION_LEVEL', TRUE),
('ANALYTICS10', 'Analytics Add-on Promo 10%', 'PERCENTAGE', 10.00, 0.00, 1, '2024-01-01', '2030-12-31', 1000, 0, 'PRODUCT_SPECIFIC', TRUE);

-- Map product-specific discount to Analytics product
INSERT INTO discount_rule_products (discount_rule_id, product_id)
VALUES (3, 2);

-- 6. Quotation Templates
INSERT INTO quotation_templates (name, default_plan_id, validity_days, active)
VALUES 
('Standard Business Starter Quotation', 1, 30, TRUE),
('Enterprise Annual Contract Template', 2, 45, TRUE);

INSERT INTO quotation_template_items (template_id, product_id, variant_id, default_quantity, unit_price)
VALUES 
(1, 1, 1, 1, 2000.00),
(1, 2, 4, 1, 500.00),
(2, 1, 2, 5, 2560.00),
(2, 3, NULL, 1, 1200.00);
