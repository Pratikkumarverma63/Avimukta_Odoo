-- ============================================================================
-- BRIGHTLOOP SOFTWARE - SUBSCRIPTION MANAGEMENT PLATFORM
-- PRODUCTION MYSQL 8.0+ RELATIONAL DATABASE SCHEMA & COMPREHENSIVE SEED DATA
-- ============================================================================
-- Charset: utf8mb4 | Collation: utf8mb4_unicode_ci | Storage Engine: InnoDB
-- Architecture: 25 Normalized Tables (3NF/BCNF), Foreign Key Constraints,
-- Composite Performance Indexes, Day-Level Proration Ledger, 4-Tier Smart Dunning
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `brightloop_db` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `brightloop_db`;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 1. USERS & ROLES
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `first_name` VARCHAR(80) NOT NULL,
  `last_name` VARCHAR(80) NOT NULL,
  `role` VARCHAR(30) NOT NULL,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_users_role` (`role`),
  INDEX `idx_users_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. CUSTOMERS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT UNIQUE,
  `customer_code` VARCHAR(50) NOT NULL UNIQUE,
  `company_name` VARCHAR(150) NOT NULL,
  `contact_name` VARCHAR(120) NOT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(40),
  `billing_address` TEXT,
  `shipping_address` TEXT,
  `tax_id` VARCHAR(50),
  `currency` VARCHAR(10) NOT NULL DEFAULT 'INR',
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_customers_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  INDEX `idx_customers_code` (`customer_code`),
  INDEX `idx_customers_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. PASSWORD RESET TOKENS (Single-use, 15-minute expiration)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `password_reset_tokens`;
CREATE TABLE `password_reset_tokens` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `token` VARCHAR(100) NOT NULL UNIQUE,
  `user_id` BIGINT NOT NULL,
  `expiry_date` TIMESTAMP NOT NULL,
  `used` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_prt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_prt_lookup` (`token`, `used`, `expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. TAX RULES
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `tax_rules`;
CREATE TABLE `tax_rules` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `rate_percent` DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  `fixed_amount` DECIMAL(14,2) DEFAULT NULL,
  `is_percentage` BOOLEAN NOT NULL DEFAULT TRUE,
  `applicability` VARCHAR(50) NOT NULL DEFAULT 'ALL',
  `effective_start_date` DATE NOT NULL,
  `effective_end_date` DATE NOT NULL,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_tax_effective` (`active`, `effective_start_date`, `effective_end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. PRODUCTS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `name` VARCHAR(150) NOT NULL,
  `product_type` VARCHAR(50) NOT NULL DEFAULT 'SOFTWARE',
  `sales_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `cost_price` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `description` TEXT,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_products_type` (`product_type`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. PRODUCT VARIANTS (e.g. Odoo Edition +₹560 configurable)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `product_variants`;
CREATE TABLE `product_variants` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `attribute_name` VARCHAR(80) NOT NULL,
  `variant_value` VARCHAR(80) NOT NULL,
  `price_adjustment` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `sku` VARCHAR(80) UNIQUE,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_pv_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  INDEX `idx_pv_lookup` (`product_id`, `attribute_name`, `variant_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. SUBSCRIPTION PLANS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `plans`;
CREATE TABLE `plans` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `price` DECIMAL(14,2) NOT NULL,
  `billing_frequency` VARCHAR(30) NOT NULL,
  `min_quantity` INT NOT NULL DEFAULT 1,
  `availability_start_date` DATE NOT NULL,
  `availability_end_date` DATE NOT NULL,
  `auto_close_on_expiry` BOOLEAN NOT NULL DEFAULT TRUE,
  `allow_pause` BOOLEAN NOT NULL DEFAULT TRUE,
  `allow_early_closure` BOOLEAN NOT NULL DEFAULT TRUE,
  `allow_renewal` BOOLEAN NOT NULL DEFAULT TRUE,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_plans_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT,
  INDEX `idx_plans_freq` (`billing_frequency`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. DISCOUNT RULES
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `discount_rules`;
CREATE TABLE `discount_rules` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `name` VARCHAR(120) NOT NULL,
  `discount_type` VARCHAR(30) NOT NULL,
  `discount_value` DECIMAL(14,2) NOT NULL,
  `min_purchase_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `min_quantity` INT NOT NULL DEFAULT 1,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `max_uses` INT DEFAULT NULL,
  `used_count` INT NOT NULL DEFAULT 0,
  `scope` VARCHAR(30) NOT NULL DEFAULT 'SUBSCRIPTION_LEVEL',
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_discount_active` (`code`, `active`, `start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `discount_rule_products`;
CREATE TABLE `discount_rule_products` (
  `discount_rule_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  PRIMARY KEY (`discount_rule_id`, `product_id`),
  CONSTRAINT `fk_drp_rule` FOREIGN KEY (`discount_rule_id`) REFERENCES `discount_rules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_drp_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `discount_usages`;
CREATE TABLE `discount_usages` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `discount_rule_id` BIGINT NOT NULL,
  `customer_id` BIGINT NOT NULL,
  `invoice_id` BIGINT,
  `applied_amount` DECIMAL(14,2) NOT NULL,
  `used_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_du_rule` FOREIGN KEY (`discount_rule_id`) REFERENCES `discount_rules` (`id`),
  CONSTRAINT `fk_du_cust` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  INDEX `idx_du_cust` (`customer_id`, `discount_rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. QUOTATION TEMPLATES
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `quotation_templates`;
CREATE TABLE `quotation_templates` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(150) NOT NULL,
  `default_plan_id` BIGINT,
  `validity_days` INT NOT NULL DEFAULT 30,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_qt_plan` FOREIGN KEY (`default_plan_id`) REFERENCES `plans` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `quotation_template_items`;
CREATE TABLE `quotation_template_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `variant_id` BIGINT,
  `default_quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(14,2) NOT NULL,
  CONSTRAINT `fk_qti_template` FOREIGN KEY (`template_id`) REFERENCES `quotation_templates` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_qti_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_qti_variant` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. QUOTATIONS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `quotations`;
CREATE TABLE `quotations` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `quotation_number` VARCHAR(50) NOT NULL UNIQUE,
  `customer_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  `issue_date` DATE NOT NULL,
  `expiry_date` DATE NOT NULL,
  `subtotal` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `tax_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `total_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `notes` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_quot_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_quot_plan` FOREIGN KEY (`plan_id`) REFERENCES `plans` (`id`) ON DELETE RESTRICT,
  INDEX `idx_quot_status` (`status`, `expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `quotation_items`;
CREATE TABLE `quotation_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `quotation_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `variant_id` BIGINT,
  `description` VARCHAR(255),
  `quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(14,2) NOT NULL,
  `tax_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `line_total` DECIMAL(14,2) NOT NULL,
  CONSTRAINT `fk_qi_quot` FOREIGN KEY (`quotation_id`) REFERENCES `quotations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_qi_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_qi_variant` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 11. SUBSCRIPTIONS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `subscriptions`;
CREATE TABLE `subscriptions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `subscription_number` VARCHAR(50) NOT NULL UNIQUE,
  `customer_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  `start_date` DATE NOT NULL,
  `expiry_date` DATE,
  `next_billing_date` DATE NOT NULL,
  `last_billed_date` DATE,
  `payment_terms` VARCHAR(50) NOT NULL DEFAULT 'NET_30',
  `subtotal` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `tax_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `total_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `version` BIGINT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_sub_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_sub_plan` FOREIGN KEY (`plan_id`) REFERENCES `plans` (`id`) ON DELETE RESTRICT,
  INDEX `idx_sub_billing` (`status`, `next_billing_date`),
  INDEX `idx_sub_cust` (`customer_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `subscription_items`;
CREATE TABLE `subscription_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `subscription_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `variant_id` BIGINT,
  `description` VARCHAR(255),
  `quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(14,2) NOT NULL,
  `tax_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `line_total` DECIMAL(14,2) NOT NULL,
  CONSTRAINT `fk_si_sub` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_si_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_si_variant` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 12. INVOICES
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `invoices`;
CREATE TABLE `invoices` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `invoice_number` VARCHAR(50) NOT NULL UNIQUE,
  `customer_id` BIGINT NOT NULL,
  `subscription_id` BIGINT,
  `invoice_date` DATE NOT NULL,
  `due_date` DATE NOT NULL,
  `billing_period_start` DATE,
  `billing_period_end` DATE,
  `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  `subtotal` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `tax_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `total_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `amount_paid` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `outstanding_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `sent_to_customer` BOOLEAN NOT NULL DEFAULT FALSE,
  `notes` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_inv_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_inv_sub` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`id`) ON DELETE SET NULL,
  INDEX `idx_inv_status_due` (`status`, `due_date`),
  INDEX `idx_inv_cust` (`customer_id`, `status`),
  INDEX `idx_inv_period` (`subscription_id`, `billing_period_start`, `billing_period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `invoice_items`;
CREATE TABLE `invoice_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `invoice_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `variant_id` BIGINT,
  `description` VARCHAR(255),
  `quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(14,2) NOT NULL,
  `tax_rule_id` BIGINT,
  `tax_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_rule_id` BIGINT,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `line_total` DECIMAL(14,2) NOT NULL,
  CONSTRAINT `fk_ii_inv` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ii_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_ii_variant` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `fk_ii_tax` FOREIGN KEY (`tax_rule_id`) REFERENCES `tax_rules` (`id`),
  CONSTRAINT `fk_ii_disc` FOREIGN KEY (`discount_rule_id`) REFERENCES `discount_rules` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 13. PAYMENTS & IMMUTABLE FINANCIAL LEDGER
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `payments`;
CREATE TABLE `payments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `payment_number` VARCHAR(50) NOT NULL UNIQUE,
  `invoice_id` BIGINT NOT NULL,
  `amount` DECIMAL(14,2) NOT NULL,
  `payment_method` VARCHAR(40) NOT NULL,
  `transaction_reference` VARCHAR(150),
  `payment_date` TIMESTAMP NOT NULL,
  `status` VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
  `notes` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_pay_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE RESTRICT,
  INDEX `idx_pay_inv` (`invoice_id`),
  INDEX `idx_pay_date` (`payment_date`),
  INDEX `idx_pay_method` (`payment_method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `payment_reversals`;
CREATE TABLE `payment_reversals` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `original_payment_id` BIGINT NOT NULL,
  `reversal_number` VARCHAR(50) NOT NULL UNIQUE,
  `amount` DECIMAL(14,2) NOT NULL,
  `reason` TEXT NOT NULL,
  `reversed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_pr_payment` FOREIGN KEY (`original_payment_id`) REFERENCES `payments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 14. BILLING EXECUTION AUDIT LOGS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `billing_records`;
CREATE TABLE `billing_records` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `job_execution_id` VARCHAR(100) NOT NULL,
  `run_date` DATE NOT NULL,
  `subscriptions_scanned` INT NOT NULL DEFAULT 0,
  `invoices_generated` INT NOT NULL DEFAULT 0,
  `total_amount_billed` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `status` VARCHAR(30) NOT NULL,
  `execution_log` TEXT,
  `started_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` TIMESTAMP,
  INDEX `idx_br_date` (`run_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 15. NOTIFICATIONS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `customer_id` BIGINT NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `message` TEXT NOT NULL,
  `notification_type` VARCHAR(50) NOT NULL,
  `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_notif_cust` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE,
  INDEX `idx_notif_cust` (`customer_id`, `is_read`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 16. AUDIT LOGS
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `entity_name` VARCHAR(80) NOT NULL,
  `entity_id` BIGINT NOT NULL,
  `action` VARCHAR(40) NOT NULL,
  `changed_by` VARCHAR(120) NOT NULL,
  `details` TEXT,
  `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_audit_entity` (`entity_name`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 17. ADVANCED UNIQUE FEATURE 1: PRORATION ENGINE LEDGER
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `proration_records`;
CREATE TABLE `proration_records` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `subscription_id` BIGINT NOT NULL,
  `customer_id` BIGINT NOT NULL,
  `change_type` VARCHAR(50) NOT NULL,
  `effective_date` DATE NOT NULL,
  `old_plan_price` DECIMAL(14,2) NOT NULL,
  `new_plan_price` DECIMAL(14,2) NOT NULL,
  `days_used` INT NOT NULL,
  `days_in_cycle` INT NOT NULL,
  `prorated_credit_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `prorated_charge_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `net_adjustment_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_proration_sub` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_proration_cust` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE,
  INDEX `idx_proration_sub` (`subscription_id`),
  INDEX `idx_proration_cust` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 18. ADVANCED UNIQUE FEATURE 2: 4-TIER SMART DUNNING & REVENUE RECOVERY
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `dunning_cases`;
CREATE TABLE `dunning_cases` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `invoice_id` BIGINT NOT NULL,
  `customer_id` BIGINT NOT NULL,
  `dunning_stage` VARCHAR(50) NOT NULL DEFAULT 'HEALTHY',
  `days_overdue` INT NOT NULL DEFAULT 0,
  `late_fee_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `last_action_date` TIMESTAMP NULL DEFAULT NULL,
  `next_action_date` TIMESTAMP NULL DEFAULT NULL,
  `attempts_count` INT NOT NULL DEFAULT 0,
  `notes` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_dunning_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dunning_cust` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE,
  INDEX `idx_dunning_invoice` (`invoice_id`),
  INDEX `idx_dunning_stage` (`dunning_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- COMPREHENSIVE SEED DATA
-- ============================================================================

-- 1. Users (BCrypt Encrypted Passwords)
-- Admin: Admin@12345
-- Staff: Staff@12345
-- Customer: Customer@12345 / Corporate@12345
INSERT INTO `users` (`id`, `email`, `password`, `first_name`, `last_name`, `role`, `active`) VALUES
(1, 'admin@brightloop.com', '$2a$10$wT5H5dKqL4hW71B0v2m2vubfFqf1m9iYJp1qY9Wb.g/EwP2.H9z0G', 'Super', 'Admin', 'ROLE_ADMIN', TRUE),
(2, 'staff@brightloop.com', '$2a$10$wT5H5dKqL4hW71B0v2m2vubfFqf1m9iYJp1qY9Wb.g/EwP2.H9z0G', 'Sarah', 'Sales', 'ROLE_STAFF', TRUE),
(3, 'customer@brightloop.com', '$2a$10$wT5H5dKqL4hW71B0v2m2vubfFqf1m9iYJp1qY9Wb.g/EwP2.H9z0G', 'Raj', 'Patel', 'ROLE_CUSTOMER', TRUE),
(4, 'tata.billing@tcs-cloud.com', '$2a$10$wT5H5dKqL4hW71B0v2m2vubfFqf1m9iYJp1qY9Wb.g/EwP2.H9z0G', 'Priya', 'Sharma', 'ROLE_CUSTOMER', TRUE),
(5, 'finance@infosys-labs.in', '$2a$10$wT5H5dKqL4hW71B0v2m2vubfFqf1m9iYJp1qY9Wb.g/EwP2.H9z0G', 'Amit', 'Verma', 'ROLE_CUSTOMER', TRUE),
(6, 'procurement@relianceretail.com', '$2a$10$wT5H5dKqL4hW71B0v2m2vubfFqf1m9iYJp1qY9Wb.g/EwP2.H9z0G', 'Vikram', 'Singh', 'ROLE_CUSTOMER', TRUE),
(7, 'accounts@zomatotech.com', '$2a$10$wT5H5dKqL4hW71B0v2m2vubfFqf1m9iYJp1qY9Wb.g/EwP2.H9z0G', 'Sneha', 'Rao', 'ROLE_CUSTOMER', TRUE);

-- 2. Enterprise Customers
INSERT INTO `customers` (`id`, `user_id`, `customer_code`, `company_name`, `contact_name`, `email`, `phone`, `billing_address`, `tax_id`) VALUES
(1, 3, 'CUST-10001', 'Acme Retail Solutions', 'Raj Patel', 'customer@brightloop.com', '+91-9876543210', '42 Commercial Street, Bangalore, Karnataka 560001', 'GSTIN29AABCU9603R1Z7'),
(2, 4, 'CUST-10002', 'Tata Consultancy Global', 'Priya Sharma', 'tata.billing@tcs-cloud.com', '+91-9820012345', 'Bandra Kurla Complex, Mumbai, Maharashtra 400051', 'GSTIN27AAACT2727Q1ZW'),
(3, 5, 'CUST-10003', 'Infosys FinTech Labs', 'Amit Verma', 'finance@infosys-labs.in', '+91-9880198765', 'Electronics City, Bengaluru, Karnataka 560100', 'GSTIN29AAACI4321P1ZQ'),
(4, 6, 'CUST-10004', 'Reliance Omnichannel Retail', 'Vikram Singh', 'procurement@relianceretail.com', '+91-9811234567', 'Ghansoli, Navi Mumbai, Maharashtra 400701', 'GSTIN27AAACR1234M1ZV'),
(5, 7, 'CUST-10005', 'Zomato Hyperlocal Technologies', 'Sneha Rao', 'accounts@zomatotech.com', '+91-9810987654', 'DLF Phase 5, Gurugram, Haryana 122002', 'GSTIN06AAACZ5678K1ZT');

-- 3. Tax Rules
INSERT INTO `tax_rules` (`id`, `name`, `code`, `rate_percent`, `is_percentage`, `applicability`, `effective_start_date`, `effective_end_date`, `active`) VALUES
(1, 'Goods & Services Tax 18%', 'GST_18', 18.0000, TRUE, 'ALL', '2024-01-01', '2030-12-31', TRUE),
(2, 'Value Added Tax 5%', 'VAT_5', 5.0000, TRUE, 'ALL', '2024-01-01', '2030-12-31', TRUE),
(3, 'Zero Tax / Exempt', 'EXEMPT_0', 0.0000, TRUE, 'ALL', '2024-01-01', '2030-12-31', TRUE);

-- 4. Products
INSERT INTO `products` (`id`, `code`, `name`, `product_type`, `sales_price`, `cost_price`, `description`, `active`) VALUES
(1, 'PROD-ERP', 'Brightloop Business ERP', 'SOFTWARE', 2000.00, 800.00, 'Comprehensive Cloud ERP for modern enterprises with sales, accounting, and inventory.', TRUE),
(2, 'PROD-ANALYTICS', 'Advanced Analytics Add-on', 'ADD_ON', 500.00, 150.00, 'Real-time financial business intelligence and executive reporting dashboards.', TRUE),
(3, 'PROD-SUPPORT', '24/7 Dedicated SLA Support', 'SERVICE', 1200.00, 400.00, 'Round-the-clock priority technical support with 1-hour response SLA.', TRUE);

-- 5. Product Variants (Configurable Odoo variant +₹560 as requested in problem statement)
INSERT INTO `product_variants` (`id`, `product_id`, `attribute_name`, `variant_value`, `price_adjustment`, `sku`, `active`) VALUES
(1, 1, 'Brand', 'Standard', 0.00, 'ERP-STD', TRUE),
(2, 1, 'Brand', 'Odoo', 560.00, 'ERP-ODOO', TRUE),
(3, 1, 'Edition', 'Enterprise Multi-Company', 1500.00, 'ERP-ENT', TRUE),
(4, 2, 'Tier', 'Basic Analytics', 0.00, 'ADDON-BASIC', TRUE),
(5, 2, 'Tier', 'AI Predictive Analytics', 400.00, 'ADDON-PRED', TRUE);

-- 6. Subscription Plans
INSERT INTO `plans` (`id`, `product_id`, `name`, `code`, `price`, `billing_frequency`, `min_quantity`, `availability_start_date`, `availability_end_date`, `auto_close_on_expiry`, `allow_pause`, `allow_early_closure`, `allow_renewal`, `active`) VALUES
(1, 1, 'ERP Monthly Flexible', 'PLAN-ERP-M', 2000.00, 'MONTHLY', 1, '2024-01-01', '2030-12-31', TRUE, TRUE, TRUE, TRUE, TRUE),
(2, 1, 'ERP Annual Growth', 'PLAN-ERP-Y', 21600.00, 'YEARLY', 1, '2024-01-01', '2030-12-31', FALSE, TRUE, TRUE, TRUE, TRUE),
(3, 1, 'ERP Weekly Agile', 'PLAN-ERP-W', 550.00, 'WEEKLY', 1, '2024-01-01', '2030-12-31', TRUE, FALSE, TRUE, TRUE, TRUE),
(4, 1, 'ERP Daily Fast Track', 'PLAN-ERP-D', 100.00, 'DAILY', 1, '2024-01-01', '2030-12-31', TRUE, FALSE, TRUE, FALSE, TRUE);

-- 7. Discount Rules
INSERT INTO `discount_rules` (`id`, `code`, `name`, `discount_type`, `discount_value`, `min_purchase_amount`, `min_quantity`, `start_date`, `end_date`, `max_uses`, `used_count`, `scope`, `active`) VALUES
(1, 'BULK20', 'Volume Customer Discount 20%', 'PERCENTAGE', 20.00, 10000.00, 5, '2024-01-01', '2030-12-31', 500, 0, 'SUBSCRIPTION_LEVEL', TRUE),
(2, 'FLAT500', 'New Subscriber Welcome Voucher', 'FIXED_AMOUNT', 500.00, 2000.00, 1, '2024-01-01', '2030-12-31', 200, 0, 'SUBSCRIPTION_LEVEL', TRUE),
(3, 'ANALYTICS10', 'Analytics Add-on Promo 10%', 'PERCENTAGE', 10.00, 0.00, 1, '2024-01-01', '2030-12-31', 1000, 0, 'PRODUCT_SPECIFIC', TRUE);

-- 8. Subscriptions (Multi-Cadence: Monthly, Yearly, Weekly, Paused, Closed)
INSERT INTO `subscriptions` (`id`, `subscription_number`, `customer_id`, `plan_id`, `status`, `start_date`, `expiry_date`, `next_billing_date`, `last_billed_date`, `payment_terms`, `subtotal`, `tax_amount`, `discount_amount`, `total_amount`) VALUES
(1, 'SUB-2026-10001', 1, 1, 'ACTIVE', DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), NULL, DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), 'NET_30', 2560.00, 460.80, 0.00, 3020.80),
(2, 'SUB-2026-10002', 1, 1, 'ACTIVE', DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), NULL, DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'NET_30', 900.00, 162.00, 0.00, 1062.00),
(3, 'SUB-2026-20001', 2, 2, 'ACTIVE', DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 275 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 275 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), 'NET_45', 23100.00, 4158.00, 0.00, 27258.00),
(4, 'SUB-2026-30001', 3, 1, 'PAUSED', DATE_SUB(CURRENT_DATE, INTERVAL 120 DAY), NULL, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), 'NET_30', 2000.00, 360.00, 0.00, 2360.00),
(5, 'SUB-2026-40001', 4, 3, 'ACTIVE', DATE_SUB(CURRENT_DATE, INTERVAL 21 DAY), NULL, DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), 'NET_15', 550.00, 99.00, 0.00, 649.00),
(6, 'SUB-2026-50001', 5, 1, 'CLOSED', DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 40 DAY), 'NET_30', 2000.00, 360.00, 0.00, 2360.00);

-- Subscription Line Items
INSERT INTO `subscription_items` (`id`, `subscription_id`, `product_id`, `variant_id`, `description`, `quantity`, `unit_price`, `tax_amount`, `discount_amount`, `line_total`) VALUES
(1, 1, 1, 2, 'Brightloop Business ERP (Odoo Edition)', 1, 2560.00, 460.80, 0.00, 3020.80),
(2, 2, 2, 5, 'Advanced Analytics Add-on (AI Predictive Tier)', 1, 900.00, 162.00, 0.00, 1062.00),
(3, 3, 1, 3, 'ERP Annual Contract (Enterprise Multi-Company)', 1, 23100.00, 4158.00, 0.00, 27258.00),
(4, 4, 1, 1, 'Brightloop Business ERP (Standard)', 1, 2000.00, 360.00, 0.00, 2360.00),
(5, 5, 1, 1, 'ERP Weekly Agile Instance', 1, 550.00, 99.00, 0.00, 649.00),
(6, 6, 1, 1, 'Brightloop Business ERP (Closed Early)', 1, 2000.00, 360.00, 0.00, 2360.00);

-- 9. Invoices
INSERT INTO `invoices` (`id`, `invoice_number`, `customer_id`, `subscription_id`, `invoice_date`, `due_date`, `billing_period_start`, `billing_period_end`, `status`, `subtotal`, `tax_amount`, `discount_amount`, `total_amount`, `amount_paid`, `outstanding_amount`, `sent_to_customer`) VALUES
(1, 'INV-2026-00101', 1, 1, DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), 'PAID', 2560.00, 460.80, 0.00, 3020.80, 3020.80, 0.00, TRUE),
(2, 'INV-2026-00102', 1, 1, DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), CURRENT_DATE, DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), CURRENT_DATE, 'PAID', 2560.00, 460.80, 0.00, 3020.80, 3020.80, 0.00, TRUE),
(3, 'INV-2026-00103', 1, 2, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 25 DAY), 'CONFIRMED', 900.00, 162.00, 0.00, 1062.00, 0.00, 1062.00, TRUE),
(4, 'INV-2026-00201', 2, 3, DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 275 DAY), 'PAID', 23100.00, 4158.00, 0.00, 27258.00, 27258.00, 0.00, TRUE),
(5, 'INV-2026-00202', 2, 3, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 'CONFIRMED', 1200.00, 216.00, 0.00, 1416.00, 500.00, 916.00, TRUE),
(6, 'INV-2026-00301', 3, 4, DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), 'CONFIRMED', 2000.00, 360.00, 0.00, 2360.00, 0.00, 2360.00, TRUE),
(7, 'INV-2026-00401', 4, 5, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), 'PAID', 550.00, 99.00, 0.00, 649.00, 649.00, 0.00, TRUE);

