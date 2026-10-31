import React, { useEffect, useState } from 'react';
import { quotationApi, planApi, productApi } from '../../services/api';
import { Quotation, QuotationTemplate, Plan, Product } from '../../types';
import {
  FileSpreadsheet,
  Plus,
  Send,
  CheckCircle2,
  Clock,
  ArrowRight,
  Sparkles,
  Info,
} from 'lucide-react';

export const QuotationsPage: React.FC = () => {
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [templates, setTemplates] = useState<QuotationTemplate[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showTemplateModal, setShowTemplateModal] = useState(false);

  // New quote form
  const [quoteForm, setQuoteForm] = useState({
    customerId: 1,
    planId: 1,
    validityDays: 30,
    notes: 'Standard enterprise business quote',
    quantity: 1,
  });

  // New template form
  const [templateForm, setTemplateForm] = useState({
    name: '',
    defaultPlanId: 1,
    validityDays: 30,
  });

  const loadData = async () => {
    setLoading(true);
    try {
      const [quotesRes, tplsRes, plansRes] = await Promise.all([
        quotationApi.getAll(),
        quotationApi.getTemplates(),
        planApi.getAll(),
      ]);
      setQuotations(quotesRes.content);
      setTemplates(tplsRes);
      setPlans(plansRes.content);
      if (plansRes.content.length > 0) {
        setQuoteForm((prev) => ({ ...prev, planId: plansRes.content[0].id }));
        setTemplateForm((prev) => ({ ...prev, defaultPlanId: plansRes.content[0].id }));
      }
    } catch (err) {
      console.error('Failed to load quotations or templates', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateQuoteFromTemplate = (tpl: QuotationTemplate) => {
    setQuoteForm({
      customerId: 1,
      planId: tpl.defaultPlanId,
      validityDays: tpl.validityDays,
      notes: `Generated from template: ${tpl.name}`,
      quantity: 1,
    });
    setShowCreateModal(true);
  };

  const handleCreateQuotation = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const selectedPlan = plans.find((p) => p.id === Number(quoteForm.planId));
      if (!selectedPlan) return;

      const payload = {
        customerId: Number(quoteForm.customerId),
        planId: selectedPlan.id,
        validUntil: new Date(Date.now() + quoteForm.validityDays * 86400000).toISOString().split('T')[0],
        notes: quoteForm.notes,
        items: [
          {
            productId: selectedPlan.productId,
            quantity: Number(quoteForm.quantity),
            unitPrice: selectedPlan.price,
            taxRatePercent: 18.0,
            discountAmount: 0.0,
          },
        ],
      };

      await quotationApi.create(payload);
      setShowCreateModal(false);
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create quotation');
    }
  };

  const handleConfirmQuotation = async (id: number) => {
    if (!confirm('Confirming this quote will automatically transform it into an ACTIVE subscription and start recurring billing schedule. Proceed?')) {
      return;
    }
    try {
      const sub = await quotationApi.confirm(id);
      alert(`Quote successfully confirmed and converted into Subscription: ${sub.subscriptionNumber}!`);
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to confirm quotation');
    }
  };

  const handleCreateTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await quotationApi.createTemplate(templateForm);
      setShowTemplateModal(false);
      setTemplateForm({ name: '', defaultPlanId: plans[0]?.id || 1, validityDays: 30 });
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create template');
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'DRAFT':
        return <span className="px-2 py-0.5 rounded text-xs font-semibold bg-slate-100 text-slate-700">DRAFT</span>;
      case 'QUOTATION':
        return <span className="px-2 py-0.5 rounded text-xs font-semibold bg-sky-100 text-sky-800">SENT QUOTE</span>;
      case 'CONFIRMED':
        return <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-800">CONFIRMED (SUBSCRIBED)</span>;
      case 'EXPIRED':
        return <span className="px-2 py-0.5 rounded text-xs font-semibold bg-rose-100 text-rose-800">EXPIRED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-xs font-semibold bg-slate-100 text-slate-700">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Quotes & Reusable Templates</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Create customer quotes from business templates and convert confirmed quotes directly into active subscriptions
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowTemplateModal(true)}
            className="px-3.5 py-2 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 text-sm font-semibold rounded-lg shadow-sm transition"
          >
            + New Template
          </button>
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center space-x-2 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-lg shadow-sm transition"
          >
            <Plus className="w-4 h-4" />
            <span>Create Quotation</span>
          </button>
        </div>
      </div>

      {/* Reusable Templates Banner */}
      <div>
        <h2 className="text-sm font-bold text-slate-700 uppercase tracking-wider mb-3">
          Quick Templates
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {templates.map((tpl) => (
            <div
              key={tpl.id}
              className="p-4 bg-gradient-to-br from-white to-sky-50/50 rounded-xl border border-sky-100 shadow-sm flex items-center justify-between"
            >
              <div>
                <div className="font-semibold text-slate-900 text-sm">{tpl.name}</div>
                <div className="text-xs text-slate-500 mt-0.5">
                  Plan #{tpl.defaultPlanId} • Valid for {tpl.validityDays} days
                </div>
              </div>
              <button
                onClick={() => handleCreateQuoteFromTemplate(tpl)}
                className="px-3 py-1.5 text-xs font-semibold bg-sky-600 hover:bg-sky-500 text-white rounded-lg shadow-sm flex items-center space-x-1"
              >
                <span>Use</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Lifecycle Flow Info */}
      <div className="p-4 bg-white border border-slate-200 rounded-xl text-xs flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <span className="font-semibold text-slate-700">Enforced Lifecycle:</span>
          <div className="flex items-center space-x-2">
            <span className="px-2 py-0.5 bg-slate-100 rounded text-slate-700 font-medium">1. DRAFT</span>
            <span>&rarr;</span>
            <span className="px-2 py-0.5 bg-sky-100 rounded text-sky-800 font-medium">2. QUOTATION</span>
            <span>&rarr;</span>
            <span className="px-2 py-0.5 bg-emerald-100 rounded text-emerald-800 font-medium">
              3. CONFIRMED &rarr; Auto-converted to ACTIVE Subscription
            </span>
          </div>
        </div>
      </div>

      {/* Quotations List */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                <th className="py-3 px-4">Quote Number</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Plan</th>
                <th className="py-3 px-4">Subtotal</th>
                <th className="py-3 px-4">Tax (18%)</th>
                <th className="py-3 px-4">Total Amount</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Valid Until</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {quotations.map((q) => (
                <tr key={q.id} className="hover:bg-slate-50/70 transition">
                  <td className="py-3 px-4 font-mono font-medium text-sky-700">{q.quotationNumber}</td>
                  <td className="py-3 px-4 font-semibold text-slate-900">{q.customerName || `Customer #${q.customerId}`}</td>
                  <td className="py-3 px-4 text-slate-600">{q.planName || `Plan #${q.planId}`}</td>
                  <td className="py-3 px-4 text-slate-600">₹{q.subtotal?.toLocaleString()}</td>
                  <td className="py-3 px-4 text-slate-500">₹{q.taxAmount?.toLocaleString()}</td>
                  <td className="py-3 px-4 font-bold text-slate-900">₹{q.totalAmount?.toLocaleString()}</td>
                  <td className="py-3 px-4">{getStatusBadge(q.status)}</td>
                  <td className="py-3 px-4 text-xs text-slate-500">{q.validUntil}</td>
                  <td className="py-3 px-4 text-right">
                    {q.status !== 'CONFIRMED' && (
                      <button
                        onClick={() => handleConfirmQuotation(q.id)}
                        className="px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold transition"
                      >
                        Confirm & Subscribe
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal: Create Quotation */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Create Customer Quotation</h2>
            <form onSubmit={handleCreateQuotation} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Target Plan</label>
                <select
                  value={quoteForm.planId}
                  onChange={(e) => setQuoteForm({ ...quoteForm, planId: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                >
                  {plans.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (₹{p.price.toLocaleString()} / {p.billingFrequency.toLowerCase()})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Customer ID</label>
                  <input
                    type="number"
                    required
                    value={quoteForm.customerId}
                    onChange={(e) => setQuoteForm({ ...quoteForm, customerId: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Quantity</label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={quoteForm.quantity}
                    onChange={(e) => setQuoteForm({ ...quoteForm, quantity: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Validity (Days)</label>
                <input
                  type="number"
                  required
                  value={quoteForm.validityDays}
                  onChange={(e) => setQuoteForm({ ...quoteForm, validityDays: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Quotation Notes</label>
                <textarea
                  rows={2}
                  value={quoteForm.notes}
                  onChange={(e) => setQuoteForm({ ...quoteForm, notes: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-500 rounded-lg"
                >
                  Generate Quotation
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Create Template */}
      {showTemplateModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Create Reusable Quotation Template</h2>
            <form onSubmit={handleCreateTemplate} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Template Name</label>
                <input
                  type="text"
                  required
                  value={templateForm.name}
                  onChange={(e) => setTemplateForm({ ...templateForm, name: e.target.value })}
                  placeholder="Enterprise Standard Contract"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Default Plan</label>
                <select
                  value={templateForm.defaultPlanId}
                  onChange={(e) => setTemplateForm({ ...templateForm, defaultPlanId: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                >
                  {plans.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Validity (Days)</label>
                <input
                  type="number"
                  required
                  value={templateForm.validityDays}
                  onChange={(e) => setTemplateForm({ ...templateForm, validityDays: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowTemplateModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-500 rounded-lg"
                >
                  Save Template
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
