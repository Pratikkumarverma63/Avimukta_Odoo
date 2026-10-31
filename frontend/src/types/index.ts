// Brightloop Software Subscription Management System - Type Definitions

export type Role = 'ROLE_ADMIN' | 'ROLE_STAFF' | 'ROLE_CUSTOMER';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  active?: boolean;
  customerId?: number;
  customerCode?: string;
  companyName?: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  customerId?: number;
  customerCode?: string;
  companyName?: string;
}

export interface ForgotPasswordResponse {
  email: string;
  otpCode?: string;
  resetToken?: string;
  expiryMinutes?: number;
  message?: string;
}

export interface UserProfileResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  active: boolean;
  customerId?: number;
  customerCode?: string;
  companyName?: string;
}

export interface Customer {
  id: number;
  customerNumber: string;
  companyName: string;
  billingEmail: string;
  billingPhone?: string;
  phone?: string;
  contactName?: string;
  billingAddress?: string;
  gstNumber?: string;
  panNumber?: string;
  taxId?: string;
  userId?: number;
}

export interface ProductVariant {
  id: number;
  attributeName: string;
  variantValue: string;
  priceAdjustment: number;
  sku: string;
  active: boolean;
}

export interface Product {
  id: number;
  code: string;
  name: string;
  productType: 'SOFTWARE' | 'SERVICE' | 'ADD_ON';
  salesPrice: number;
  costPrice: number;
  description: string;
  active: boolean;
  variants: ProductVariant[];
}

export type BillingFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface Plan {
  id: number;
  productId: number;
  productName: string;
  name: string;
  code: string;
  price: number;
  billingFrequency: BillingFrequency;
  minQuantity: number;
  availabilityStartDate: string;
  availabilityEndDate: string;
  autoCloseOnExpiry: boolean;
  allowPause: boolean;
  allowEarlyClosure: boolean;
  allowRenewal: boolean;
  active: boolean;
  product?: Product;
}

export type QuotationStatus = 'DRAFT' | 'QUOTATION' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED';

export interface QuotationItem {
  id?: number;
  productId: number;
  productName?: string;
  productVariantId?: number;
  variantDescription?: string;
  quantity: number;
  unitPrice: number;
  taxRatePercent: number;
  taxAmount: number;
  discountAmount: number;
  lineTotal: number;
}

export interface Quotation {
  id: number;
  quotationNumber: string;
  customerId: number;
  customerName?: string;
  planId: number;
  planName?: string;
  status: QuotationStatus;
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  validUntil: string;
  notes?: string;
  items: QuotationItem[];
  createdAt: string;
}

export interface QuotationTemplate {
  id: number;
  name: string;
  defaultPlanId: number;
  defaultPlanName?: string;
  validityDays: number;
  active: boolean;
}

export type SubscriptionStatus = 'DRAFT' | 'QUOTATION' | 'CONFIRMED' | 'ACTIVE' | 'PAUSED' | 'CLOSED' | 'RENEWED';

export interface SubscriptionItem {
  id?: number;
  productId: number;
  productName?: string;
  productVariantId?: number;
  variantId?: number;
  variantName?: string;
  variantDescription?: string;
  description?: string;
  quantity: number;
  unitPrice: number;
  taxRatePercent?: number;
  taxAmount?: number;
  discountAmount?: number;
  lineTotal: number;
}

export interface Subscription {
  id: number;
  subscriptionNumber: string;
  customerId: number;
  customerName: string;
  planId: number;
  planName: string;
  planCode?: string;
  billingFrequency?: BillingFrequency;
  startDate: string;
  expiryDate?: string;
  nextBillingDate: string;
  paymentTerms: string;
  status: SubscriptionStatus;
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  pausedAt?: string;
  closedAt?: string;
  items: SubscriptionItem[];
  createdAt: string;
}

export type InvoiceStatus = 'DRAFT' | 'CONFIRMED' | 'PAID' | 'CANCELLED' | 'REFUNDED';

