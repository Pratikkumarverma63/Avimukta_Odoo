package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.InvoiceStatus;
import com.brightloop.subscription.domain.enums.NotificationType;
import com.brightloop.subscription.domain.enums.PaymentStatus;
import com.brightloop.subscription.domain.enums.Role;
import com.brightloop.subscription.repository.InvoiceRepository;
import com.brightloop.subscription.repository.NotificationRepository;
import com.brightloop.subscription.repository.PaymentRepository;
import com.brightloop.subscription.security.SecurityUtils;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.PaymentService;
import com.brightloop.subscription.web.dto.PaymentRequest;
import com.brightloop.subscription.web.dto.PaymentResponse;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ForbiddenException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            NotificationRepository notificationRepository,
            AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public PaymentResponse recordPayment(PaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Invoice " + invoice.getInvoiceNumber() + " is already marked as PAID.");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cannot record payment against a CANCELLED invoice.");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero.");
        }

        if (request.getAmount().compareTo(invoice.getOutstandingAmount()) > 0) {
            throw new BadRequestException("Payment amount (₹" + request.getAmount() +
                    ") exceeds current outstanding amount (₹" + invoice.getOutstandingAmount() + ").");
        }

        User currentUser = SecurityUtils.getCurrentUser().orElse(null);
        String paymentNumber = "PAY-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 90000) + 10000);

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .invoice(invoice)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : OffsetDateTime.now())
                .transactionReference(request.getTransactionReference())
                .notes(request.getNotes())
                .status(PaymentStatus.COMPLETED)
                .createdBy(currentUser)
                .build();

        payment = paymentRepository.save(payment);

        // Update Invoice Ledger
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(request.getAmount());
        BigDecimal newOutstanding = invoice.getTotalAmount().subtract(newAmountPaid).max(BigDecimal.ZERO);

        invoice.setAmountPaid(newAmountPaid);
        invoice.setOutstandingAmount(newOutstanding);

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        }

        invoiceRepository.save(invoice);

        // Notify customer
        Notification notification = Notification.builder()
                .customer(invoice.getCustomer())
                .title("Payment Received")
                .message("We have received your payment of ₹" + request.getAmount() +
                        " for Invoice " + invoice.getInvoiceNumber() + " via " + request.getPaymentMethod() + ".")
                .notificationType(NotificationType.PAYMENT_RECEIVED)
                .referenceType("PAYMENT")
                .referenceId(payment.getId())
                .build();
        notificationRepository.save(notification);

        auditLogService.logAction("PAYMENT_RECORDED", "Payment", payment.getId(), null,
                "Amount: ₹" + payment.getAmount() + " Method: " + payment.getPaymentMethod());

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        verifyCustomerAccess(payment.getInvoice().getCustomer().getId());

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoiceId(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        verifyCustomerAccess(invoice.getCustomer().getId());

        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable, Long customerId) {
        Long isolatedCustomerId = resolveCustomerIdForQuery(customerId);

        Page<Payment> page;
        if (isolatedCustomerId != null) {
            page = paymentRepository.findByCustomerId(isolatedCustomerId, pageable);
        } else {
            page = paymentRepository.findAll(pageable);
        }

        return page.map(this::mapToResponse);
    }

    private void verifyCustomerAccess(Long targetCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            Long currentCustId = SecurityUtils.getCurrentCustomerId();
            if (currentCustId == null || !currentCustId.equals(targetCustomerId)) {
                throw new ForbiddenException("Access Denied: You do not have permission to access another customer's payment records.");
            }
        }
    }

    private Long resolveCustomerIdForQuery(Long requestedCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            return SecurityUtils.getCurrentCustomerId();
        }
        return requestedCustomerId;
    }

    private PaymentResponse mapToResponse(Payment p) {
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
                .createdById(p.getCreatedBy() != null ? p.getCreatedBy().getId() : null)
                .createdByName(p.getCreatedBy() != null ? p.getCreatedBy().getFirstName() + " " + p.getCreatedBy().getLastName() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