-- Invoice Line Items
INSERT INTO `invoice_items` (`id`, `invoice_id`, `product_id`, `variant_id`, `description`, `quantity`, `unit_price`, `tax_amount`, `discount_amount`, `line_total`) VALUES
(1, 1, 1, 2, 'Brightloop Business ERP (Odoo Edition) - Cycle 1', 1, 2560.00, 460.80, 0.00, 3020.80),
(2, 2, 1, 2, 'Brightloop Business ERP (Odoo Edition) - Cycle 2', 1, 2560.00, 460.80, 0.00, 3020.80),
(3, 3, 2, 5, 'Advanced Analytics Add-on (AI Predictive Tier)', 1, 900.00, 162.00, 0.00, 1062.00),
(4, 4, 1, 3, 'ERP Annual Contract - Enterprise Multi-Company Edition', 1, 23100.00, 4158.00, 0.00, 27258.00),
(5, 5, 3, NULL, '24/7 Priority SLA Dedicated Support Extension', 1, 1200.00, 216.00, 0.00, 1416.00),
(6, 6, 1, 1, 'Brightloop Business ERP - Standard Monthly Instance (Overdue)', 1, 2000.00, 360.00, 0.00, 2360.00),
(7, 7, 1, 1, 'ERP Weekly Agile Sprint Instance', 1, 550.00, 99.00, 0.00, 649.00);