export interface InvoiceItem {
  id: number;
  productId: number;
  productName: string;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRatePercent: number;
  taxAmount: number;
  discountAmount: number;
  lineTotal: number;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  subscriptionId: number;
  subscriptionNumber: string;
  customerId: number;
  customerName: string;
  customerBillingEmail: string;
  invoiceDate: string;
  dueDate: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
  status: InvoiceStatus;
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  amountPaid: number;
  outstandingAmount: number;
  items: InvoiceItem[];
}

export type PaymentMethod = 'UPI' | 'BANK_TRANSFER' | 'CARD' | 'CASH' | 'OTHER';

export interface Payment {
  id: number;
  paymentNumber: string;
  invoiceId: number;
  invoiceNumber: string;
  customerId: number;
  customerName: string;
  amount: number;
  paymentMethod: PaymentMethod;
  transactionReference?: string;
  paymentDate: string;
  notes?: string;
  status?: string;
  createdAt: string;
}

export interface TaxRule {
  id: number;
  name: string;
  code: string;
  ratePercent: number;
  isPercentage: boolean;
  applicability: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  active: boolean;
}

export interface DiscountRule {
  id: number;
  name: string;
  code: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  minPurchaseAmount: number;
  minQuantity: number;
  startDate: string;
  endDate: string;
  maxUses: number;
  usedCount: number;
  scope: 'SUBSCRIPTION_LEVEL' | 'PRODUCT_SPECIFIC';
  active: boolean;
}

export interface DashboardMetrics {
  totalCustomers: number;
  activeSubscriptions: number;
  totalSubscriptions: number;
  monthlyRecurringRevenue: number;
  annualRunRate: number;
  totalInvoicedRevenue: number;
  totalPaymentsReceived: number;
  totalOutstandingAmount: number;
  overdueInvoicesCount: number;
  overdueInvoicesAmount: number;
  upcomingRenewalsCount: number;
  subscriptionsByStatus: Record<string, number>;
  invoicesByStatus: Record<string, number>;
  paymentsByMethod: Record<string, number>;
}

export interface MonthlyTrendPoint {
  month: string;
  invoicedAmount: number;
  collectedAmount: number;
  outstandingAmount: number;
  activeSubscriptionsCount: number;
}

export interface RevenueTrend {
  monthlyPoints: MonthlyTrendPoint[];
}

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}

export interface CustomerDashboard {
  customerCode?: string;
  companyName?: string;
  contactName?: string;
  email?: string;
  phone?: string;
  billingAddress?: string;
  taxId?: string;
  activeSubscriptionsCount?: number;
  totalInvoicedAmount?: number;
  totalAmountPaid?: number;
  totalOutstandingAmount?: number;
  nextUpcomingBillingDate?: string;
  unreadNotificationCount?: number;
  activeSubscriptions: Subscription[];
  recentInvoices: Invoice[];
  recentPayments: Payment[];
  unreadNotifications?: Notification[];
  customer?: Customer;
  totalOutstandingBalance?: number;
  nextRenewalDate?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface DunningCase {
  id: number;
  invoice: Invoice;
  customer: Customer;
  dunningStage: 'HEALTHY' | 'REMINDER_SENT' | 'GRACE_PERIOD' | 'LATE_FEE_APPLIED' | 'SUSPENDED';
  daysOverdue: number;
  lateFeeAmount: number;
  lastActionDate?: string;
  nextActionDate?: string;
  attemptsCount: number;
  notes?: string;
  status?: string;
  createdAt: string;
}

export interface ProrationCalculationRequest {
  newPlanId: number;
  newVariantId?: number;
  effectiveDate: string;
}

export interface ProrationCalculationResponse {
  subscriptionId: number;
  effectiveDate: string;
  daysUsed: number;
  daysRemaining: number;
  daysInCycle: number;
  oldPlanPrice: number;
  newPlanPrice: number;
  proratedCredit: number;
  proratedCharge: number;
  netAdjustment: number;
  summary: string;
}

export interface ProrationRecord {
  id: number;
  subscriptionId: number;
  customerId: number;
  changeType: string;
  effectiveDate: string;
  oldPlanPrice: number;
  newPlanPrice: number;
  daysUsed: number;
  daysInCycle: number;
  proratedCreditAmount: number;
  proratedChargeAmount: number;
  netAdjustmentAmount: number;
  createdAt: string;
}
