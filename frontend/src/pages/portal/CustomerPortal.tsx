import React, { useEffect, useState } from 'react';
import { portalApi, invoiceApi, subscriptionApi, productApi } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { CustomerDashboard, Invoice, Subscription, Plan, Product, ProductVariant, Payment } from '../../types';
import { BrandLogo } from '../../components/common/BrandLogo';
import {
  Building,
  Calendar,
  CreditCard,
  Printer,
  Receipt,
  Repeat,
  Bell,
  CheckCircle2,
  AlertCircle,
  FileText,
  PauseCircle,
  PlayCircle,
  RefreshCw,
  Sparkles,
  Zap,
  ShieldCheck,
  Check,
  ChevronRight,
  Layers,
  User,
  Phone,
  MapPin,
  Lock,
  XCircle,
  Clock,
  ArrowUpRight,
  HelpCircle,
} from 'lucide-react';

export const CustomerPortal: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'overview' | 'invoices' | 'payments' | 'profile'>('overview');
  const [dashboard, setDashboard] = useState<CustomerDashboard | null>(null);
  const [availablePlans, setAvailablePlans] = useState<Plan[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  // Subscription Enrollment & Actions
  const [subscribingPlanId, setSubscribingPlanId] = useState<number | null>(null);
  const [selectedVariants, setSelectedVariants] = useState<Record<number, number>>({});
  const [enrollSuccessMessage, setEnrollSuccessMessage] = useState<string | null>(null);
  const [cancellationSubId, setCancellationSubId] = useState<number | null>(null);
  const [cancellationReason, setCancellationReason] = useState('');
  const [cancellationLoading, setCancellationLoading] = useState(false);

  // Profile State
  const [profileForm, setProfileForm] = useState({
    companyName: '',
    contactName: '',
    phone: '',
    billingAddress: '',
    taxId: '',
  });
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);

  // Password State
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);

  // Printable Invoice
  const [printableHtml, setPrintableHtml] = useState<string | null>(null);

  const loadCustomerData = async () => {
    try {
      const [dashData, plansData, prodData] = await Promise.all([
        portalApi.getDashboard(),
        portalApi.getAvailablePlans().catch(() => [] as Plan[]),
        productApi.getAll({ size: 100 }).then((res) => res.content).catch(() => [] as Product[]),
      ]);
      setDashboard(dashData);
      setAvailablePlans(plansData);
      setProducts(prodData);

      if (dashData) {
        setProfileForm({
          companyName: dashData.companyName || dashData.customer?.companyName || '',
          contactName: dashData.contactName || dashData.customer?.contactName || '',
          phone: dashData.phone || dashData.customer?.phone || '',
          billingAddress: dashData.billingAddress || dashData.customer?.billingAddress || '',
          taxId: dashData.taxId || dashData.customer?.taxId || '',
        });
      }
    } catch (err) {
      console.error('Failed to load customer portal data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCustomerData();
  }, []);

  const handleSubscribe = async (plan: Plan) => {
    const variantId = selectedVariants[plan.id];
    setSubscribingPlanId(plan.id);
    setEnrollSuccessMessage(null);
    try {
      const newSub = await portalApi.subscribe(plan.id, variantId, 1);
      setEnrollSuccessMessage(`Successfully enrolled in ${plan.name} (${newSub.subscriptionNumber})! Immediate invoice created.`);
      await loadCustomerData();
      setTimeout(() => setEnrollSuccessMessage(null), 7000);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to subscribe to plan. Please try again.');
    } finally {
      setSubscribingPlanId(null);
    }
  };

  const handlePause = async (id: number) => {
    if (!confirm('Pause your subscription recurring billing?')) return;
    try {
      await subscriptionApi.pause(id);
      await loadCustomerData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to pause subscription');
    }
  };

  const handleResume = async (id: number) => {
    if (!confirm('Resume subscription billing?')) return;
    try {
      await subscriptionApi.resume(id);
      await loadCustomerData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to resume subscription');
    }
  };

  const handleConfirmCancellation = async () => {
    if (!cancellationSubId) return;
    setCancellationLoading(true);
    try {
      await portalApi.cancelSubscription(cancellationSubId, cancellationReason);
      setCancellationSubId(null);
      setCancellationReason('');
      setEnrollSuccessMessage('Subscription cancellation request processed successfully.');
      await loadCustomerData();
      setTimeout(() => setEnrollSuccessMessage(null), 6000);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to process cancellation request');
    } finally {
      setCancellationLoading(false);
    }
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileSaving(true);
    setProfileSuccess(null);
    try {
      const updated = await portalApi.updateProfile(profileForm);
      setDashboard(updated);
      setProfileSuccess('Company profile and billing details updated successfully!');
      setTimeout(() => setProfileSuccess(null), 5000);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update company profile');
    } finally {
      setProfileSaving(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(null);

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('New password and confirm password do not match.');
      return;
    }

    if (passwordForm.newPassword.length < 8) {
      setPasswordError('Password must be at least 8 characters long.');
      return;
    }

    setPasswordSaving(true);
    try {
      await portalApi.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordSuccess('Password changed successfully! Keep it safe.');
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setTimeout(() => setPasswordSuccess(null), 5000);
    } catch (err: any) {
      setPasswordError(err.response?.data?.message || 'Failed to change password. Verify your current password.');
    } finally {
      setPasswordSaving(false);
    }
  };

  const handleViewInvoice = async (id: number) => {
    try {
      const html = await invoiceApi.getPrintableHtml(id);
      setPrintableHtml(html);
    } catch (err: any) {
      alert('Failed to retrieve printable invoice');
    }
  };

  const handleMarkNotificationRead = async (id: number) => {
    try {
      await portalApi.markNotificationRead(id);
      await loadCustomerData();
    } catch (err) {
      console.error('Failed to mark notification read', err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center space-y-3">
          <RefreshCw className="w-8 h-8 text-sky-600 animate-spin" />
          <span className="text-sm font-medium text-slate-500">Loading your enterprise subscription portal...</span>
        </div>
      </div>
    );
  }

  const companyName = dashboard?.companyName || dashboard?.customer?.companyName || user?.companyName || 'Acme Retail Solutions';
  const customerCode = dashboard?.customerCode || dashboard?.customer?.customerNumber || user?.customerCode || 'CUST-10001';
  const contactEmail = dashboard?.email || dashboard?.customer?.billingEmail || user?.email || 'customer@brightloop.com';
  const outstandingBalance = dashboard?.totalOutstandingAmount ?? dashboard?.totalOutstandingBalance ?? 0;
  const activeSubs = dashboard?.activeSubscriptions || [];
  const invoicesList = dashboard?.recentInvoices || [];
  const paymentsList = dashboard?.recentPayments || [];
  const nextRenewalDate = dashboard?.nextUpcomingBillingDate || dashboard?.nextRenewalDate;

  return (
    <div className="space-y-8 pb-16">
      {/* Welcome Banner: White Background with 1px Blue Border */}
      <div className="bg-white rounded-3xl p-7 text-slate-900 shadow-lg border border-blue-500 relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div className="flex items-center space-x-4">
            <div className="p-2.5 rounded-2xl border border-blue-400 bg-white shadow-sm flex-shrink-0 flex items-center justify-center">
              <BrandLogo size="lg" variant="light" showTagline={false} />
            </div>

            <div>
              <div className="flex items-center space-x-2.5">
                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-300">
                  Brightloop Customer Hub
                </span>
                <span className="flex items-center space-x-1 text-xs text-emerald-700 font-bold">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Verified Corporate Account</span>
                </span>
              </div>
              <h1 className="text-2xl md:text-3xl font-black mt-1 text-slate-950 tracking-tight">
                {companyName}
              </h1>
              <p className="text-xs text-slate-600 mt-1 flex flex-wrap items-center gap-2">
                <span>Account ID:</span>
                <strong className="font-mono bg-blue-50 px-2 py-0.5 rounded-lg text-blue-900 border border-blue-300">
                  {customerCode}
                </strong>
                <span>•</span>
                <span>Billing Email: <strong className="text-slate-800">{contactEmail}</strong></span>
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {nextRenewalDate && (
              <div className="bg-blue-50/60 px-5 py-3.5 rounded-2xl border border-blue-300 text-left shadow-sm">
                <div className="text-[11px] text-blue-800 uppercase font-black tracking-wider flex items-center space-x-1">
                  <Clock className="w-3.5 h-3.5 text-blue-600" />
                  <span>Upcoming Renewal</span>
                </div>
                <div className="text-xl font-black text-slate-900 mt-0.5">{nextRenewalDate}</div>
                <div className="text-[11px] text-emerald-700 font-bold">Auto-renewal active</div>
              </div>
            )}

            <div className="bg-slate-50 px-6 py-3.5 rounded-2xl border border-blue-300 text-right shadow-sm">
              <div className="text-[11px] text-blue-800 uppercase font-black tracking-wider">Outstanding Balance</div>
              <div className={`text-2xl font-black mt-0.5 ${outstandingBalance > 0 ? 'text-amber-700' : 'text-slate-900'}`}>
                ₹{outstandingBalance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
              <div className="text-[11px] text-slate-600 font-medium">
                {activeSubs.length} Active Plan{activeSubs.length === 1 ? '' : 's'}
              </div>
            </div>
          </div>
        </div>

        {/* Tab Navigation Pill Bar */}
        <div className="mt-8 pt-5 border-t border-slate-200 flex flex-wrap items-center gap-2">
          <button
            onClick={() => setActiveTab('overview')}
            className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center space-x-2 border ${
              activeTab === 'overview'
                ? 'bg-blue-600 text-white border-blue-600 shadow-md shadow-blue-500/20'
                : 'bg-slate-50 text-slate-700 hover:text-blue-700 hover:bg-blue-50/50 border-slate-300'
            }`}
          >
            <Repeat className="w-4 h-4" />
            <span>Active Subscriptions ({activeSubs.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('invoices')}
            className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center space-x-2 border ${
              activeTab === 'invoices'
                ? 'bg-blue-600 text-white border-blue-600 shadow-md shadow-blue-500/20'
                : 'bg-slate-50 text-slate-700 hover:text-blue-700 hover:bg-blue-50/50 border-slate-300'
            }`}
          >
            <Receipt className="w-4 h-4" />
            <span>Tax Invoices ({invoicesList.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('payments')}
            className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center space-x-2 border ${
              activeTab === 'payments'
                ? 'bg-blue-600 text-white border-blue-600 shadow-md shadow-blue-500/20'
                : 'bg-slate-50 text-slate-700 hover:text-blue-700 hover:bg-blue-50/50 border-slate-300'
            }`}
          >
            <CreditCard className="w-4 h-4" />
            <span>Payment History ({paymentsList.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('profile')}
            className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center space-x-2 border ${
              activeTab === 'profile'
                ? 'bg-blue-600 text-white border-blue-600 shadow-md shadow-blue-500/20'
                : 'bg-slate-50 text-slate-700 hover:text-blue-700 hover:bg-blue-50/50 border-slate-300'
            }`}
          >
            <Building className="w-4 h-4" />
            <span>Company Profile & Security</span>
          </button>
        </div>
      </div>

      {/* Success Notification Alert */}
      {enrollSuccessMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl flex items-center justify-between text-sm text-emerald-900 shadow-sm animate-fade-in">
          <div className="flex items-center space-x-3">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
            <span className="font-semibold">{enrollSuccessMessage}</span>
          </div>
          <button
            onClick={() => setEnrollSuccessMessage(null)}
            className="text-xs font-bold text-emerald-700 hover:text-emerald-950 uppercase ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Unread Notifications Banner */}
      {dashboard?.unreadNotifications && dashboard.unreadNotifications.length > 0 && (
        <div className="space-y-2">
          {dashboard.unreadNotifications.map((notif) => (
            <div
              key={notif.id}
              className="p-3.5 bg-sky-50 border border-sky-200 rounded-xl flex items-center justify-between text-xs text-sky-900 shadow-sm"
            >
              <div className="flex items-center space-x-2.5">
                <Bell className="w-4 h-4 text-sky-600 flex-shrink-0" />
                <div>
                  <strong className="font-semibold">{notif.title}</strong>: {notif.message}
                </div>
              </div>
              <button
                onClick={() => handleMarkNotificationRead(notif.id)}
                className="text-xs font-semibold text-sky-700 hover:text-sky-900 underline ml-4 flex-shrink-0"
              >
                Dismiss
              </button>
            </div>
          ))}
        </div>
      )}

      {/* TAB 1: OVERVIEW & SUBSCRIPTIONS */}
      {activeTab === 'overview' && (
        <div className="space-y-8 animate-fade-in">
          {/* Active Subscriptions Section */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-800 flex items-center space-x-2">
                  <Repeat className="w-5 h-5 text-sky-600" />
                  <span>My Subscriptions ({activeSubs.length})</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Contracted business tiers, recurring cadences, and automated invoicing
                </p>
              </div>
              <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200">
                ● Live Subscription Engine Active
              </span>
            </div>

            {activeSubs.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                {activeSubs.map((sub) => (
                  <div
                    key={sub.id}
                    className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm hover:shadow-md transition flex flex-col justify-between relative group"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <span className="text-xs font-mono font-bold text-sky-700 bg-sky-50 px-2.5 py-1 rounded-lg border border-sky-100">
                          {sub.subscriptionNumber}
                        </span>
                        <span
                          className={`px-3 py-0.5 rounded-full text-xs font-bold uppercase tracking-wide ${
                            sub.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                              : sub.status === 'PAUSED'
                              ? 'bg-amber-100 text-amber-800 border border-amber-200'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {sub.status}
                        </span>
                      </div>

                      <h3 className="text-lg font-bold text-slate-900">{sub.planName}</h3>
                      <div className="text-3xl font-black text-slate-900 mt-2">
                        ₹{sub.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                        <span className="text-xs font-medium text-slate-500 ml-1.5">
                          / {sub.billingFrequency ? sub.billingFrequency.toLowerCase() : 'billing period'}
                        </span>
                      </div>

                      {/* Line Items Breakdown */}
                      {sub.items && sub.items.length > 0 && (
                        <div className="mt-4 bg-slate-50 p-3 rounded-xl border border-slate-200/80">
                          <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                            Subscription Line Items & Configured Variants:
                          </div>
                          {sub.items.map((item, idx) => (
                            <div key={idx} className="text-xs text-slate-700 flex justify-between items-center py-1 border-b border-slate-200/50 last:border-b-0">
                              <span className="font-semibold text-slate-800">
                                {item.description || item.productName}
                                {item.variantName ? ` (${item.variantName})` : ''}
                              </span>
                              <span className="font-mono text-slate-600 bg-white px-1.5 py-0.5 rounded border border-slate-200">
                                Qty: {item.quantity}
                              </span>
                            </div>
                          ))}
                        </div>
                      )}

                      <div className="mt-4 pt-3 border-t border-slate-100 text-xs space-y-2 text-slate-600">
                        <div className="flex items-center justify-between">
                          <span>Next Recurring Billing Date:</span>
                          <strong className="text-sky-700 font-bold flex items-center space-x-1">
                            <Calendar className="w-3.5 h-3.5" />
                            <span>{sub.nextBillingDate || 'N/A'}</span>
                          </strong>
                        </div>
                        <div className="flex items-center justify-between">
                          <span>Billing Terms:</span>
                          <span className="font-medium text-slate-800">{sub.paymentTerms || 'NET_30'}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span>Subtotal + GST:</span>
                          <span className="font-mono text-slate-700">₹{sub.subtotal} + ₹{sub.taxAmount} GST</span>
                        </div>
                      </div>
                    </div>

                    <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                      <button
                        onClick={() => setCancellationSubId(sub.id)}
                        className="text-xs font-semibold text-rose-600 hover:text-rose-800 underline transition"
                      >
                        Request Cancellation
                      </button>

                      <div className="flex items-center space-x-2">
                        {sub.status === 'ACTIVE' ? (
                          <button
                            onClick={() => handlePause(sub.id)}
                            className="px-3.5 py-1.5 bg-amber-50 hover:bg-amber-100 text-amber-700 rounded-xl text-xs font-bold border border-amber-200 flex items-center space-x-1.5 transition shadow-sm"
                          >
                            <PauseCircle className="w-3.5 h-3.5" />
                            <span>Pause Plan</span>
                          </button>
                        ) : (
                          <button
                            onClick={() => handleResume(sub.id)}
                            className="px-3.5 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-xl text-xs font-bold border border-emerald-200 flex items-center space-x-1.5 transition shadow-sm"
                          >
                            <PlayCircle className="w-3.5 h-3.5" />
                            <span>Resume Plan</span>
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-8 bg-white rounded-2xl border border-slate-200 text-center text-slate-500 shadow-sm">
                <Layers className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <p className="font-semibold text-slate-800">No active subscriptions currently linked.</p>
                <p className="text-xs text-slate-500 mt-1">
                  Choose a subscription plan below to activate your software tier immediately.
                </p>
              </div>
            )}
          </div>

          {/* Available Subscription Plans & Instant Subscribe Catalog */}
          <div className="space-y-4 pt-6 border-t border-slate-200">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-800 flex items-center space-x-2">
                  <Zap className="w-5 h-5 text-amber-500" />
                  <span>Available Subscription Plans & Self-Service Enrollment</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Subscribe to additional modules or change software tiers. Instant automated prorated tax invoice generation.
                </p>
              </div>
              <span className="px-3 py-1 bg-amber-50 text-amber-700 border border-amber-200 rounded-full text-xs font-bold">
                {availablePlans.length} Plans Available
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {availablePlans.map((plan) => {
                const isSubscribing = subscribingPlanId === plan.id;
                const product = products.find((p) => p.id === plan.productId);
                const variants = product?.variants || [];
                const hasVariants = variants.length > 0;
                const selectedVariantId = selectedVariants[plan.id];
                const selectedVariant = variants.find((v: ProductVariant) => v.id === selectedVariantId);
                const variantExtra = selectedVariant ? selectedVariant.priceAdjustment : 0;
                const effectivePrice = plan.price + variantExtra;

                return (
                  <div
                    key={plan.id}
                    className="bg-white rounded-2xl border border-slate-200 hover:border-sky-300 p-6 shadow-sm hover:shadow-md transition flex flex-col justify-between relative group"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <span
                          className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider ${
                            plan.billingFrequency === 'YEARLY'
                              ? 'bg-purple-100 text-purple-700'
                              : plan.billingFrequency === 'MONTHLY'
                              ? 'bg-sky-100 text-sky-700'
                              : 'bg-emerald-100 text-emerald-700'
                          }`}
                        >
                          {plan.billingFrequency}
                        </span>
                        <span className="text-[11px] font-mono text-slate-400 font-medium">{plan.code}</span>
                      </div>

                      <h3 className="text-base font-bold text-slate-900 group-hover:text-sky-600 transition">
                        {plan.name}
                      </h3>
                      <p className="text-xs text-slate-500 mt-1 line-clamp-2">
                        {product?.name || plan.productName || 'Brightloop Enterprise Cloud'}
                      </p>

                      <div className="mt-4 pt-3 border-t border-slate-100 flex items-baseline space-x-1.5">
                        <span className="text-2xl font-black text-slate-900">
                          ₹{effectivePrice.toLocaleString()}
                        </span>
                        <span className="text-xs text-slate-500 font-medium">
                          / {plan.billingFrequency.toLowerCase()}
                        </span>
                        {variantExtra > 0 && (
                          <span className="text-[11px] text-emerald-600 font-semibold ml-1">
                            (+₹{variantExtra} variant)
                          </span>
                        )}
                      </div>

                      {/* Configurable Product Variant Selector */}
                      {hasVariants && (
                        <div className="mt-4 p-3 bg-slate-50 rounded-xl border border-slate-200/80">
                          <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                            Choose Edition / Variant:
                          </label>
                          <select
                            value={selectedVariantId || ''}
                            onChange={(e) => {
                              const val = e.target.value ? Number(e.target.value) : undefined;
                              setSelectedVariants((prev) => ({
                                ...prev,
                                [plan.id]: val as any,
                              }));
                            }}
                            className="w-full text-xs bg-white border border-slate-300 rounded-lg px-2.5 py-1.5 font-medium text-slate-800 focus:ring-1 focus:ring-sky-500"
                          >
                            <option value="">Standard Edition (No Variant Surcharge)</option>
                            {variants.map((v: ProductVariant) => (
                              <option key={v.id} value={v.id}>
                                {v.attributeName}: {v.variantValue} (+₹{v.priceAdjustment})
                              </option>
                            ))}
                          </select>
                        </div>
                      )}

                      {/* Plan Features Checklist */}
                      <ul className="mt-4 space-y-2 text-xs text-slate-600">
                        <li className="flex items-center space-x-2">
                          <Check className="w-3.5 h-3.5 text-emerald-500 flex-shrink-0" />
                          <span>{plan.allowPause ? 'Self-service pause & resume anytime' : 'Fixed commitment term'}</span>
                        </li>
                        <li className="flex items-center space-x-2">
                          <Check className="w-3.5 h-3.5 text-emerald-500 flex-shrink-0" />
                          <span>{plan.allowRenewal ? 'Automatic recurring renewal enabled' : 'Single billing cycle'}</span>
                        </li>
                        <li className="flex items-center space-x-2">
                          <Check className="w-3.5 h-3.5 text-emerald-500 flex-shrink-0" />
                          <span>Automated 18% GST tax invoice breakdown</span>
                        </li>
                        <li className="flex items-center space-x-2">
                          <Check className="w-3.5 h-3.5 text-emerald-500 flex-shrink-0" />
                          <span>NET-30 credit terms supported</span>
                        </li>
                      </ul>
                    </div>

                    <div className="mt-6 pt-4 border-t border-slate-100">
                      <button
                        onClick={() => handleSubscribe(plan)}
                        disabled={isSubscribing}
                        className="w-full py-2.5 px-4 bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white rounded-xl text-xs font-bold shadow-md hover:shadow-lg transition flex items-center justify-center space-x-2 disabled:opacity-60"
                      >
                        {isSubscribing ? (
                          <>
                            <RefreshCw className="w-4 h-4 animate-spin" />
                            <span>Enrolling Account...</span>
                          </>
                        ) : (
                          <>
                            <Sparkles className="w-4 h-4" />
                            <span>Subscribe Now</span>
                          </>
                        )}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: INVOICES & STATEMENTS */}
      {activeTab === 'invoices' && (
        <div className="space-y-4 animate-fade-in">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-slate-800 flex items-center space-x-2">
                <Receipt className="w-5 h-5 text-indigo-600" />
                <span>Invoices & Billing Statements ({invoicesList.length})</span>
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Official GST-compliant tax invoices with breakdown of subtotal, tax rules, and payments
              </p>
            </div>
            <span className="text-xs text-slate-500">
              Outstanding Amount: <strong className="text-amber-600 font-mono">₹{outstandingBalance.toLocaleString()}</strong>
            </span>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                    <th className="py-3 px-4">Invoice #</th>
                    <th className="py-3 px-4">Invoice Date</th>
                    <th className="py-3 px-4">Due Date</th>
                    <th className="py-3 px-4">Total Amount</th>
                    <th className="py-3 px-4">Paid</th>
                    <th className="py-3 px-4">Outstanding</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {invoicesList.length > 0 ? (
                    invoicesList.map((inv) => (
                      <tr key={inv.id} className="hover:bg-slate-50/70 transition">
                        <td className="py-3.5 px-4 font-mono font-bold text-sky-700">{inv.invoiceNumber}</td>
                        <td className="py-3.5 px-4 text-slate-600">{inv.invoiceDate}</td>
                        <td className="py-3.5 px-4 text-slate-600">{inv.dueDate}</td>
                        <td className="py-3.5 px-4 font-bold text-slate-900">₹{inv.totalAmount.toLocaleString()}</td>
                        <td className="py-3.5 px-4 text-emerald-700 font-semibold">
                          ₹{inv.amountPaid.toLocaleString()}
                        </td>
                        <td className="py-3.5 px-4 font-bold text-amber-600">
                          ₹{inv.outstandingAmount.toLocaleString()}
                        </td>
                        <td className="py-3.5 px-4">
                          <span
                            className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                              inv.status === 'PAID'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {inv.status}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 text-right">
                          <button
                            onClick={() => handleViewInvoice(inv.id)}
                            className="px-3 py-1.5 text-xs font-bold text-sky-700 hover:text-sky-800 hover:bg-sky-50 rounded-lg border border-sky-200 transition inline-flex items-center space-x-1.5 shadow-sm"
                          >
                            <Printer className="w-3.5 h-3.5 text-sky-600" />
                            <span>View / Print PDF</span>
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={8} className="py-8 text-center text-slate-400 text-xs">
                        No invoices generated yet for this account.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: PAYMENT HISTORY */}
      {activeTab === 'payments' && (
        <div className="space-y-4 animate-fade-in">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-slate-800 flex items-center space-x-2">
                <CreditCard className="w-5 h-5 text-emerald-600" />
                <span>Payment History & Receipts ({paymentsList.length})</span>
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Immutable financial reconciliation ledger of all cash settlements, UPI, cards, and bank transfers
              </p>
            </div>
            <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200">
              Total Paid: ₹{dashboard?.totalAmountPaid?.toLocaleString() || 0}
            </span>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                    <th className="py-3 px-4">Receipt #</th>
                    <th className="py-3 px-4">Invoice Ref</th>
                    <th className="py-3 px-4">Payment Method</th>
                    <th className="py-3 px-4">Transaction / UTR Ref</th>
                    <th className="py-3 px-4">Amount Paid</th>
                    <th className="py-3 px-4">Settlement Date</th>
                    <th className="py-3 px-4">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {paymentsList.length > 0 ? (
                    paymentsList.map((pay) => (
                      <tr key={pay.id} className="hover:bg-slate-50/70 transition">
                        <td className="py-3.5 px-4 font-mono font-bold text-emerald-700">{pay.paymentNumber}</td>
                        <td className="py-3.5 px-4 font-mono text-sky-700">{pay.invoiceNumber}</td>
                        <td className="py-3.5 px-4">
                          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200">
                            {pay.paymentMethod}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 font-mono text-xs text-slate-600">
                          {pay.transactionReference || 'N/A'}
                        </td>
                        <td className="py-3.5 px-4 font-extrabold text-slate-900">
                          ₹{pay.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                        </td>
                        <td className="py-3.5 px-4 text-slate-600 text-xs">
                          {pay.paymentDate ? pay.paymentDate.substring(0, 10) : 'Recent'}
                        </td>
                        <td className="py-3.5 px-4">
                          <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                            {pay.status || 'COMPLETED'}
                          </span>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={7} className="py-8 text-center text-slate-400 text-xs">
                        No payment records found yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: COMPANY PROFILE & SECURITY */}
      {activeTab === 'profile' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 animate-fade-in">
          {/* Company Profile Details */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
            <h3 className="text-lg font-bold text-slate-800 flex items-center space-x-2 mb-2">
              <Building className="w-5 h-5 text-sky-600" />
              <span>Company Profile & Billing Information</span>
            </h3>
            <p className="text-xs text-slate-500 mb-6">
              Update your corporate identity, billing address and tax compliance details
            </p>

            {profileSuccess && (
              <div className="mb-4 p-3 bg-emerald-50 border border-emerald-300 rounded-xl text-xs text-emerald-800 flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span>{profileSuccess}</span>
              </div>
            )}

            <form onSubmit={handleSaveProfile} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Company Legal Name</label>
                <input
                  type="text"
                  required
                  value={profileForm.companyName}
                  onChange={(e) => setProfileForm({ ...profileForm, companyName: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Contact Person Name</label>
                <input
                  type="text"
                  required
                  value={profileForm.contactName}
                  onChange={(e) => setProfileForm({ ...profileForm, contactName: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Phone Number</label>
                  <input
                    type="text"
                    value={profileForm.phone}
                    onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                    className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-sky-500 font-medium"
                    placeholder="+91-9876543210"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">GSTIN / Tax ID</label>
                  <input
                    type="text"
                    value={profileForm.taxId}
                    onChange={(e) => setProfileForm({ ...profileForm, taxId: e.target.value })}
                    className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-sky-500 font-medium uppercase"
                    placeholder="29AABCU9603R1Z7"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Registered Billing Address</label>
                <textarea
                  rows={3}
                  value={profileForm.billingAddress}
                  onChange={(e) => setProfileForm({ ...profileForm, billingAddress: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-sky-500 font-medium"
                  placeholder="Street, City, State, PIN"
                />
              </div>

              <button
                type="submit"
                disabled={profileSaving}
                className="w-full py-2.5 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold shadow-md transition disabled:opacity-60"
              >
                {profileSaving ? 'Saving Changes...' : 'Update Corporate Profile'}
              </button>
            </form>
          </div>

          {/* Security & Password Change */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
            <h3 className="text-lg font-bold text-slate-800 flex items-center space-x-2 mb-2">
              <Lock className="w-5 h-5 text-indigo-600" />
              <span>Security & Password Management</span>
            </h3>
            <p className="text-xs text-slate-500 mb-6">
              Protect your Brightloop account with a strong password (minimum 8 chars with uppercase, lowercase & special character)
            </p>

            {passwordSuccess && (
              <div className="mb-4 p-3 bg-emerald-50 border border-emerald-300 rounded-xl text-xs text-emerald-800 flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span>{passwordSuccess}</span>
              </div>
            )}

            {passwordError && (
              <div className="mb-4 p-3 bg-rose-50 border border-rose-300 rounded-xl text-xs text-rose-800 flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
                <span>{passwordError}</span>
              </div>
            )}

            <form onSubmit={handleChangePassword} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Current Password</label>
                <input
                  type="password"
                  required
                  value={passwordForm.currentPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-indigo-500"
                  placeholder="••••••••••••"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">New Password</label>
                <input
                  type="password"
                  required
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-indigo-500"
                  placeholder="Min 8 chars, 1 uppercase, 1 symbol"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Confirm New Password</label>
                <input
                  type="password"
                  required
                  value={passwordForm.confirmPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                  className="w-full text-sm border border-slate-300 rounded-xl px-3.5 py-2.5 focus:ring-2 focus:ring-indigo-500"
                  placeholder="••••••••••••"
                />
              </div>

              <button
                type="submit"
                disabled={passwordSaving}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold shadow-md transition disabled:opacity-60"
              >
                {passwordSaving ? 'Updating Password...' : 'Change Password'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Request Cancellation */}
      {cancellationSubId && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 animate-scale-up">
            <h3 className="text-base font-bold text-slate-900 flex items-center space-x-2">
              <AlertCircle className="w-5 h-5 text-rose-600" />
              <span>Confirm Subscription Cancellation</span>
            </h3>
            <p className="text-xs text-slate-500 mt-2">
              Are you sure you want to request cancellation for this subscription? Recurring automated billing will be terminated according to plan terms.
            </p>

            <div className="mt-4">
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Reason for Cancellation</label>
              <textarea
                rows={3}
                value={cancellationReason}
                onChange={(e) => setCancellationReason(e.target.value)}
                className="w-full text-xs border border-slate-300 rounded-xl p-2.5 focus:ring-2 focus:ring-rose-500"
                placeholder="e.g. Scaling down team, project completed, switching tier..."
              />
            </div>

            <div className="mt-6 flex items-center justify-end space-x-3">
              <button
                onClick={() => setCancellationSubId(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl"
              >
                Keep Active
              </button>
              <button
                onClick={handleConfirmCancellation}
                disabled={cancellationLoading}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold rounded-xl shadow-md transition disabled:opacity-60"
              >
                {cancellationLoading ? 'Cancelling...' : 'Confirm Cancellation'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Printable Invoice Preview */}
      {printableHtml && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full max-h-[90vh] flex flex-col overflow-hidden">
            <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
              <h2 className="text-base font-bold text-slate-800 flex items-center space-x-2">
                <FileText className="w-5 h-5 text-sky-600" />
                <span>Customer Tax Invoice Document</span>
              </h2>
              <div className="flex items-center space-x-3">
                <button
                  onClick={() => {
                    const printWindow = window.open('', '_blank');
                    if (printWindow) {
                      printWindow.document.write(printableHtml);
                      printWindow.document.close();
                      printWindow.print();
                    }
                  }}
                  className="px-3 py-1.5 bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold rounded-lg flex items-center space-x-1.5 shadow-sm"
                >
                  <Printer className="w-3.5 h-3.5" />
                  <span>Print Document</span>
                </button>
                <button
                  onClick={() => setPrintableHtml(null)}
                  className="px-3 py-1.5 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-semibold rounded-lg"
                >
                  Close
                </button>
              </div>
            </div>
            <div className="flex-1 overflow-auto p-6 bg-slate-100">
              <div
                className="bg-white shadow-md p-8 rounded-lg max-w-3xl mx-auto"
                dangerouslySetInnerHTML={{ __html: printableHtml }}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