-- 10. Reconciled Payments
INSERT INTO `payments` (`id`, `payment_number`, `invoice_id`, `amount`, `payment_method`, `transaction_reference`, `payment_date`, `status`, `notes`) VALUES
(1, 'PAY-2026-00101', 1, 3020.80, 'UPI', 'UPI/430198273612', DATE_SUB(NOW(), INTERVAL 55 DAY), 'COMPLETED', 'Instant UPI settlement via PhonePe Business'),
(2, 'PAY-2026-00102', 2, 3020.80, 'BANK_TRANSFER', 'NEFT/HDFC998231456', DATE_SUB(NOW(), INTERVAL 23 DAY), 'COMPLETED', 'Corporate NEFT bank settlement verified'),
(3, 'PAY-2026-00201', 4, 27258.00, 'BANK_TRANSFER', 'RTGS/SBIN002938127', DATE_SUB(NOW(), INTERVAL 78 DAY), 'COMPLETED', 'Tata corporate treasury RTGS remittance'),
(4, 'PAY-2026-00202', 5, 500.00, 'CARD', 'CARD/VISA-CORP-4242', DATE_SUB(NOW(), INTERVAL 5 DAY), 'COMPLETED', 'Part payment recorded via Corporate Visa Card'),
(5, 'PAY-2026-00401', 7, 649.00, 'UPI', 'UPI/RAZOR88723461', DATE_SUB(NOW(), INTERVAL 8 DAY), 'COMPLETED', 'Auto-debit UPI recurring mandate');

