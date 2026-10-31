import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { planApi, productApi } from '../../services/api';
import { Plan, Product, BillingFrequency } from '../../types';
import { Layers, Plus, Calendar, Clock, CheckCircle2, XCircle, AlertCircle } from 'lucide-react';

export const PlansPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  const [planForm, setPlanForm] = useState({
    productId: 1,
    name: '',
    code: '',
    price: 2000,
    billingFrequency: 'MONTHLY' as BillingFrequency,
    minQuantity: 1,
    availabilityStartDate: '2024-01-01',
    availabilityEndDate: '2030-12-31',
    autoCloseOnExpiry: true,
    allowPause: true,
    allowEarlyClosure: true,
    allowRenewal: true,
  });

  const loadData = async () => {
    setLoading(true);
    try {
      const [plansRes, prodsRes] = await Promise.all([planApi.getAll(), productApi.getAll()]);
      setPlans(plansRes.content);
      setProducts(prodsRes.content);
      if (prodsRes.content.length > 0) {
        setPlanForm((prev) => ({ ...prev, productId: prodsRes.content[0].id }));
      }
    } catch (err) {
      console.error('Failed to load plans or products', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreatePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await planApi.create({
        ...planForm,
        price: Number(planForm.price),
        minQuantity: Number(planForm.minQuantity),
        productId: Number(planForm.productId),
      });
      setShowAddModal(false);
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create plan');
    }
  };

  const getFrequencyBadge = (freq: BillingFrequency) => {
    const map = {
      DAILY: 'bg-amber-100 text-amber-800 border-amber-200',
      WEEKLY: 'bg-emerald-100 text-emerald-800 border-emerald-200',
      MONTHLY: 'bg-sky-100 text-sky-800 border-sky-200',
      YEARLY: 'bg-purple-100 text-purple-800 border-purple-200',
    };
    return (
      <span className={`px-2 py-0.5 rounded text-xs font-semibold border ${map[freq]}`}>
        {freq}
      </span>
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Subscription Plans</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Configure recurring billing cadences (Daily, Weekly, Monthly, Yearly), pause rights, and renewal policies
          </p>
        </div>

        {isAdmin ? (
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center space-x-2 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-lg shadow-sm transition"
          >
            <Plus className="w-4 h-4" />
            <span>New Subscription Plan</span>
          </button>
        ) : (
          <span className="px-3 py-1.5 bg-slate-100 text-slate-500 rounded-lg text-xs font-semibold border border-slate-200">
            Catalog Mode (Admin Configures Plans)
          </span>
        )}
      </div>

      {/* Plans Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {plans.map((p) => (
          <div key={p.id} className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-mono font-bold text-slate-400">{p.code}</span>
                {getFrequencyBadge(p.billingFrequency)}
              </div>

              <h2 className="text-lg font-bold text-slate-900 mb-1">{p.name}</h2>
              <div className="text-xs text-sky-700 font-medium mb-4">Product: {p.productName}</div>

              <div className="mb-6">
                <div className="text-3xl font-extrabold text-slate-900">
                  ₹{p.price.toLocaleString()}
                  <span className="text-xs font-normal text-slate-500 ml-1">/ {p.billingFrequency.toLowerCase()}</span>
                </div>
                <div className="text-xs text-slate-400 mt-1">Min. order quantity: {p.minQuantity} seat(s)</div>
              </div>

              {/* Lifecycle & Policy Permissions */}
              <div className="space-y-2 border-t border-slate-100 pt-4 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Auto-Close on Expiry:</span>
                  <span className={`font-semibold ${p.autoCloseOnExpiry ? 'text-emerald-600' : 'text-slate-400'}`}>
                    {p.autoCloseOnExpiry ? 'Yes (Enforced)' : 'No'}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Customer Pause Permission:</span>
                  <span className={`font-semibold ${p.allowPause ? 'text-emerald-600' : 'text-rose-500'}`}>
                    {p.allowPause ? 'Allowed' : 'Disabled'}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Early Closure Right:</span>
                  <span className={`font-semibold ${p.allowEarlyClosure ? 'text-emerald-600' : 'text-rose-500'}`}>
                    {p.allowEarlyClosure ? 'Allowed' : 'Locked Contract'}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Renewal Permission:</span>
                  <span className={`font-semibold ${p.allowRenewal ? 'text-emerald-600' : 'text-rose-500'}`}>
                    {p.allowRenewal ? 'Eligible' : 'Non-Renewable'}
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-slate-100 text-[11px] text-slate-400 flex items-center justify-between">
              <span>Valid: {p.availabilityStartDate}</span>
              <span>to {p.availabilityEndDate}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Modal: Create Plan */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Create New Subscription Plan</h2>
            <form onSubmit={handleCreatePlan} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Target Product</label>
                <select
                  value={planForm.productId}
                  onChange={(e) => setPlanForm({ ...planForm, productId: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                >
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (Base: ₹{p.salesPrice})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Plan Name</label>
                  <input
                    type="text"
                    required
                    value={planForm.name}
                    onChange={(e) => setPlanForm({ ...planForm, name: e.target.value })}
                    placeholder="ERP Growth Quarterly"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Plan Code</label>
                  <input
                    type="text"
                    required
                    value={planForm.code}
                    onChange={(e) => setPlanForm({ ...planForm, code: e.target.value })}
                    placeholder="PLAN-ERP-Q"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={planForm.price}
                    onChange={(e) => setPlanForm({ ...planForm, price: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Frequency</label>
                  <select
                    value={planForm.billingFrequency}
                    onChange={(e: any) => setPlanForm({ ...planForm, billingFrequency: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  >
                    <option value="DAILY">DAILY</option>
                    <option value="WEEKLY">WEEKLY</option>
                    <option value="MONTHLY">MONTHLY</option>
                    <option value="YEARLY">YEARLY</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Min Quantity</label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={planForm.minQuantity}
                    onChange={(e) => setPlanForm({ ...planForm, minQuantity: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              {/* Policy Toggles */}
              <div className="grid grid-cols-2 gap-2 text-xs border border-slate-200 rounded-lg p-3 bg-slate-50">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={planForm.allowPause}
                    onChange={(e) => setPlanForm({ ...planForm, allowPause: e.target.checked })}
                    className="rounded text-sky-600"
                  />
                  <span>Allow Subscription Pause</span>
                </label>
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={planForm.allowEarlyClosure}
                    onChange={(e) => setPlanForm({ ...planForm, allowEarlyClosure: e.target.checked })}
                    className="rounded text-sky-600"
                  />
                  <span>Allow Early Closure</span>
                </label>
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={planForm.allowRenewal}
                    onChange={(e) => setPlanForm({ ...planForm, allowRenewal: e.target.checked })}
                    className="rounded text-sky-600"
                  />
                  <span>Allow Renewal</span>
                </label>
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={planForm.autoCloseOnExpiry}
                    onChange={(e) => setPlanForm({ ...planForm, autoCloseOnExpiry: e.target.checked })}
                    className="rounded text-sky-600"
                  />
                  <span>Auto-close on Expiry</span>
                </label>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-500 rounded-lg"
                >
                  Create Plan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
