package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.Invoice;
import com.brightloop.subscription.domain.entity.InvoiceItem;
import com.brightloop.subscription.domain.enums.InvoiceStatus;
import com.brightloop.subscription.domain.enums.Role;
import com.brightloop.subscription.repository.InvoiceRepository;
import com.brightloop.subscription.security.SecurityUtils;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.InvoiceService;
import com.brightloop.subscription.web.dto.InvoiceItemResponse;
import com.brightloop.subscription.web.dto.InvoiceResponse;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ForbiddenException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final AuditLogService auditLogService;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        verifyCustomerAccess(invoice.getCustomer().getId());

        return mapToInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable, InvoiceStatus status, Long customerId, Boolean overdueOnly) {
        Long isolatedCustomerId = resolveCustomerIdForQuery(customerId);

        Page<Invoice> page;
        if (isolatedCustomerId != null && status != null) {
            page = invoiceRepository.findByCustomerIdAndStatus(isolatedCustomerId, status, pageable);
        } else if (isolatedCustomerId != null) {
            page = invoiceRepository.findByCustomerId(isolatedCustomerId, pageable);
        } else if (status != null) {
            page = invoiceRepository.findByStatus(status, pageable);
        } else {
            page = invoiceRepository.findAll(pageable);
        }

        return page.map(this::mapToInvoiceResponse);
    }

    @Override
    public InvoiceResponse confirmInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT invoices can be confirmed. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.CONFIRMED);
        invoice = invoiceRepository.save(invoice);
        auditLogService.logAction("INVOICE_CONFIRMED", "Invoice", invoice.getId(), "DRAFT", "CONFIRMED");

        return mapToInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse cancelInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Cannot cancel an already PAID invoice. Please issue a refund/credit instead.");
        }

        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setOutstandingAmount(BigDecimal.ZERO);

        invoice = invoiceRepository.save(invoice);
        auditLogService.logAction("INVOICE_CANCELLED", "Invoice", invoice.getId(), oldStatus.name(), "CANCELLED");

        return mapToInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public String generatePrintableInvoiceHtml(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        verifyCustomerAccess(invoice.getCustomer().getId());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Invoice ")
            .append(invoice.getInvoiceNumber()).append("</title>")
            .append("<style>")
            .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; line-height: 1.5; padding: 40px; margin: 0; }")
            .append(".invoice-box { max-width: 800px; margin: auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }")
            .append(".header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #3b82f6; padding-bottom: 20px; margin-bottom: 24px; }")
            .append(".company h1 { margin: 0; color: #1e40af; font-size: 26px; }")
            .append(".company p { margin: 2px 0; color: #64748b; font-size: 14px; }")
            .append(".inv-title { text-align: right; }")
            .append(".inv-title h2 { margin: 0; font-size: 22px; color: #0f172a; }")
            .append(".details { display: flex; justify-content: space-between; margin-bottom: 24px; font-size: 14px; }")
            .append(".details-col { width: 48%; }")
            .append("table { width: 100%; border-collapse: collapse; margin-bottom: 24px; font-size: 14px; }")
            .append("th { background-color: #f8fafc; color: #475569; text-align: left; padding: 10px 12px; border-bottom: 1px solid #cbd5e1; }")
            .append("td { padding: 10px 12px; border-bottom: 1px solid #f1f5f9; }")
            .append(".text-right { text-align: right; }")
            .append(".totals { width: 300px; margin-left: auto; font-size: 14px; }")
            .append(".totals-row { display: flex; justify-content: space-between; padding: 6px 0; }")
            .append(".totals-row.grand-total { border-top: 2px solid #0f172a; font-weight: bold; font-size: 16px; color: #1e40af; padding-top: 10px; }")
            .append(".badge { display: inline-block; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; text-transform: uppercase; }")
            .append(".badge-paid { background-color: #dcfce7; color: #15803d; }")
            .append(".badge-confirmed { background-color: #dbeafe; color: #1d4ed8; }")
            .append(".badge-draft { background-color: #f1f5f9; color: #475569; }")
            .append("</style></head><body>")
            .append("<div class='invoice-box'>")
            .append("<div class='header'>")
            .append("<div class='company'><h1>Brightloop Software</h1><p>Modern Business Subscription Platforms</p><p>support@brightloop.com | GSTIN29AAAAA0000A1Z5</p></div>")
            .append("<div class='inv-title'><h2>INVOICE</h2><p><strong>#").append(invoice.getInvoiceNumber()).append("</strong></p>")
            .append("<span class='badge badge-").append(invoice.getStatus().name().toLowerCase()).append("'>").append(invoice.getStatus()).append("</span></div>")
            .append("</div>")
            .append("<div class='details'>")
            .append("<div class='details-col'><strong>Billed To:</strong><br>")
            .append(invoice.getCustomer().getCompanyName()).append("<br>")
            .append(invoice.getCustomer().getContactName()).append("<br>")
            .append(invoice.getCustomer().getEmail()).append("<br>")
            .append(invoice.getCustomer().getBillingAddress() != null ? invoice.getCustomer().getBillingAddress() : "").append("<br>")
            .append(invoice.getCustomer().getTaxId() != null ? "Tax ID: " + invoice.getCustomer().getTaxId() : "")
            .append("</div>")
            .append("<div class='details-col' style='text-align: right;'>")
            .append("<strong>Invoice Date:</strong> ").append(invoice.getInvoiceDate()).append("<br>")
            .append("<strong>Due Date:</strong> ").append(invoice.getDueDate()).append("<br>")
            .append(invoice.getSubscription() != null ? "<strong>Subscription:</strong> " + invoice.getSubscription().getSubscriptionNumber() + "<br>" : "")
            .append(invoice.getBillingPeriodStart() != null ? "<strong>Period:</strong> " + invoice.getBillingPeriodStart() + " to " + invoice.getBillingPeriodEnd() : "")
            .append("</div></div>")
            .append("<table><thead><tr><th>Description</th><th>Qty</th><th class='text-right'>Unit Price</th><th class='text-right'>Tax</th><th class='text-right'>Total</th></tr></thead><tbody>");

        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            for (InvoiceItem item : invoice.getItems()) {
                String desc = item.getDescription() != null ? item.getDescription() : "Subscription License";
                html.append("<tr>")
                    .append("<td>").append(desc).append("</td>")
                    .append("<td>").append(item.getQuantity() != null ? item.getQuantity() : 1).append("</td>")
                    .append("<td class='text-right'>₹").append(item.getUnitPrice()).append("</td>")
                    .append("<td class='text-right'>₹").append(item.getTaxAmount()).append("</td>")
                    .append("<td class='text-right'>₹").append(item.getLineTotal()).append("</td>")
                    .append("</tr>");
            }
        } else {
            String planDesc = invoice.getSubscription() != null && invoice.getSubscription().getPlan() != null
                    ? invoice.getSubscription().getPlan().getName() + " Subscription"
                    : "Brightloop Cloud Subscription Service";
            html.append("<tr>")
                .append("<td>").append(planDesc).append("</td>")
                .append("<td>1</td>")
                .append("<td class='text-right'>₹").append(invoice.getSubtotal()).append("</td>")
                .append("<td class='text-right'>₹").append(invoice.getTaxAmount()).append("</td>")
                .append("<td class='text-right'>₹").append(invoice.getTotalAmount()).append("</td>")
                .append("</tr>");
        }

        html.append("</tbody></table>")
            .append("<div class='totals'>")
            .append("<div class='totals-row'><span>Subtotal:</span><span>₹").append(invoice.getSubtotal()).append("</span></div>")
            .append("<div class='totals-row'><span>Discount:</span><span>-₹").append(invoice.getDiscountAmount()).append("</span></div>")
            .append("<div class='totals-row'><span>Tax:</span><span>₹").append(invoice.getTaxAmount()).append("</span></div>")
            .append("<div class='totals-row grand-total'><span>Total Amount:</span><span>₹").append(invoice.getTotalAmount()).append("</span></div>")
            .append("<div class='totals-row'><span>Amount Paid:</span><span>₹").append(invoice.getAmountPaid()).append("</span></div>")
            .append("<div class='totals-row' style='font-weight: bold;'><span>Outstanding:</span><span>₹").append(invoice.getOutstandingAmount()).append("</span></div>")
            .append("</div>")
            .append("<div style='margin-top: 40px; padding-top: 16px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; text-align: center;'>")
            .append("Thank you for partnering with Brightloop Software. Payment terms: ").append(invoice.getCustomer().getCustomerCode())
            .append("</div></div></body></html>");

        return html.toString();
    }

    private void verifyCustomerAccess(Long targetCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            Long currentCustId = SecurityUtils.getCurrentCustomerId();
            if (currentCustId == null || !currentCustId.equals(targetCustomerId)) {
                throw new ForbiddenException("Access Denied: You do not have permission to access another customer's invoice.");
            }
        }
    }

    private Long resolveCustomerIdForQuery(Long requestedCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            return SecurityUtils.getCurrentCustomerId();
        }
        return requestedCustomerId;
    }

    public InvoiceResponse mapToInvoiceResponse(Invoice inv) {
        boolean isOverdue = inv.getStatus() != InvoiceStatus.PAID &&
                inv.getStatus() != InvoiceStatus.CANCELLED &&
                inv.getDueDate().isBefore(LocalDate.now());

        List<InvoiceItemResponse> itemResponses = inv.getItems().stream()
                .map(i -> InvoiceItemResponse.builder()
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

        return InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .subscriptionId(inv.getSubscription() != null ? inv.getSubscription().getId() : null)
                .subscriptionNumber(inv.getSubscription() != null ? inv.getSubscription().getSubscriptionNumber() : null)
                .customerId(inv.getCustomer().getId())
                .customerCode(inv.getCustomer().getCustomerCode())
                .customerCompanyName(inv.getCustomer().getCompanyName())
                .customerContactName(inv.getCustomer().getContactName())
                .customerEmail(inv.getCustomer().getEmail())
                .customerPhone(inv.getCustomer().getPhone())
                .customerBillingAddress(inv.getCustomer().getBillingAddress())
                .customerTaxId(inv.getCustomer().getTaxId())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .status(inv.getStatus())
                .overdue(isOverdue)
                .subtotal(inv.getSubtotal())
                .taxAmount(inv.getTaxAmount())
                .discountAmount(inv.getDiscountAmount())
                .totalAmount(inv.getTotalAmount())
                .amountPaid(inv.getAmountPaid())
                .outstandingAmount(inv.getOutstandingAmount())
                .billingPeriodStart(inv.getBillingPeriodStart())
                .billingPeriodEnd(inv.getBillingPeriodEnd())
                .notes(inv.getNotes())
                .items(itemResponses)
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }
}