-- 11. Proration Record (Mid-Cycle Early Termination & Day-Level Credit Calculation)
INSERT INTO `proration_records` (`id`, `subscription_id`, `customer_id`, `change_type`, `effective_date`, `old_plan_price`, `new_plan_price`, `days_used`, `days_in_cycle`, `prorated_credit_amount`, `prorated_charge_amount`, `net_adjustment_amount`) VALUES
(1, 6, 5, 'EARLY_TERMINATION', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 2360.00, 0.00, 16, 30, 1101.33, 0.00, -1101.33);

-- 12. Smart Dunning Case (Automated Overdue Recovery with 2% Compound Late Fee)
INSERT INTO `dunning_cases` (`id`, `invoice_id`, `customer_id`, `dunning_stage`, `days_overdue`, `late_fee_amount`, `last_action_date`, `next_action_date`, `attempts_count`, `notes`) VALUES
(1, 6, 3, 'LATE_FEE_APPLIED', 15, 47.20, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 3, 'Automated Dunning: 2 reminders dispatched. 2% compound late fee applied.');

-- 13. Quotations
INSERT INTO `quotations` (`id`, `quotation_number`, `customer_id`, `plan_id`, `status`, `issue_date`, `expiry_date`, `subtotal`, `tax_amount`, `discount_amount`, `total_amount`, `notes`) VALUES
(1, 'QUO-2026-0001', 1, 2, 'QUOTATION', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 20 DAY), 21600.00, 3888.00, 0.00, 25488.00, 'Annual upgrade proposal for Acme Retail Solutions'),
(2, 'QUO-2026-0002', 3, 1, 'DRAFT', DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 28 DAY), 900.00, 162.00, 0.00, 1062.00, 'Draft expansion quote for Analytics add-on');

-- 14. Notifications
INSERT INTO `notifications` (`id`, `customer_id`, `title`, `message`, `notification_type`, `is_read`) VALUES
(1, 1, 'Invoice INV-2026-00103 Generated', 'Your monthly billing invoice of ₹1,062.00 for AI Analytics Add-on is ready.', 'INVOICE_GENERATED', FALSE),
(2, 1, 'Payment Receipt Confirmed', 'Receipt PAY-2026-00102 of ₹3,020.80 successfully reconciled via Corporate NEFT.', 'PAYMENT_RECEIVED', FALSE),
(3, 1, 'Odoo Brand Variant Activated', 'Your subscription SUB-2026-10001 is active with configurable Odoo brand add-on (+₹560/mo).', 'RENEWAL_DUE', TRUE);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- SELECT COUNT(*) AS total_tables FROM information_schema.tables WHERE table_schema = 'brightloop_db';
-- Expected: 25 tables
-- ============================================================================
