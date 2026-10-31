import React, { useEffect, useState } from 'react';
import { paymentApi } from '../../services/api';
import { Payment } from '../../types';
import { CreditCard, ShieldCheck, Search, RefreshCw, Calendar, FileText } from 'lucide-react';

export const PaymentsPage: React.FC = () => {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  const loadPayments = async () => {
    setLoading(true);
    try {
      const res = await paymentApi.getAll();
      setPayments(res.content);
    } catch (err) {
      console.error('Failed to load payments', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPayments();
  }, []);

  const filteredPayments = payments.filter(
    (p) =>
      p.paymentNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.invoiceNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (p.transactionReference && p.transactionReference.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Payment Ledger & Reconciliation</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Immutable financial audit trail across UPI, Bank Transfers, Cards, and Cash settlements
          </p>
        </div>

        <button
          onClick={loadPayments}
          className="p-2 bg-white border border-slate-200 text-slate-600 hover:text-slate-900 rounded-lg shadow-sm"
          title="Refresh Ledger"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Ledger Immutability Banner */}
      <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center space-x-3 text-emerald-900 text-xs">
        <ShieldCheck className="w-5 h-5 text-emerald-600 flex-shrink-0" />
        <div>
          <span className="font-semibold text-emerald-800">Financial Immutability Guarantee:</span>
          <p className="mt-0.5 text-emerald-700">
            Payment records are append-only and cannot be altered or deleted. Every credit reconciles against invoice balances.
          </p>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative w-full sm:max-w-md">
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search by payment #, customer, invoice or UTR..."
          className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
        />
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                <th className="py-3 px-4">Receipt #</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Invoice #</th>
                <th className="py-3 px-4">Method</th>
                <th className="py-3 px-4">Transaction / UTR</th>
                <th className="py-3 px-4">Amount Paid</th>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4">Notes</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredPayments.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50/70 transition">
                  <td className="py-3.5 px-4 font-mono font-medium text-sky-700">{p.paymentNumber}</td>
                  <td className="py-3.5 px-4 font-semibold text-slate-900">{p.customerName}</td>
                  <td className="py-3.5 px-4 font-mono text-xs text-slate-600">{p.invoiceNumber}</td>
                  <td className="py-3.5 px-4">
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">
                      {p.paymentMethod}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-mono text-xs text-slate-600">{p.transactionReference || '—'}</td>
                  <td className="py-3.5 px-4 font-bold text-emerald-700">₹{p.amount.toLocaleString()}</td>
                  <td className="py-3.5 px-4 text-xs text-slate-500">{p.paymentDate}</td>
                  <td className="py-3.5 px-4 text-xs text-slate-500 italic">{p.notes || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
