# Brightloop Software — Enterprise Subscription Management Platform

[![Java 25](https://img.shields.io/badge/Java-25%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.5.16](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3.4-38bdf8.svg)](https://tailwindcss.com/)
[![MySQL 8.0 & PostgreSQL 16](https://img.shields.io/badge/RDBMS-MySQL%208.0%20%7C%20PostgreSQL%2016-336791.svg)](database/mysql_schema_and_seed.sql)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A production-grade, secure, and highly scalable Subscription Business Management Platform designed and engineered for **Brightloop Software**. This system covers every requirement from the hackathon problem statement and includes **game-changing architectural features** that set it far apart from boilerplate CRUD applications:

1. **Mid-Cycle Proration Engine (`proration_records`)**: Mathematical day-level pro-rata calculation and immutable credit adjustments when switching or pausing plans mid-cycle.
2. **Automated 4-Tier Smart Dunning & Revenue Recovery Engine (`dunning_cases`)**: Automatic escalation (`REMINDER_SENT` → `GRACE_PERIOD` → `LATE_FEE_APPLIED` → `SUSPENDED`) with 2% compound late fee calculation.
3. **Dual-Database Support (MySQL 8.0+ & PostgreSQL 16)**: 25 normalized tables (3NF/BCNF), complete ready-to-run MySQL DDL dump with seed data (`database/mysql_schema_and_seed.sql`), and in-app interactive **Database & ERD Explorer**.
4. **Custom Cyber-Loop Vector SVG Brand Identity**: Sleek visual identity replacing generic logos across the navbar, auth, portal, and printable tax invoices.
5. **Rich Enterprise Dataset Pre-Seeded**: 5 corporate clients (Acme Retail, Tata Consultancy, Infosys, Reliance, Zomato), 6 diverse subscriptions, 7 invoices, 5 reconciled multi-channel payments, and active proration/dunning records.

---

## Architecture Overview

```
+--------------------------------------------------------------------------------------------------+
|                                    BRIGHTLOOP FRONTEND (SPA)                                     |
|               React 18 + TypeScript + Vite + Tailwind CSS + Lucide Icons + Axios Interceptors    |
|   [BrandLogo SVG]  [Finance Dashboard]  [Proration Simulator]  [Dunning Center]  [ERD Explorer]   |
+--------------------------------------------------------------------------------------------------+
          | (JWT Bearer Token / Role-Guarded REST Endpoints)
          v
+--------------------------------------------------------------------------------------------------+
|                                  BRIGHTLOOP BACKEND API GATEWAY                                  |
|   Spring Boot 3.3.5 / Spring Security 6 / Stateless JWT Filters / Centralized Global Exception    |
+--------------------------------------------------------------------------------------------------+
     |                  |                   |                   |                  |
     v                  v                   v                   v                  v
+----------+    +---------------+    +--------------+    +---------------+    +-------------------+
|  Auth &  |    |   Products    |    |  Quotations  |    | Subscription  |    | Automated Billing |
|  Users   |    |  & Variants   |    | & Templates  |    | State Machine |    | Engine (Scheduler)|
+----------+    +---------------+    +--------------+    +---------------+    +-------------------+
     |                  |                   |                   |                  |
     v                  v                   v                   v                  v
+----------+    +---------------+    +--------------+    +---------------+    +-------------------+
| Invoices |    |    Payment    |    | Taxes (GST)  |    | * PRORATION * |    | * SMART DUNNING * |
| & HTML   |    | Ledger (UPI/  |    | & Discounts  |    | Mid-Cycle Day |    | 4-Tier Recovery   |
| Print    |    | Bank/Card)    |    | (Atomic Caps)|    | Pro-rata Calc |    | 2% Compound Fee   |
+----------+    +---------------+    +--------------+    +---------------+    +-------------------+
                                            |
                                            v (JPA / Hibernate 6 / NUMERIC(14,2) BigDecimal)
+--------------------------------------------------------------------------------------------------+
|                  ENTERPRISE RELATIONAL DATABASE (MySQL 8.0+ / PostgreSQL 16 / H2)                 |
|  25 Normalized Tables • Flyway Migrations • Composite Indexes • Unique Constraint Idempotency    |
|  Complete SQL Dump: database/mysql_schema_and_seed.sql (Instant 1-Click Database Setup)         |
+--------------------------------------------------------------------------------------------------+
```

---

## 1. Key Business Features & Implementation

### 1. User Roles & Authentication (RBAC)
- **Three Core Roles**:
  - `ROLE_ADMIN`: Complete system access, catalog & variant management, plan authoring, tax & discount rules, staff provisioning (`/api/v1/admin/staff`), billing triggers, and financial analytics.
  - `ROLE_STAFF`: Day-to-day operations, quotation generation from templates, subscription lifecycle management, invoice creation, and payment recording. Cannot create staff accounts.
  - `ROLE_CUSTOMER`: Strict self-service portal isolation. Accesses only their own subscriptions, invoices, and payment receipts.
- **Security & Password Policy**:
  - Hashed with BCrypt (strength 12).
  - Enforced complexity: `> 8 characters`, at least 1 uppercase letter, 1 lowercase letter, and 1 special character.
  - Expiring, single-use 15-minute password reset token dispatched by email.
  - Stateless JWT authentication with unique `jti` UUIDs and refresh token rotation.

### 2. Products & Configurable Variant Pricing
- Configurable base products (`SOFTWARE`, `SERVICE`, `ADD_ON`) with sales price and cost price.
- **Problem Statement Rule Implemented**: Base price = ₹2,000. Selecting the *Odoo Brand* variant adds ₹560 dynamically, calculating an effective price of ₹2,560 without hardcoding. Supports arbitrary variants (Editions, Tiers, Brands).

### 3. Subscription Plan Cadence & Leap-Year Arithmetic
- Supported Billing Frequencies: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`.
- Calendar-aware date arithmetic in `BillingDateCalculator.java`:
  - Month-end handling (Jan 31 + 1 month = Feb 28 or Feb 29 in leap years).
  - Leap-year Feb 29 + 1 year = Feb 28 in non-leap years.
- Granular permissions per plan: `autoCloseOnExpiry`, `allowPause`, `allowEarlyClosure`, `allowRenewal`.

### 4. Quotation Templates & Subscription Creation
- Reusable quotation templates with default plan, product lines, quantities, and validity duration.
- Enforced Quotation Lifecycle: `DRAFT` &rarr; `QUOTATION` &rarr; `CONFIRMED`.
- **One-Click Conversion**: Confirming a quotation automatically transforms it into an active subscription (`SUB-YYYY-XXXXX`), initializes billing items, sets the initial billing date, and starts recurring billing.

### 5. Subscription State Machine
- Lifecycle states: `DRAFT` &rarr; `QUOTATION` &rarr; `CONFIRMED` &rarr; `ACTIVE` &rarr; `PAUSED` &rarr; `CLOSED` &rarr; `RENEWED`.
- Transition validations:
  - Pause: sets status to `PAUSED` and records `pausedAt`.
  - Resume: returns status to `ACTIVE`.
  - Early Closure: sets status to `CLOSED` and stamps `closedAt`.
  - Renewal: increments subscription period, resets billing dates, and maintains audit trail.

### 6. Automated Recurring Billing Engine
- Identifies active subscriptions due for billing on or before the execution date.
- **Zero-Duplicate Guarantee**: Hard unique constraint on `billing_records (subscription_id, billing_period_start, billing_period_end)` prevents duplicate invoices even under concurrent scheduler triggers or race conditions.
- Advances `nextBillingDate` accurately according to plan cadence.
- Dispatches transactional customer notifications.
- Separates invoice generation from actual payment collection.

### 7. Centralized Taxes & Concurrency-Safe Discounts
- **Taxes**: Configurable tax rules (GST 18%, VAT 5%, Zero Tax) applied accurately using `BigDecimal` with `RoundingMode.HALF_UP`.
- **Discounts**: Fixed amount and percentage discounts.
- **Concurrency-Safe Usage Limit**: Uses atomic SQL updates (`UPDATE discount_rules SET used_count = used_count + 1 WHERE id = :id AND used_count < max_uses`) to guarantee usage limits are never exceeded under concurrent checkout requests.

### 8. Invoices & Formal Printable Documents
- Invoice Lifecycle: `DRAFT` &rarr; `CONFIRMED` &rarr; `PAID` / `CANCELLED`.
- Full breakdown: Subtotal, CGST/SGST/VAT, Discounts, Net Total, Paid Amount, and Outstanding Balance.
- In-browser printable/downloadable HTML layout formatted as a formal enterprise tax invoice.

### 9. Immutable Multi-Channel Payment Ledger
- Supported channels: `UPI`, `BANK_TRANSFER` (NEFT/RTGS), `CARD`, `CASH`, `OTHER`.
- Partial payment reconciliation: reduces invoice outstanding balance and transitions status to `PAID` only when outstanding balance reaches ₹0.00.
- Append-only immutable ledger: payments cannot be edited or deleted.

### 10. Executive & Finance Analytics Dashboard
- Calculated Metrics:
  - Monthly Recurring Revenue (MRR) normalized across all frequencies.
  - Annualized Run Rate (ARR = MRR * 12).
  - Cash Collected vs Invoiced Revenue vs Outstanding Balances.
  - Overdue Invoices identification (count & total amount).
  - 30-day upcoming contract renewal pipeline.
  - 6-month historical invoiced vs collected revenue trend timeline.
  - Status breakdowns for subscriptions, invoices, and payment channels.

### 11. Customer Self-Service Portal
- Isolated view for corporate clients:
  - Active subscriptions and upcoming billing dates.
  - Invoice history and printable tax statements.
  - Payment receipts and outstanding balance tracking.
  - Self-service subscription pause/resume controls.
  - Notification inbox.

---

## 2. Technology Stack & Architectural Decisions

| Layer | Technology | Decision Rationale |
|---|---|---|
| **Language** | Java 25 LTS | Pattern matching, records, virtual threads, and modern memory efficiency. |
| **Framework** | Spring Boot 3.5.16 | Java 25-compatible production ecosystem with Spring Data JPA, Spring Security 6, and Actuator. |
| **Security** | Spring Security + JWT | Stateless bearer tokens with UUID `jti` for revocation, BCrypt strength 12 password hashing. |
| **Database** | PostgreSQL 16 (Prod) / H2 (Dev/Test) | Acid-compliant relational database with Flyway schema versioning. Decimal types (`NUMERIC(14,2)`) for zero floating-point errors. |
| **Frontend** | React 18 + TypeScript + Vite | Strongly typed UI, fast hot-reloading, responsive layout with Tailwind CSS. |
| **Testing** | JUnit 5 + MockMvc + Mockito | Integration test suite covering authentication, variant pricing, lifecycle state machine, billing scheduler, and 1,000-subscription benchmarks. |
| **API Docs** | SpringDoc OpenAPI 3 / Swagger | Interactive interactive documentation at `/swagger-ui.html`. |
| **Containers** | Docker & Docker Compose | Multi-stage lightweight Alpine images for zero-dependency orchestration. |

---

## 3. Pre-Seeded Default Accounts

The database is pre-seeded via Flyway and `DataInitializer.java` with test accounts:

| Role | Email | Password | Access Rights |
|---|---|---|---|
| **ADMIN** | `admin@brightloop.com` | `Admin@12345` | Full system control, catalog, plans, staff creation, finance analytics, billing trigger |
| **STAFF** | `staff@brightloop.com` | `Staff@12345` | Customer operations, quotations, subscriptions, invoices, payment recording |
| **CUSTOMER** | `customer@brightloop.com` | `Customer@12345` | Customer portal, own subscriptions, invoice downloads, balance tracking |

*The login UI includes 1-click credential buttons for instant testing.*

---

## 4. Performance Benchmarks (< 2.0s SLA)

Verified in `BenchmarkPerformanceTests.java`:
- **Active Subscription Scale**: 1,000 active subscriptions populated in database.
- **Dashboard Metric Aggregation**: Completed in **< 350 ms** (SLA: < 2,000 ms).
- **Automated Recurring Billing Engine Run**: Processed and generated invoices for due subscriptions in **< 400 ms** (SLA: < 2,000 ms).
- **Full Integration Suite**: 23 test cases spanning all modules pass in **~25 seconds**.

---

## 5. Getting Started & Running Locally

### Prerequisites
- Java 25 LTS (`export JAVA_HOME=...`)
- Maven 3.9+
- Node.js 20+ & npm

### Backend Setup
```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home
mvn clean test -o     # Run the full 23-test integration suite
mvn spring-boot:run   # Start backend on http://localhost:8080
```
- API Base URL: `http://localhost:8080/api/v1`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON Spec: `http://localhost:8080/v3/api-docs`
- Spring Boot Actuator Health: `http://localhost:8080/actuator/health`

### Frontend Setup
```bash
cd frontend
npm install
npm run build         # TypeScript type-check and Vite production build
npm run dev           # Start frontend dev server on http://localhost:3000
```
- Open `http://localhost:3000` in your browser.

---

## 6. Docker Deployment

To spin up the entire production stack (PostgreSQL 16, Spring Boot backend, and React frontend with Nginx):

```bash
docker-compose up --build
```

- **Frontend Application**: `http://localhost:3000` (or `http://localhost:80`)
- **Backend API**: `http://localhost:8080`
- **PostgreSQL Database**: `localhost:5432` (Database: `brightloop_prod`)

---

## 7. REST API Reference

| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/v1/auth/login` | `POST` | Public | Authenticate user & return JWT + refresh token |
| `/api/v1/auth/register` | `POST` | Public | Register enterprise customer account |
| `/api/v1/auth/forgot-password` | `POST` | Public | Request expiring 15-minute reset token |
| `/api/v1/auth/reset-password` | `POST` | Public | Submit reset token & update password |
| `/api/v1/admin/staff` | `GET`, `POST` | Admin | Provision and list operational staff accounts |
| `/api/v1/products` | `GET`, `POST` | Admin/Staff | Product catalog management |
| `/api/v1/products/{id}/variants` | `POST` | Admin | Add configurable pricing variant (e.g. Odoo +₹560) |
| `/api/v1/plans` | `GET`, `POST` | Admin/Staff | Subscription plan configuration |
| `/api/v1/quotation-templates` | `GET`, `POST` | Admin/Staff | Reusable quotation templates |
| `/api/v1/quotations` | `GET`, `POST` | Admin/Staff | Create quotes & items |
| `/api/v1/quotations/{id}/confirm` | `POST` | Admin/Staff | Confirm quote & convert into active subscription |
| `/api/v1/subscriptions` | `GET`, `POST` | Admin/Staff | List subscriptions with filters |
| `/api/v1/subscriptions/{id}/pause` | `POST` | Admin/Staff | Pause subscription |
| `/api/v1/subscriptions/{id}/resume` | `POST` | Admin/Staff | Resume subscription |
| `/api/v1/subscriptions/{id}/close` | `POST` | Admin/Staff | Early closure of subscription |
| `/api/v1/subscriptions/{id}/renew` | `POST` | Admin/Staff | Renew subscription period |
| `/api/v1/billing/trigger` | `POST` | Admin | Manually trigger automated recurring billing run |
| `/api/v1/invoices` | `GET` | Admin/Staff | Invoices with overdue and status filters |
| `/api/v1/invoices/{id}/print` | `GET` | All | Get printable enterprise HTML tax invoice |
| `/api/v1/payments` | `GET`, `POST` | Admin/Staff | Record payments (UPI, Bank, Card, Cash) & reconcile |
| `/api/v1/taxes` | `GET`, `POST` | Admin/Staff | Centralized tax rules (GST, VAT) |
| `/api/v1/discounts` | `GET`, `POST` | Admin | Promotional discounts with concurrency limits |
| `/api/v1/dashboard/metrics` | `GET` | Admin/Staff | Aggregated finance metrics (MRR, ARR, Cashflow) |
| `/api/v1/dashboard/revenue-trend` | `GET` | Admin/Staff | 6-month historical revenue trend points |
| `/api/v1/customer/dashboard` | `GET` | Customer | Customer self-service dashboard |
| `/api/v1/customer/subscriptions` | `GET` | Customer | Customer's isolated subscription list |
| `/api/v1/customer/invoices` | `GET` | Customer | Customer's isolated invoices list |
| `/api/v1/subscriptions/{id}/prorate/calculate` | `POST` | All | Calculate daily pro-rata credit/charge for mid-cycle plan switch |
| `/api/v1/subscriptions/{id}/prorate/apply` | `POST` | Admin/Staff | Apply mid-cycle switch & write immutable proration ledger record |
| `/api/v1/subscriptions/{id}/prorate/history` | `GET` | Admin/Staff | Audit history of subscription proration events |
| `/api/v1/dunning/cases` | `GET` | Admin/Staff | List active revenue recovery & overdue dunning cases |
| `/api/v1/dunning/process` | `POST` | Admin | Execute automated dunning recovery job & compound late fees |

---

## 8. MySQL 8.0+ Database Execution Guide

Brightloop Software includes first-class native support for **MySQL 8.0+** alongside PostgreSQL and H2 in-memory dev mode:

### Step 1: Initialize Database & Load Seed Data in MySQL
```bash
# Log in to MySQL and load the comprehensive 25-table DDL & seed script:
mysql -u root -p < database/mysql_schema_and_seed.sql
```

### Step 2: Run Spring Boot with MySQL Profile
```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home

# Start backend connected directly to your local MySQL instance:
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
Or execute the packaged jar with environment variables:
```bash
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/brightloop_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
SPRING_DATASOURCE_USERNAME="root" \
SPRING_DATASOURCE_PASSWORD="your_password" \
java -jar target/subscription-management-system-1.0.0-SNAPSHOT.jar --spring.profiles.active=mysql
```
*(By default, running with `--spring.profiles.active=dev` uses the zero-config in-memory database with preloaded seed data).*
