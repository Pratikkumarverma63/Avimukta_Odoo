import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { invoiceApi, quotationApi, subscriptionApi, paymentApi } from '../../services/api';
import { Invoice, Quotation, Subscription, PaymentMethod } from '../../types';
import {
  FileSpreadsheet,
  Receipt,
  Repeat,
  CreditCard,
  AlertCircle,
  CheckCircle2,
  Clock,
  ArrowRight,
  PlusCircle,
  Calendar,
  Building,
  RefreshCw,
  Search,
  UserCheck,
  ShieldCheck,
} from 'lucide-react';

export const StaffDashboard: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);

  // Quick Record Payment Modal
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [paymentAmount, setPaymentAmount] = useState<number>(0);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('UPI');
  const [transactionRef, setTransactionRef] = useState('');
  const [paymentNotes, setPaymentNotes] = useState('');
  const [submittingPayment, setSubmittingPayment] = useState(false);

  const loadStaffData = async () => {
    setLoading(true);
    try {
      const [invRes, quoRes, subRes] = await Promise.all([
        invoiceApi.getAll({ page: 0, size: 20 }),
        quotationApi.getAll({ page: 0, size: 20 }),
        subscriptionApi.getAll({ page: 0, size: 20 }),
      ]);
      setInvoices(invRes.content || []);
      setQuotations(quoRes.content || []);
      setSubscriptions(subRes.content || []);
    } catch (err) {
      console.error('Failed to load staff operational data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStaffData();
  }, []);

  const pendingInvoices = invoices.filter((i) => i.status === 'CONFIRMED' || i.outstandingAmount > 0);
  const pendingQuotations = quotations.filter((q) => q.status === 'DRAFT' || q.status === 'QUOTATION');
  const activeSubs = subscriptions.filter((s) => s.status === 'ACTIVE');

  const totalOutstanding = pendingInvoices.reduce((sum, inv) => sum + Number(inv.outstandingAmount || 0), 0);

  const handleOpenPayment = (inv: Invoice) => {
    setSelectedInvoice(inv);
    setPaymentAmount(inv.outstandingAmount);
    setTransactionRef(`UPI/${Math.floor(100000000000 + Math.random() * 900000000000)}`);
    setPaymentNotes(`Payment collected by staff (${user?.firstName || 'Staff'})`);
  };

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInvoice) return;
    setSubmittingPayment(true);
    try {
      await paymentApi.recordPayment({
        invoiceId: selectedInvoice.id,
        amount: Number(paymentAmount),
        paymentMethod,
        transactionReference: transactionRef,
        notes: paymentNotes,
      });
      alert(`Payment of ₹${paymentAmount} successfully reconciled for invoice ${selectedInvoice.invoiceNumber}`);
      setSelectedInvoice(null);
      await loadStaffData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to record payment');
    } finally {
      setSubmittingPayment(false);
    }
  };

  const handleConfirmQuote = async (id: number) => {
    if (!confirm('Confirm this quotation and automatically create the active customer subscription?')) {
      return;
    }
    try {
      await quotationApi.confirm(id);
      alert('Quotation successfully converted to active subscription!');
      await loadStaffData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to confirm quotation');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center space-y-3">
          <RefreshCw className="w-8 h-8 text-sky-600 animate-spin" />
          <span className="text-sm font-medium text-slate-500">Loading Staff Operations Hub...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-sky-700 via-indigo-800 to-slate-900 rounded-2xl p-6 text-white shadow-xl relative overflow-hidden">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-sky-400/20 text-sky-200 text-xs font-semibold uppercase tracking-wider border border-sky-400/30 mb-2">
              <UserCheck className="w-3.5 h-3.5 text-sky-300" />
              <span>Staff Customer Operations Workspace</span>
            </div>
            <h1 className="text-2xl font-black tracking-tight text-white">
              Welcome, {user?.firstName} {user?.lastName}
            </h1>
            <p className="text-xs text-sky-100 mt-1">
              Brightloop Customer Service & Sales Desk • Daily Operations, Quotation Conversions & Payment Reconciliation
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={loadStaffData}
              className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/20 rounded-xl text-white transition shadow-sm"
              title="Refresh Queue"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
            <button
              onClick={() => navigate('/admin/quotations')}
              className="px-4 py-2.5 bg-white text-slate-900 hover:bg-slate-100 rounded-xl text-xs font-bold transition shadow-md flex items-center space-x-2"
            >
              <PlusCircle className="w-4 h-4 text-sky-600" />
              <span>Create Quotation</span>
            </button>
          </div>
        </div>
      </div>

      {/* Operational KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Open Quotations</span>
            <span className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <FileSpreadsheet className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-black text-slate-900 mt-2">{pendingQuotations.length} Quotes</div>
          <div className="text-[11px] text-indigo-600 font-medium mt-1">Awaiting customer conversion</div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Subscriptions</span>
            <span className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Repeat className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-black text-slate-900 mt-2">{activeSubs.length} Active</div>
          <div className="text-[11px] text-emerald-600 font-medium mt-1">Live customer recurring contracts</div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Invoices To Collect</span>
            <span className="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
              <Receipt className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-black text-slate-900 mt-2">{pendingInvoices.length} Invoices</div>
          <div className="text-[11px] text-rose-600 font-medium mt-1">₹{totalOutstanding.toLocaleString()} balance due</div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Payment Ledger</span>
            <span className="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
              <CreditCard className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-black text-slate-900 mt-2">Multi-Channel</div>
          <div className="text-[11px] text-sky-600 font-medium mt-1">UPI, NEFT, RTGS, Cards</div>
        </div>
      </div>

      {/* Two Action Queues: Invoices Requiring Collection & Quotes Requiring Conversion */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Queue 1: Invoices Pending Collection */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="p-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900 flex items-center space-x-2">
                <Receipt className="w-4 h-4 text-rose-600" />
                <span>Customer Invoices Awaiting Collection</span>
              </h2>
              <p className="text-[11px] text-slate-500">Collect pending dues & record receipts</p>
            </div>
            <button
              onClick={() => navigate('/admin/invoices')}
              className="text-xs font-semibold text-sky-600 hover:text-sky-700 flex items-center"
            >
              <span>View All</span>
              <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </button>
          </div>

          <div className="divide-y divide-slate-100 flex-1 overflow-auto max-h-96">
            {pendingInvoices.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">All customer invoices are fully settled!</div>
            ) : (
              pendingInvoices.map((inv) => (
                <div key={inv.id} className="p-4 hover:bg-slate-50/80 transition flex items-center justify-between gap-3 text-xs">
                  <div>
                    <div className="font-mono font-bold text-slate-900">{inv.invoiceNumber}</div>
                    <div className="text-slate-600 font-medium mt-0.5">{inv.customerName}</div>
                    <div className="text-[11px] text-slate-400 mt-0.5 flex items-center gap-1.5">
                      <Clock className="w-3 h-3 text-amber-500" />
                      <span>Due: {inv.dueDate}</span>
                    </div>
                  </div>

                  <div className="text-right flex flex-col items-end">
                    <div className="font-black text-rose-600 text-sm">₹{inv.outstandingAmount.toLocaleString()}</div>
                    <div className="text-[10px] text-slate-400">Total: ₹{inv.totalAmount.toLocaleString()}</div>
                    <button
                      onClick={() => handleOpenPayment(inv)}
                      className="mt-2 px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-lg text-[11px] transition shadow-sm"
                    >
                      Record Payment
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Queue 2: Quotations Awaiting Conversion */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="p-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900 flex items-center space-x-2">
                <FileSpreadsheet className="w-4 h-4 text-indigo-600" />
                <span>Sales Quotes Pending Customer Conversion</span>
              </h2>
              <p className="text-[11px] text-slate-500">Convert approved customer quotes into subscriptions</p>
            </div>
            <button
              onClick={() => navigate('/admin/quotations')}
              className="text-xs font-semibold text-sky-600 hover:text-sky-700 flex items-center"
            >
              <span>View All</span>
              <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </button>
          </div>

          <div className="divide-y divide-slate-100 flex-1 overflow-auto max-h-96">
            {pendingQuotations.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">No open quotations pending conversion.</div>
            ) : (
              pendingQuotations.map((quo) => (
                <div key={quo.id} className="p-4 hover:bg-slate-50/80 transition flex items-center justify-between gap-3 text-xs">
                  <div>
                    <div className="font-mono font-bold text-indigo-700">{quo.quotationNumber}</div>
                    <div className="text-slate-900 font-medium mt-0.5">{quo.customerName}</div>
                    <div className="text-slate-500 text-[11px] mt-0.5">Plan: {quo.planName}</div>
                    <div className="text-[10px] text-slate-400">Valid until: {quo.validUntil}</div>
                  </div>

                  <div className="text-right flex flex-col items-end">
                    <div className="font-black text-slate-900 text-sm">₹{quo.totalAmount.toLocaleString()}</div>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200 mt-1">
                      {quo.status}
                    </span>
                    <button
                      onClick={() => handleConfirmQuote(quo.id)}
                      className="mt-2 px-3 py-1 bg-gradient-to-r from-indigo-600 to-sky-600 hover:from-indigo-700 hover:to-sky-700 text-white font-bold rounded-lg text-[11px] transition shadow-sm"
                    >
                      Confirm & Activate
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Record Payment Modal */}
      {selectedInvoice && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-slate-100 overflow-hidden">
            <div className="p-4 bg-slate-900 text-white flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold">Record Customer Payment</h3>
                <p className="text-[11px] text-slate-300 font-mono">Invoice: {selectedInvoice.invoiceNumber}</p>
              </div>
              <button onClick={() => setSelectedInvoice(null)} className="text-slate-400 hover:text-white">✕</button>
            </div>

            <form onSubmit={handleRecordPayment} className="p-5 space-y-3.5 text-xs">
              <div className="p-3 bg-slate-50 rounded-lg space-y-1">
                <div>Customer: <strong>{selectedInvoice.customerName}</strong></div>
                <div>Outstanding Balance: <strong className="text-rose-600">₹{selectedInvoice.outstandingAmount.toLocaleString()}</strong></div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Amount to Record (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  max={selectedInvoice.outstandingAmount}
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(Number(e.target.value))}
                  className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-sm font-bold text-slate-900 focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Payment Channel / Method</label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
                  className="w-full p-2 bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-sky-500"
                >
                  <option value="UPI">UPI (GPay / PhonePe / Razorpay)</option>
                  <option value="BANK_TRANSFER">Bank Transfer (NEFT / RTGS)</option>
                  <option value="CARD">Corporate Credit / Debit Card</option>
                  <option value="CASH">Cash Settlement</option>
                  <option value="OTHER">Other Financial Rail</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Transaction Ref / Bank UTR</label>
                <input
                  type="text"
                  required
                  value={transactionRef}
                  onChange={(e) => setTransactionRef(e.target.value)}
                  className="w-full p-2 bg-white border border-slate-200 rounded-lg font-mono focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Audit Notes</label>
                <textarea
                  rows={2}
                  value={paymentNotes}
                  onChange={(e) => setPaymentNotes(e.target.value)}
                  className="w-full p-2 bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setSelectedInvoice(null)}
                  className="px-3 py-1.5 text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingPayment}
                  className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-lg shadow transition"
                >
                  {submittingPayment ? 'Reconciling...' : 'Confirm Receipt'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
