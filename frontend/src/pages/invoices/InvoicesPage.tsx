import React, { useEffect, useState } from 'react';
import { invoiceApi, paymentApi } from '../../services/api';
import { Invoice, InvoiceStatus, PaymentMethod } from '../../types';
import {
  Receipt,
  CreditCard,
  Printer,
  AlertTriangle,
  CheckCircle2,
  Filter,
  DollarSign,
  Search,
  Eye,
  FileText,
} from 'lucide-react';

export const InvoicesPage: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [printableHtml, setPrintableHtml] = useState<string | null>(null);

  // Record Payment Modal State
  const [selectedInvoiceForPayment, setSelectedInvoiceForPayment] = useState<Invoice | null>(null);
  const [paymentForm, setPaymentForm] = useState({
    amount: 0,
    paymentMethod: 'UPI' as PaymentMethod,
    transactionReference: '',
    notes: 'Online corporate payment settlement',
  });

  const loadInvoices = async () => {
    setLoading(true);
    try {
      const params = statusFilter !== 'ALL' ? { status: statusFilter } : {};
      const res = await invoiceApi.getAll(params);
      setInvoices(res.content);
    } catch (err) {
      console.error('Failed to load invoices', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInvoices();
  }, [statusFilter]);

  const handleOpenPaymentModal = (inv: Invoice) => {
    setSelectedInvoiceForPayment(inv);
    setPaymentForm({
      amount: inv.outstandingAmount,
      paymentMethod: 'UPI',
      transactionReference: `TXN-${Date.now().toString().slice(-6)}`,
      notes: `Settlement for invoice ${inv.invoiceNumber}`,
    });
  };

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInvoiceForPayment) return;

    try {
      await paymentApi.recordPayment({
        invoiceId: selectedInvoiceForPayment.id,
        amount: Number(paymentForm.amount),
        paymentMethod: paymentForm.paymentMethod,
        transactionReference: paymentForm.transactionReference,
        notes: paymentForm.notes,
      });
      setSelectedInvoiceForPayment(null);
      await loadInvoices();
      alert('Payment recorded successfully! Ledger reconciled.');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to record payment');
    }
  };

  const handleViewPrintable = async (id: number) => {
    try {
      const html = await invoiceApi.getPrintableHtml(id);
      setPrintableHtml(html);
    } catch (err: any) {
      console.error('Printable invoice error:', err);
      alert(err.response?.data?.message || err.message || 'Failed to generate printable invoice');
    }
  };

  const getStatusBadge = (inv: Invoice) => {
    const isOverdue =
      inv.status !== 'PAID' && inv.status !== 'CANCELLED' && new Date(inv.dueDate) < new Date();

    if (inv.status === 'PAID') {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
          PAID
        </span>
      );
    }
    if (isOverdue) {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">
          OVERDUE
        </span>
      );
    }
    if (inv.status === 'CONFIRMED') {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
          UNPAID
        </span>
      );
    }
    return (
      <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">
        {inv.status}
      </span>
    );
  };

  const filteredInvoices = invoices.filter(
    (inv) =>
      inv.invoiceNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      inv.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      inv.subscriptionNumber.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Invoice Management & Billing</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Automated invoice generation, tax & discount breakdowns, multi-channel payment reconciliation, and PDF/HTML print
          </p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search invoice or customer..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-xs font-medium text-slate-500">Filter:</span>
          {['ALL', 'CONFIRMED', 'PAID', 'DRAFT'].map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition ${
                statusFilter === st
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                <th className="py-3 px-4">Invoice #</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Subscription</th>
                <th className="py-3 px-4">Due Date</th>
                <th className="py-3 px-4">Subtotal</th>
                <th className="py-3 px-4">Total Amount</th>
                <th className="py-3 px-4">Outstanding</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredInvoices.map((inv) => (
                <tr key={inv.id} className="hover:bg-slate-50/70 transition">
                  <td className="py-3.5 px-4 font-mono font-medium text-sky-700">{inv.invoiceNumber}</td>
                  <td className="py-3.5 px-4">
                    <div className="font-semibold text-slate-900">{inv.customerName}</div>
                    <div className="text-xs text-slate-400">{inv.customerBillingEmail}</div>
                  </td>
                  <td className="py-3.5 px-4 font-mono text-xs text-slate-600">{inv.subscriptionNumber}</td>
                  <td className="py-3.5 px-4 text-xs text-slate-600">{inv.dueDate}</td>
                  <td className="py-3.5 px-4 text-slate-600">₹{inv.subtotal.toLocaleString()}</td>
                  <td className="py-3.5 px-4 font-bold text-slate-900">₹{inv.totalAmount.toLocaleString()}</td>
                  <td className="py-3.5 px-4 font-semibold text-amber-600">
                    ₹{inv.outstandingAmount.toLocaleString()}
                  </td>
                  <td className="py-3.5 px-4">{getStatusBadge(inv)}</td>
                  <td className="py-3.5 px-4 text-right space-x-2">
                    <button
                      onClick={() => handleViewPrintable(inv.id)}
                      className="p-1.5 text-slate-600 hover:text-sky-600 hover:bg-slate-100 rounded border border-slate-200 transition"
                      title="View Printable Invoice"
                    >
                      <Printer className="w-4 h-4" />
                    </button>

                    {inv.outstandingAmount > 0 && inv.status !== 'CANCELLED' && (
                      <button
                        onClick={() => handleOpenPaymentModal(inv)}
                        className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold transition"
                      >
                        Record Payment
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal: Printable Invoice Preview */}
      {printableHtml && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full max-h-[90vh] flex flex-col overflow-hidden">
            <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
              <h2 className="text-base font-bold text-slate-800 flex items-center space-x-2">
                <FileText className="w-5 h-5 text-sky-600" />
                <span>Formal Enterprise Tax Invoice</span>
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
                  className="px-3 py-1.5 bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold rounded-lg flex items-center space-x-1.5"
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

      {/* Modal: Record Payment */}
      {selectedInvoiceForPayment && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-1">Record Payment Receipt</h2>
            <p className="text-xs text-slate-500 mb-4">
              Invoice <strong>{selectedInvoiceForPayment.invoiceNumber}</strong> • Total:{' '}
              ₹{selectedInvoiceForPayment.totalAmount.toLocaleString()} • Outstanding:{' '}
              <strong className="text-amber-600">
                ₹{selectedInvoiceForPayment.outstandingAmount.toLocaleString()}
              </strong>
            </p>

            <form onSubmit={handleRecordPayment} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Payment Method
                </label>
                <select
                  value={paymentForm.paymentMethod}
                  onChange={(e: any) => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                >
                  <option value="UPI">UPI</option>
                  <option value="BANK_TRANSFER">Bank Transfer (NEFT/RTGS/IMPS)</option>
                  <option value="CARD">Credit / Debit Card</option>
                  <option value="CASH">Cash</option>
                  <option value="OTHER">Other Gateway Settlement</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Amount Received (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  max={selectedInvoiceForPayment.outstandingAmount}
                  required
                  value={paymentForm.amount}
                  onChange={(e) => setPaymentForm({ ...paymentForm, amount: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm font-semibold"
                />
                <p className="text-xs text-slate-400 mt-1">
                  Remaining balance after settlement:{' '}
                  <strong>
                    ₹
                    {Math.max(
                      0,
                      selectedInvoiceForPayment.outstandingAmount - Number(paymentForm.amount || 0)
                    ).toLocaleString()}
                  </strong>
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Transaction / UTR Reference
                </label>
                <input
                  type="text"
                  required
                  value={paymentForm.transactionReference}
                  onChange={(e) => setPaymentForm({ ...paymentForm, transactionReference: e.target.value })}
                  placeholder="UPI/UTR Reference ID"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Notes</label>
                <textarea
                  rows={2}
                  value={paymentForm.notes}
                  onChange={(e) => setPaymentForm({ ...paymentForm, notes: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setSelectedInvoiceForPayment(null)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-emerald-600 hover:bg-emerald-500 rounded-lg"
                >
                  Confirm Payment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
