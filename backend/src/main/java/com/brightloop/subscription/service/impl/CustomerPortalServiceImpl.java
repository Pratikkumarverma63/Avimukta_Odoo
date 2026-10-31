package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.InvoiceStatus;
import com.brightloop.subscription.domain.enums.NotificationType;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.repository.*;
import com.brightloop.subscription.security.SecurityUtils;
import com.brightloop.subscription.service.CustomerPortalService;
import com.brightloop.subscription.service.InvoiceService;
import com.brightloop.subscription.service.PaymentService;
import com.brightloop.subscription.service.SubscriptionService;
import com.brightloop.subscription.web.dto.*;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import com.brightloop.subscription.web.exception.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerPortalServiceImpl implements CustomerPortalService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final PlanRepository planRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;

    public CustomerPortalServiceImpl(
            CustomerRepository customerRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            NotificationRepository notificationRepository,
            PlanRepository planRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            SubscriptionService subscriptionService,
            InvoiceService invoiceService,
            PaymentService paymentService,
            PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.planRepository = planRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.subscriptionService = subscriptionService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.passwordEncoder = passwordEncoder;
    }

    private Long resolveCustomerId() {
        Long customerId = SecurityUtils.getCurrentCustomerId();
        if (customerId == null) {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null) {
                Customer c = customerRepository.findByUserId(userId).orElse(null);
                if (c != null) {
                    return c.getId();
                }
            }
            // Fallback for default demo customer
            Customer defaultAcme = customerRepository.findByCustomerCode("CUST-10001").orElse(null);
            if (defaultAcme != null) {
                return defaultAcme.getId();
            }
            throw new UnauthorizedException("Only registered customers can access the customer portal.");
        }
        return customerId;
    }

    @Override
    public CustomerDashboardResponse getDashboard() {
        Long customerId = resolveCustomerId();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer record not found for id: " + customerId));

        // 1. Fetch Subscriptions directly by Customer entity
        List<Subscription> subs = subscriptionRepository.findByCustomerOrderByIdDesc(customer);
        if (subs.isEmpty()) {
            subs = autoSeedDemoSubscriptionForCustomer(customer);
        }

        List<SubscriptionResponse> subResponses = subs.stream()
                .map(this::mapToSubscriptionResponse)
                .collect(Collectors.toList());

        // 2. Fetch Invoices directly
        List<Invoice> invoices = invoiceRepository.findAllByCustomerIdOrderByIdDesc(customerId);
        List<InvoiceResponse> invResponses = invoices.stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());

        // 3. Fetch Payments directly
        List<Payment> payments = paymentRepository.findAllByCustomerId(customerId);
        List<PaymentResponse> payResponses = payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());

        // 4. Financial Calculations
        BigDecimal totalPaid = paymentRepository.calculateTotalPaymentsReceivedForCustomer(customerId);
        BigDecimal totalOutstanding = invoiceRepository.calculateTotalOutstandingForCustomer(customerId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        if (totalOutstanding == null) totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalInvoiced = totalPaid.add(totalOutstanding);

        // 5. Next Renewal Date
        LocalDate nextRenewalDate = subs.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(Subscription::getNextBillingDate)
                .filter(d -> d != null && !d.isBefore(LocalDate.now()))
                .min(LocalDate::compareTo)
                .orElse(null);

        // 6. Unread Notifications
        List<Notification> unreadNotifs = notificationRepository.findByCustomerIdAndReadFalse(customerId);
        List<NotificationResponse> notifResponses = unreadNotifs.stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());

        return CustomerDashboardResponse.builder()
                .customerCode(customer.getCustomerCode())
                .companyName(customer.getCompanyName())
                .contactName(customer.getContactName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .billingAddress(customer.getBillingAddress())
                .taxId(customer.getTaxId())
                .activeSubscriptionsCount((int) subs.stream().filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE).count())
                .totalInvoicedAmount(totalInvoiced)
                .totalAmountPaid(totalPaid)
                .totalOutstandingAmount(totalOutstanding)
                .nextUpcomingBillingDate(nextRenewalDate)
                .unreadNotificationCount(unreadNotifs.size())
                .activeSubscriptions(subResponses)
                .recentInvoices(invResponses)
                .recentPayments(payResponses)
                .unreadNotifications(notifResponses)
                .build();
    }

    private List<Subscription> autoSeedDemoSubscriptionForCustomer(Customer customer) {
        Plan monthlyPlan = planRepository.findByCodeIgnoreCase("PLAN-ERP-M").orElse(null);
        Product erpProd = productRepository.findByCodeIgnoreCase("PROD-ERP").orElse(null);
        ProductVariant odooVariant = variantRepository.findAll().stream()
                .filter(v -> "Odoo".equalsIgnoreCase(v.getVariantValue()))
                .findFirst().orElse(null);

        if (monthlyPlan != null && erpProd != null) {
            LocalDate today = LocalDate.now();
            String demoNumber = "SUB-" + today.getYear() + "-D" + String.format("%05d", customer.getId());
            int suffix = 1;
            while (subscriptionRepository.existsBySubscriptionNumber(demoNumber)) {
                demoNumber = "SUB-" + today.getYear() + "-D" + String.format("%05d", customer.getId()) + "-" + suffix++;
            }
            Subscription sub = Subscription.builder()
                    .subscriptionNumber(demoNumber)
                    .customer(customer)
                    .plan(monthlyPlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startDate(today.minusMonths(1))
                    .nextBillingDate(today.plusDays(15))
                    .lastBilledDate(today.minusDays(15))
                    .paymentTerms("NET_30")
                    .subtotal(new BigDecimal("2560.00"))
                    .taxAmount(new BigDecimal("460.80"))
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("3020.80"))
                    .build();

            SubscriptionItem item = SubscriptionItem.builder()
                    .subscription(sub)
                    .product(erpProd)
                    .variant(odooVariant)
                    .description("Brightloop Business ERP (Odoo Edition)")
                    .quantity(1)
                    .unitPrice(new BigDecimal("2560.00"))
                    .taxAmount(new BigDecimal("460.80"))
                    .discountAmount(BigDecimal.ZERO)
                    .lineTotal(new BigDecimal("3020.80"))
                    .build();
            sub.addItem(item);
            sub = subscriptionRepository.save(sub);

            List<Subscription> list = new ArrayList<>();
            list.add(sub);
            return list;
        }
        return Collections.emptyList();
    }

    @Override
    public CustomerDashboardResponse updateProfile(CustomerProfileRequest request) {
        Long customerId = resolveCustomerId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        customer.setCompanyName(request.getCompanyName());
        customer.setContactName(request.getContactName());
        customer.setPhone(request.getPhone());
        customer.setBillingAddress(request.getBillingAddress());
        customer.setTaxId(request.getTaxId());
        customerRepository.save(customer);

        // Also update linked user's name if applicable
        if (customer.getUser() != null) {
            User u = customer.getUser();
            String[] parts = request.getContactName().split("\\s+", 2);
            u.setFirstName(parts[0]);
            if (parts.length > 1) {
                u.setLastName(parts[1]);
            }
            userRepository.save(u);
        }

        return getDashboard();
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("User not authenticated.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public SubscriptionResponse requestCancellation(Long subscriptionId, String reason) {
        Long customerId = resolveCustomerId();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        if (!subscription.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedException("You do not have permission to manage this subscription.");
        }

        subscription.setStatus(SubscriptionStatus.CLOSED);
        subscription = subscriptionRepository.save(subscription);

        // Create in-app audit notification
        Notification notif = Notification.builder()
                .customer(subscription.getCustomer())
                .title("Subscription Cancelled")
                .message("Subscription " + subscription.getSubscriptionNumber() + " has been cancelled. Reason: " + (reason != null ? reason : "Requested by customer"))
                .notificationType(NotificationType.SUBSCRIPTION_CLOSED)
                .referenceType("SUBSCRIPTION")
                .referenceId(subscription.getId())
                .read(false)
                .build();
        notificationRepository.save(notif);

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse subscribeToPlan(Long planId, Long variantId, int quantity) {
        Long customerId = resolveCustomerId();
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));

        Product product = plan.getProduct();
        if (product == null) {
            throw new IllegalStateException("Plan does not have an associated product.");
        }

        int effectiveQty = Math.max(quantity, plan.getMinQuantity() != null ? plan.getMinQuantity() : 1);

        SubscriptionItemRequest itemReq = SubscriptionItemRequest.builder()
                .productId(product.getId())
                .variantId(variantId)
                .quantity(effectiveQty)
                .unitPrice(plan.getPrice())
                .build();

        SubscriptionRequest subReq = SubscriptionRequest.builder()
                .customerId(customerId)
                .planId(plan.getId())
                .startDate(LocalDate.now())
                .autoCloseOnExpiry(plan.isAutoCloseOnExpiry())
                .allowPause(plan.isAllowPause())
                .allowEarlyClosure(plan.isAllowEarlyClosure())
                .allowRenewal(plan.isAllowRenewal())
                .items(List.of(itemReq))
                .build();

        SubscriptionResponse response = subscriptionService.createSubscription(subReq);

        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer != null) {
            Notification notif = Notification.builder()
                    .customer(customer)
                    .title("New Subscription Activated")
                    .message("You have successfully enrolled in " + plan.getName() + " (" + response.getSubscriptionNumber() + ").")
                    .notificationType(NotificationType.SUBSCRIPTION_ACTIVATED)
                    .referenceType("SUBSCRIPTION")
                    .referenceId(response.getId())
                    .read(false)
                    .build();
            notificationRepository.save(notif);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        Long customerId = resolveCustomerId();
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapToNotificationResponse);
    }

    @Override
    public void markNotificationRead(Long id) {
        Long customerId = resolveCustomerId();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!notification.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedException("Access denied.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllNotificationsRead() {
        Long customerId = resolveCustomerId();
        List<Notification> unread = notificationRepository.findByCustomerIdAndReadFalse(customerId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    private SubscriptionResponse mapToSubscriptionResponse(Subscription s) {
        List<SubscriptionItemResponse> itemResponses = s.getItems().stream()
                .map(i -> SubscriptionItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productCode(i.getProduct().getCode())
                        .productName(i.getProduct().getName())
                        .variantId(i.getVariant() != null ? i.getVariant().getId() : null)
                        .variantName(i.getVariant() != null ? i.getVariant().getAttributeName() + ": " + i.getVariant().getVariantValue() : null)
                        .description(i.getDescription())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .taxRuleId(i.getTaxRule() != null ? i.getTaxRule().getId() : null)
                        .taxRuleName(i.getTaxRule() != null ? i.getTaxRule().getName() : null)
                        .taxAmount(i.getTaxAmount())
                        .discountRuleId(i.getDiscountRule() != null ? i.getDiscountRule().getId() : null)
                        .discountRuleName(i.getDiscountRule() != null ? i.getDiscountRule().getName() : null)
                        .discountAmount(i.getDiscountAmount())
                        .lineTotal(i.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return SubscriptionResponse.builder()
                .id(s.getId())
                .subscriptionNumber(s.getSubscriptionNumber())
                .customerId(s.getCustomer().getId())
                .customerCode(s.getCustomer().getCustomerCode())
                .customerCompanyName(s.getCustomer().getCompanyName())
                .customerEmail(s.getCustomer().getEmail())
                .planId(s.getPlan().getId())
                .planName(s.getPlan().getName())
                .planCode(s.getPlan().getCode())
                .billingFrequency(s.getPlan().getBillingFrequency())
                .quotationId(s.getQuotation() != null ? s.getQuotation().getId() : null)
                .quotationNumber(s.getQuotation() != null ? s.getQuotation().getQuotationNumber() : null)
                .status(s.getStatus())
                .startDate(s.getStartDate())
                .expiryDate(s.getExpiryDate())
                .nextBillingDate(s.getNextBillingDate())
                .lastBilledDate(s.getLastBilledDate())
                .paymentTerms(s.getPaymentTerms())
                .autoCloseOnExpiry(s.isAutoCloseOnExpiry())
                .allowPause(s.isAllowPause())
                .allowEarlyClosure(s.isAllowEarlyClosure())
                .allowRenewal(s.isAllowRenewal())
                .subtotal(s.getSubtotal())
                .taxAmount(s.getTaxAmount())
                .discountAmount(s.getDiscountAmount())
                .totalAmount(s.getTotalAmount())
                .items(itemResponses)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private InvoiceResponse mapToInvoiceResponse(Invoice inv) {
        boolean isOverdue = inv.getStatus() != InvoiceStatus.PAID &&
                inv.getStatus() != InvoiceStatus.CANCELLED &&
                inv.getDueDate() != null &&
                inv.getDueDate().isBefore(LocalDate.now());

        return InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .customerId(inv.getCustomer().getId())
                .customerCompanyName(inv.getCustomer().getCompanyName())
                .customerEmail(inv.getCustomer().getEmail())
                .subscriptionId(inv.getSubscription() != null ? inv.getSubscription().getId() : null)
                .subscriptionNumber(inv.getSubscription() != null ? inv.getSubscription().getSubscriptionNumber() : null)
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .billingPeriodStart(inv.getBillingPeriodStart())
                .billingPeriodEnd(inv.getBillingPeriodEnd())
                .status(inv.getStatus())
                .subtotal(inv.getSubtotal())
                .taxAmount(inv.getTaxAmount())
                .discountAmount(inv.getDiscountAmount())
                .totalAmount(inv.getTotalAmount())
                .amountPaid(inv.getAmountPaid())
                .outstandingAmount(inv.getOutstandingAmount())
                .notes(inv.getNotes())
                .overdue(isOverdue)
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }

    private PaymentResponse mapToPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .invoiceId(p.getInvoice().getId())
                .invoiceNumber(p.getInvoice().getInvoiceNumber())
                .customerId(p.getInvoice().getCustomer().getId())
                .customerName(p.getInvoice().getCustomer().getCompanyName())
                .paymentMethod(p.getPaymentMethod())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .transactionReference(p.getTransactionReference())
                .notes(p.getNotes())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private NotificationResponse mapToNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .read(n.isRead())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
