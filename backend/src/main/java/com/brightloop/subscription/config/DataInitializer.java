package com.brightloop.subscription.config;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.*;
import com.brightloop.subscription.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final QuotationRepository quotationRepository;
    private final NotificationRepository notificationRepository;
    private final ProrationRecordRepository prorationRepository;
    private final DunningCaseRepository dunningRepository;

    public DataInitializer(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            QuotationRepository quotationRepository,
            NotificationRepository notificationRepository,
            ProrationRecordRepository prorationRepository,
            DunningCaseRepository dunningRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.quotationRepository = quotationRepository;
        this.notificationRepository = notificationRepository;
        this.prorationRepository = prorationRepository;
        this.dunningRepository = dunningRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed Admin
        User admin = userRepository.findByEmailIgnoreCase("admin@brightloop.com").orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .email("admin@brightloop.com")
                    .password(passwordEncoder.encode("Admin@12345"))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Role.ROLE_ADMIN)
                    .active(true)
                    .build();
            admin = userRepository.save(admin);
            log.info("Initialized default administrator: admin@brightloop.com");
        }

        // 2. Seed Staff
        User staff = userRepository.findByEmailIgnoreCase("staff@brightloop.com").orElse(null);
        if (staff == null) {
            staff = User.builder()
                    .email("staff@brightloop.com")
                    .password(passwordEncoder.encode("Staff@12345"))
                    .firstName("Sarah")
                    .lastName("Sales")
                    .role(Role.ROLE_STAFF)
                    .active(true)
                    .build();
            staff = userRepository.save(staff);
            log.info("Initialized default staff user: staff@brightloop.com");
        }

        // 3. Seed Demo Customer 1 (Acme Retail - Raj Patel)
        User cust1User = userRepository.findByEmailIgnoreCase("customer@brightloop.com").orElse(null);
        Customer acme = null;
        if (cust1User == null) {
            cust1User = User.builder()
                    .email("customer@brightloop.com")
                    .password(passwordEncoder.encode("Customer@12345"))
                    .firstName("Raj")
                    .lastName("Patel")
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build();
            cust1User = userRepository.save(cust1User);

            acme = Customer.builder()
                    .user(cust1User)
                    .customerCode("CUST-10001")
                    .companyName("Acme Retail Solutions")
                    .contactName("Raj Patel")
                    .email("customer@brightloop.com")
                    .phone("+91-9876543210")
                    .billingAddress("42 Commercial Street, Bangalore, Karnataka 560001")
                    .taxId("GSTIN29AABCU9603R1Z7")
                    .build();
            acme = customerRepository.save(acme);
            cust1User.setCustomer(acme);
        } else {
            acme = customerRepository.findByUserId(cust1User.getId()).orElse(null);
        }

        // 4. Seed Additional Enterprise Customers if needed
        Customer tata = seedCustomerIfMissing("tata.billing@tcs-cloud.com", "Priya", "Sharma", "CUST-10002",
                "Tata Consultancy Global", "Bandra Kurla Complex, Mumbai, Maharashtra 400051", "GSTIN27AAACT2727Q1ZW");
        Customer infosys = seedCustomerIfMissing("finance@infosys-labs.in", "Amit", "Verma", "CUST-10003",
                "Infosys FinTech Labs", "Electronics City, Bengaluru, Karnataka 560100", "GSTIN29AAACI4321P1ZQ");
        Customer reliance = seedCustomerIfMissing("procurement@relianceretail.com", "Vikram", "Singh", "CUST-10004",
                "Reliance Omnichannel Retail", "Ghansoli, Navi Mumbai, Maharashtra 400701", "GSTIN27AAACR1234M1ZV");
        Customer zomato = seedCustomerIfMissing("accounts@zomatotech.com", "Sneha", "Rao", "CUST-10005",
                "Zomato Hyperlocal Technologies", "DLF Phase 5, Gurugram, Haryana 122002", "GSTIN06AAACZ5678K1ZT");

        // 5. Seed Subscriptions, Invoices, Payments if database is empty of subscriptions
        if (subscriptionRepository.count() == 0 && acme != null) {
            Product erpProd = productRepository.findByCodeIgnoreCase("PROD-ERP").orElse(null);
            Product analyticsProd = productRepository.findByCodeIgnoreCase("PROD-ANALYTICS").orElse(null);
            ProductVariant odooVariant = variantRepository.findAll().stream()
                    .filter(v -> "Odoo".equalsIgnoreCase(v.getVariantValue()))
                    .findFirst().orElse(null);
            ProductVariant aiVariant = variantRepository.findAll().stream()
                    .filter(v -> "AI Predictive Analytics".equalsIgnoreCase(v.getVariantValue()))
                    .findFirst().orElse(null);
            Plan monthlyPlan = planRepository.findByCodeIgnoreCase("PLAN-ERP-M").orElse(null);
            Plan annualPlan = planRepository.findByCodeIgnoreCase("PLAN-ERP-Y").orElse(null);
            Plan weeklyPlan = planRepository.findByCodeIgnoreCase("PLAN-ERP-W").orElse(null);

            LocalDate today = LocalDate.now();

            if (monthlyPlan != null && erpProd != null) {
                // Subscription 1: Acme - ERP Monthly with Odoo Variant (+₹560)
                Subscription sub1 = Subscription.builder()
                        .subscriptionNumber("SUB-2026-10001")
                        .customer(acme)
                        .plan(monthlyPlan)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(today.minusMonths(2))
                        .nextBillingDate(today.plusDays(15))
                        .lastBilledDate(today.minusDays(15))
                        .paymentTerms("NET_30")
                        .subtotal(new BigDecimal("2560.00")) // 2000 base + 560 Odoo
                        .taxAmount(new BigDecimal("460.80"))   // 18% GST
                        .discountAmount(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("3020.80"))
                        .build();

                SubscriptionItem item1 = SubscriptionItem.builder()
                        .subscription(sub1)
                        .product(erpProd)
                        .variant(odooVariant)
                        .description("Brightloop Business ERP (Odoo Edition)")
                        .quantity(1)
                        .unitPrice(new BigDecimal("2560.00"))
                        .taxAmount(new BigDecimal("460.80"))
                        .discountAmount(BigDecimal.ZERO)
                        .lineTotal(new BigDecimal("3020.80"))
                        .build();
                sub1.addItem(item1);
                sub1 = subscriptionRepository.save(sub1);

                // Subscription 2: Acme - AI Analytics Add-on
                Subscription sub2 = Subscription.builder()
                        .subscriptionNumber("SUB-2026-10002")
                        .customer(acme)
                        .plan(monthlyPlan)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(today.minusMonths(1))
                        .nextBillingDate(today.plusDays(20))
                        .lastBilledDate(today.minusDays(10))
                        .paymentTerms("NET_30")
                        .subtotal(new BigDecimal("900.00"))  // 500 base + 400 AI
                        .taxAmount(new BigDecimal("162.00"))  // 18% GST
                        .discountAmount(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("1062.00"))
                        .build();
                if (analyticsProd != null) {
                    SubscriptionItem item2 = SubscriptionItem.builder()
                            .subscription(sub2)
                            .product(analyticsProd)
                            .variant(aiVariant)
                            .description("Advanced Analytics Add-on (AI Predictive Tier)")
                            .quantity(1)
                            .unitPrice(new BigDecimal("900.00"))
                            .taxAmount(new BigDecimal("162.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .lineTotal(new BigDecimal("1062.00"))
                            .build();
                    sub2.addItem(item2);
                }
                sub2 = subscriptionRepository.save(sub2);

                // Invoice 1: Acme - Cycle 1 (PAID)
                Invoice inv1 = Invoice.builder()
                        .invoiceNumber("INV-2026-00101")
                        .customer(acme)
                        .subscription(sub1)
                        .invoiceDate(today.minusMonths(2))
                        .dueDate(today.minusMonths(2).plusDays(30))
                        .billingPeriodStart(today.minusMonths(2))
                        .billingPeriodEnd(today.minusMonths(1))
                        .status(InvoiceStatus.PAID)
                        .subtotal(new BigDecimal("2560.00"))
                        .taxAmount(new BigDecimal("460.80"))
                        .discountAmount(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("3020.80"))
                        .amountPaid(new BigDecimal("3020.80"))
                        .outstandingAmount(BigDecimal.ZERO)
                        .build();
                inv1.addItem(InvoiceItem.builder()
                        .invoice(inv1)
                        .product(erpProd)
                        .variant(odooVariant)
                        .description("Brightloop Business ERP (Odoo Edition) - Billing Cycle 1")
                        .quantity(1)
                        .unitPrice(new BigDecimal("2560.00"))
                        .taxAmount(new BigDecimal("460.80"))
                        .discountAmount(BigDecimal.ZERO)
                        .lineTotal(new BigDecimal("3020.80"))
                        .build());
                inv1 = invoiceRepository.save(inv1);

                Payment pay1 = Payment.builder()
                        .paymentNumber("PAY-2026-00101")
                        .invoice(inv1)
                        .amount(new BigDecimal("3020.80"))
                        .paymentMethod(PaymentMethod.UPI)
                        .transactionReference("UPI/430198273612")
                        .paymentDate(OffsetDateTime.now().minusMonths(2).plusDays(5))
                        .status(PaymentStatus.COMPLETED)
                        .notes("Instant UPI settlement via PhonePe Business")
                        .build();
                paymentRepository.save(pay1);

                // Invoice 2: Acme - Cycle 2 (PAID)
                Invoice inv2 = Invoice.builder()
                        .invoiceNumber("INV-2026-00102")
                        .customer(acme)
                        .subscription(sub1)
                        .invoiceDate(today.minusMonths(1))
                        .dueDate(today.minusMonths(1).plusDays(30))
                        .billingPeriodStart(today.minusMonths(1))
                        .billingPeriodEnd(today)
                        .status(InvoiceStatus.PAID)
                        .subtotal(new BigDecimal("2560.00"))
                        .taxAmount(new BigDecimal("460.80"))
                        .discountAmount(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("3020.80"))
                        .amountPaid(new BigDecimal("3020.80"))
                        .outstandingAmount(BigDecimal.ZERO)
                        .build();
                inv2.addItem(InvoiceItem.builder()
                        .invoice(inv2)
                        .product(erpProd)
                        .variant(odooVariant)
                        .description("Brightloop Business ERP (Odoo Edition) - Billing Cycle 2")
                        .quantity(1)
                        .unitPrice(new BigDecimal("2560.00"))
                        .taxAmount(new BigDecimal("460.80"))
                        .discountAmount(BigDecimal.ZERO)
                        .lineTotal(new BigDecimal("3020.80"))
                        .build());
                inv2 = invoiceRepository.save(inv2);

                Payment pay2 = Payment.builder()
                        .paymentNumber("PAY-2026-00102")
                        .invoice(inv2)
                        .amount(new BigDecimal("3020.80"))
                        .paymentMethod(PaymentMethod.BANK_TRANSFER)
                        .transactionReference("NEFT/HDFC998231456")
                        .paymentDate(OffsetDateTime.now().minusMonths(1).plusDays(7))
                        .status(PaymentStatus.COMPLETED)
                        .notes("Corporate NEFT bank settlement verified")
                        .build();
                paymentRepository.save(pay2);

                // Invoice 3: Acme - Current Cycle (CONFIRMED / UNPAID)
                Invoice inv3 = Invoice.builder()
                        .invoiceNumber("INV-2026-00103")
                        .customer(acme)
                        .subscription(sub2)
                        .invoiceDate(today.minusDays(5))
                        .dueDate(today.plusDays(10))
                        .billingPeriodStart(today.minusDays(5))
                        .billingPeriodEnd(today.plusDays(25))
                        .status(InvoiceStatus.CONFIRMED)
                        .subtotal(new BigDecimal("900.00"))
                        .taxAmount(new BigDecimal("162.00"))
                        .discountAmount(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("1062.00"))
                        .amountPaid(BigDecimal.ZERO)
                        .outstandingAmount(new BigDecimal("1062.00"))
                        .build();
                if (analyticsProd != null) {
                    inv3.addItem(InvoiceItem.builder()
                            .invoice(inv3)
                            .product(analyticsProd)
                            .variant(aiVariant)
                            .description("Advanced Analytics Add-on (AI Predictive Tier)")
                            .quantity(1)
                            .unitPrice(new BigDecimal("900.00"))
                            .taxAmount(new BigDecimal("162.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .lineTotal(new BigDecimal("1062.00"))
                            .build());
                }
                invoiceRepository.save(inv3);

                // Notifications for Acme
                createNotification(acme, "Invoice INV-2026-00103 Generated",
                        "Your monthly billing invoice of ₹1,062.00 for AI Analytics Add-on is ready. Due date: " + today.plusDays(10),
                        NotificationType.INVOICE_GENERATED);
                createNotification(acme, "Payment Receipt Confirmed",
                        "Receipt PAY-2026-00102 of ₹3,020.80 successfully reconciled via Corporate NEFT.",
                        NotificationType.PAYMENT_RECEIVED);
                createNotification(acme, "Odoo Brand Variant Activated",
                        "Your subscription SUB-2026-10001 is active with configurable Odoo brand add-on (+₹560/mo).",
                        NotificationType.RENEWAL_DUE);

                // Subscription 3: Tata - Annual Contract (ACTIVE, PAID)
                if (annualPlan != null && tata != null) {
                    Subscription subTata = Subscription.builder()
                            .subscriptionNumber("SUB-2026-20001")
                            .customer(tata)
                            .plan(annualPlan)
                            .status(SubscriptionStatus.ACTIVE)
                            .startDate(today.minusMonths(3))
                            .nextBillingDate(today.plusMonths(9))
                            .lastBilledDate(today.minusMonths(3))
                            .paymentTerms("NET_45")
                            .subtotal(new BigDecimal("23100.00"))
                            .taxAmount(new BigDecimal("4158.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("27258.00"))
                            .build();
                    subTata = subscriptionRepository.save(subTata);

                    Invoice invTata = Invoice.builder()
                            .invoiceNumber("INV-2026-00201")
                            .customer(tata)
                            .subscription(subTata)
                            .invoiceDate(today.minusMonths(3))
                            .dueDate(today.minusMonths(3).plusDays(45))
                            .billingPeriodStart(today.minusMonths(3))
                            .billingPeriodEnd(today.plusMonths(9))
                            .status(InvoiceStatus.PAID)
                            .subtotal(new BigDecimal("23100.00"))
                            .taxAmount(new BigDecimal("4158.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("27258.00"))
                            .amountPaid(new BigDecimal("27258.00"))
                            .outstandingAmount(BigDecimal.ZERO)
                            .build();
                    invTata.addItem(InvoiceItem.builder()
                            .invoice(invTata)
                            .product(erpProd)
                            .variant(odooVariant)
                            .description("ERP Annual Contract - Enterprise Multi-Company Edition")
                            .quantity(1)
                            .unitPrice(new BigDecimal("23100.00"))
                            .taxAmount(new BigDecimal("4158.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .lineTotal(new BigDecimal("27258.00"))
                            .build());
                    invTata = invoiceRepository.save(invTata);

                    Payment payTata = Payment.builder()
                            .paymentNumber("PAY-2026-00201")
                            .invoice(invTata)
                            .amount(new BigDecimal("27258.00"))
                            .paymentMethod(PaymentMethod.BANK_TRANSFER)
                            .transactionReference("RTGS/SBIN002938127")
                            .paymentDate(OffsetDateTime.now().minusMonths(3).plusDays(12))
                            .status(PaymentStatus.COMPLETED)
                            .notes("Tata corporate treasury RTGS remittance")
                            .build();
                    paymentRepository.save(payTata);

                    // Tata Partial Payment Invoice
                    Invoice invTata2 = Invoice.builder()
                            .invoiceNumber("INV-2026-00202")
                            .customer(tata)
                            .subscription(subTata)
                            .invoiceDate(today.minusDays(10))
                            .dueDate(today.plusDays(20))
                            .billingPeriodStart(today.minusDays(10))
                            .billingPeriodEnd(today.plusDays(20))
                            .status(InvoiceStatus.CONFIRMED)
                            .subtotal(new BigDecimal("1200.00"))
                            .taxAmount(new BigDecimal("216.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("1416.00"))
                            .amountPaid(new BigDecimal("500.00"))
                            .outstandingAmount(new BigDecimal("916.00"))
                            .build();
                    invTata2.addItem(InvoiceItem.builder()
                            .invoice(invTata2)
                            .product(productRepository.findByCodeIgnoreCase("PROD-SUPPORT").orElse(erpProd))
                            .description("24/7 Priority SLA Dedicated Support Extension")
                            .quantity(1)
                            .unitPrice(new BigDecimal("1200.00"))
                            .taxAmount(new BigDecimal("216.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .lineTotal(new BigDecimal("1416.00"))
                            .build());
                    invTata2 = invoiceRepository.save(invTata2);

                    Payment payTataPartial = Payment.builder()
                            .paymentNumber("PAY-2026-00202")
                            .invoice(invTata2)
                            .amount(new BigDecimal("500.00"))
                            .paymentMethod(PaymentMethod.CARD)
                            .transactionReference("CARD/VISA-CORP-4242")
                            .paymentDate(OffsetDateTime.now().minusDays(5))
                            .status(PaymentStatus.COMPLETED)
                            .notes("Part payment recorded via Corporate Visa Card")
                            .build();
                    paymentRepository.save(payTataPartial);
                }

                // Subscription 4: Infosys - PAUSED
                if (infosys != null) {
                    Subscription subInfo = Subscription.builder()
                            .subscriptionNumber("SUB-2026-30001")
                            .customer(infosys)
                            .plan(monthlyPlan)
                            .status(SubscriptionStatus.PAUSED)
                            .startDate(today.minusMonths(4))
                            .nextBillingDate(today.plusMonths(1))
                            .paymentTerms("NET_30")
                            .subtotal(new BigDecimal("2000.00"))
                            .taxAmount(new BigDecimal("360.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("2360.00"))
                            .build();
                    subInfo = subscriptionRepository.save(subInfo);

                    // Overdue Invoice for Infosys
                    Invoice invInfoOverdue = Invoice.builder()
                            .invoiceNumber("INV-2026-00301")
                            .customer(infosys)
                            .subscription(subInfo)
                            .invoiceDate(today.minusDays(45))
                            .dueDate(today.minusDays(15)) // 15 days overdue!
                            .billingPeriodStart(today.minusDays(45))
                            .billingPeriodEnd(today.minusDays(15))
                            .status(InvoiceStatus.CONFIRMED)
                            .subtotal(new BigDecimal("2000.00"))
                            .taxAmount(new BigDecimal("360.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("2360.00"))
                            .amountPaid(BigDecimal.ZERO)
                            .outstandingAmount(new BigDecimal("2360.00"))
                            .build();
                    invInfoOverdue.addItem(InvoiceItem.builder()
                            .invoice(invInfoOverdue)
                            .product(erpProd)
                            .description("Brightloop Business ERP - Standard Monthly Instance")
                            .quantity(1)
                            .unitPrice(new BigDecimal("2000.00"))
                            .taxAmount(new BigDecimal("360.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .lineTotal(new BigDecimal("2360.00"))
                            .build());
                    invInfoOverdue = invoiceRepository.save(invInfoOverdue);

                    // Smart Dunning Case for Overdue Invoice
                    DunningCase dunningCase = DunningCase.builder()
                            .invoice(invInfoOverdue)
                            .customer(infosys)
                            .dunningStage("LATE_FEE_APPLIED")
                            .daysOverdue(15)
                            .lateFeeAmount(new BigDecimal("47.20")) // 2% compound late fee
                            .lastActionDate(OffsetDateTime.now().minusDays(2))
                            .nextActionDate(OffsetDateTime.now().plusDays(2))
                            .attemptsCount(3)
                            .notes("Automated Dunning: 2 reminders dispatched. 2% late fee compounding applied.")
                            .build();
                    dunningRepository.save(dunningCase);
                }

                // Subscription 5: Reliance - Weekly Agile (ACTIVE)
                if (weeklyPlan != null && reliance != null) {
                    Subscription subRel = Subscription.builder()
                            .subscriptionNumber("SUB-2026-40001")
                            .customer(reliance)
                            .plan(weeklyPlan)
                            .status(SubscriptionStatus.ACTIVE)
                            .startDate(today.minusWeeks(3))
                            .nextBillingDate(today.plusDays(4))
                            .lastBilledDate(today.minusDays(3))
                            .paymentTerms("NET_15")
                            .subtotal(new BigDecimal("550.00"))
                            .taxAmount(new BigDecimal("99.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("649.00"))
                            .build();
                    subRel = subscriptionRepository.save(subRel);

                    Invoice invRel = Invoice.builder()
                            .invoiceNumber("INV-2026-00401")
                            .customer(reliance)
                            .subscription(subRel)
                            .invoiceDate(today.minusDays(10))
                            .dueDate(today.plusDays(5))
                            .billingPeriodStart(today.minusDays(10))
                            .billingPeriodEnd(today.minusDays(3))
                            .status(InvoiceStatus.PAID)
                            .subtotal(new BigDecimal("550.00"))
                            .taxAmount(new BigDecimal("99.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("649.00"))
                            .amountPaid(new BigDecimal("649.00"))
                            .outstandingAmount(BigDecimal.ZERO)
                            .build();
                    invRel.addItem(InvoiceItem.builder()
                            .invoice(invRel)
                            .product(erpProd)
                            .description("ERP Weekly Agile Sprint Instance")
                            .quantity(1)
                            .unitPrice(new BigDecimal("550.00"))
                            .taxAmount(new BigDecimal("99.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .lineTotal(new BigDecimal("649.00"))
                            .build());
                    invRel = invoiceRepository.save(invRel);

                    Payment payRel = Payment.builder()
                            .paymentNumber("PAY-2026-00401")
                            .invoice(invRel)
                            .amount(new BigDecimal("649.00"))
                            .paymentMethod(PaymentMethod.UPI)
                            .transactionReference("UPI/RAZOR88723461")
                            .paymentDate(OffsetDateTime.now().minusDays(8))
                            .status(PaymentStatus.COMPLETED)
                            .notes("Auto-debit UPI recurring mandate")
                            .build();
                    paymentRepository.save(payRel);
                }

                // Subscription 6: Zomato - CLOSED Early with Proration Credit
                if (zomato != null) {
                    Subscription subZomato = Subscription.builder()
                            .subscriptionNumber("SUB-2026-50001")
                            .customer(zomato)
                            .plan(monthlyPlan)
                            .status(SubscriptionStatus.CLOSED)
                            .startDate(today.minusMonths(2))
                            .paymentTerms("NET_30")
                            .subtotal(new BigDecimal("2000.00"))
                            .taxAmount(new BigDecimal("360.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("2360.00"))
                            .build();
                    subZomato = subscriptionRepository.save(subZomato);

                    // Proration record: Early closure with 14 unutilized days
                    ProrationRecord proration = ProrationRecord.builder()
                            .subscription(subZomato)
                            .customer(zomato)
                            .changeType("EARLY_TERMINATION")
                            .effectiveDate(today.minusDays(10))
                            .oldPlanPrice(new BigDecimal("2360.00"))
                            .newPlanPrice(BigDecimal.ZERO)
                            .daysUsed(16)
                            .daysInCycle(30)
                            .proratedCreditAmount(new BigDecimal("1101.33")) // (14/30) * 2360
                            .proratedChargeAmount(BigDecimal.ZERO)
                            .netAdjustmentAmount(new BigDecimal("-1101.33"))
                            .build();
                    prorationRepository.save(proration);
                }

                // Quotations
                Quotation q1 = Quotation.builder()
                        .quotationNumber("QUO-2026-0001")
                        .customer(acme)
                        .plan(annualPlan != null ? annualPlan : monthlyPlan)
                        .status(QuotationStatus.QUOTATION)
                        .issueDate(today.minusDays(10))
                        .expiryDate(today.plusDays(20))
                        .subtotal(new BigDecimal("21600.00"))
                        .taxAmount(new BigDecimal("3888.00"))
                        .discountAmount(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("25488.00"))
                        .notes("Annual upgrade proposal for Acme Retail Solutions")
                        .build();
                quotationRepository.save(q1);

                if (infosys != null) {
                    Quotation q2 = Quotation.builder()
                            .quotationNumber("QUO-2026-0002")
                            .customer(infosys)
                            .plan(monthlyPlan)
                            .status(QuotationStatus.DRAFT)
                            .issueDate(today.minusDays(2))
                            .expiryDate(today.plusDays(28))
                            .subtotal(new BigDecimal("900.00"))
                            .taxAmount(new BigDecimal("162.00"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("1062.00"))
                            .notes("Draft expansion quote for Analytics add-on")
                            .build();
                    quotationRepository.save(q2);
                }

                log.info("Comprehensive enterprise dataset initialized: 5 customers, 6 subscriptions, 7 invoices, 5 payments, 2 quotations, proration & dunning records!");
            }
        }
    }

    private Customer seedCustomerIfMissing(String email, String firstName, String lastName, String code, String company, String address, String taxId) {
        User u = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) {
            u = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("Corporate@12345"))
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build();
            u = userRepository.save(u);

            Customer c = Customer.builder()
                    .user(u)
                    .customerCode(code)
                    .companyName(company)
                    .contactName(firstName + " " + lastName)
                    .email(email)
                    .phone("+91-98" + (int)(Math.random() * 90000000 + 10000000))
                    .billingAddress(address)
                    .taxId(taxId)
                    .build();
            c = customerRepository.save(c);
            u.setCustomer(c);
            return c;
        }
        return customerRepository.findByUserId(u.getId()).orElse(null);
    }

    private void createNotification(Customer customer, String title, String message, NotificationType type) {
        Notification notif = Notification.builder()
                .customer(customer)
                .title(title)
                .message(message)
                .notificationType(type)
                .read(false)
                .build();
        notificationRepository.save(notif);
    }
}
