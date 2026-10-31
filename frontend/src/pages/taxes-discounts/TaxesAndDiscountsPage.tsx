import React, { useEffect, useState } from 'react';
import { taxApi, discountApi } from '../../services/api';
import { TaxRule, DiscountRule } from '../../types';
import { Percent, Plus, Tag, ShieldCheck, CheckCircle2, AlertCircle } from 'lucide-react';

export const TaxesAndDiscountsPage: React.FC = () => {
  const [taxes, setTaxes] = useState<TaxRule[]>([]);
  const [discounts, setDiscounts] = useState<DiscountRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showTaxModal, setShowTaxModal] = useState(false);
  const [showDiscountModal, setShowDiscountModal] = useState(false);

  // New Tax Form
  const [taxForm, setTaxForm] = useState({
    name: '',
    code: '',
    ratePercent: 18.0,
    applicability: 'ALL',
    effectiveStartDate: '2024-01-01',
    effectiveEndDate: '2030-12-31',
    active: true,
  });

  // New Discount Form
  const [discountForm, setDiscountForm] = useState({
    name: '',
    code: '',
    discountType: 'PERCENTAGE' as 'PERCENTAGE' | 'FIXED_AMOUNT',
    discountValue: 10,
    minPurchaseAmount: 1000,
    minQuantity: 1,
    startDate: '2024-01-01',
    endDate: '2030-12-31',
    maxUses: 100,
    scope: 'SUBSCRIPTION_LEVEL' as 'SUBSCRIPTION_LEVEL' | 'PRODUCT_SPECIFIC',
    active: true,
  });

  const loadData = async () => {
    setLoading(true);
    try {
      const [taxRes, discRes] = await Promise.all([taxApi.getAll(), discountApi.getAll()]);
      setTaxes(taxRes);
      setDiscounts(discRes);
    } catch (err) {
      console.error('Failed to load taxes or discounts', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateTax = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await taxApi.create({
        ...taxForm,
        ratePercent: Number(taxForm.ratePercent),
      });
      setShowTaxModal(false);
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create tax rule');
    }
  };

  const handleCreateDiscount = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await discountApi.create({
        ...discountForm,
        discountValue: Number(discountForm.discountValue),
        minPurchaseAmount: Number(discountForm.minPurchaseAmount),
        minQuantity: Number(discountForm.minQuantity),
        maxUses: Number(discountForm.maxUses),
      });
      setShowDiscountModal(false);
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create discount voucher');
    }
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Taxes & Discount Rules</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Configure centralized tax calculation rules (GST, VAT) and promotional discount caps with concurrency-safe usage limits
        </p>
      </div>

      {/* Tax Rules Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-800 flex items-center space-x-2">
            <Percent className="w-5 h-5 text-sky-600" />
            <span>Configured Tax Rules</span>
          </h2>
          <button
            onClick={() => setShowTaxModal(true)}
            className="px-3 py-1.5 bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold rounded-lg shadow-sm flex items-center space-x-1"
          >
            <Plus className="w-4 h-4" />
            <span>New Tax Rule</span>
          </button>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                <th className="py-3 px-4">Code</th>
                <th className="py-3 px-4">Tax Name</th>
                <th className="py-3 px-4">Rate (%)</th>
                <th className="py-3 px-4">Applicability</th>
                <th className="py-3 px-4">Effective Dates</th>
                <th className="py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {taxes.map((t) => (
                <tr key={t.id} className="hover:bg-slate-50/70">
                  <td className="py-3 px-4 font-mono font-medium text-sky-700">{t.code}</td>
                  <td className="py-3 px-4 font-semibold text-slate-900">{t.name}</td>
                  <td className="py-3 px-4 font-bold text-slate-800">{t.ratePercent}%</td>
                  <td className="py-3 px-4 text-xs text-slate-600">{t.applicability}</td>
                  <td className="py-3 px-4 text-xs text-slate-500">
                    {t.effectiveStartDate} to {t.effectiveEndDate}
                  </td>
                  <td className="py-3 px-4">
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-800">
                      ACTIVE
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Discount Rules Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-800 flex items-center space-x-2">
            <Tag className="w-5 h-5 text-indigo-600" />
            <span>Promotional Discount Rules & Usage Caps</span>
          </h2>
          <button
            onClick={() => setShowDiscountModal(true)}
            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-lg shadow-sm flex items-center space-x-1"
          >
            <Plus className="w-4 h-4" />
            <span>New Discount Rule</span>
          </button>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                <th className="py-3 px-4">Voucher Code</th>
                <th className="py-3 px-4">Discount Name</th>
                <th className="py-3 px-4">Type & Value</th>
                <th className="py-3 px-4">Min. Purchase</th>
                <th className="py-3 px-4">Scope</th>
                <th className="py-3 px-4">Usage Cap</th>
                <th className="py-3 px-4">Validity</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {discounts.map((d) => (
                <tr key={d.id} className="hover:bg-slate-50/70">
                  <td className="py-3 px-4 font-mono font-bold text-indigo-700">{d.code}</td>
                  <td className="py-3 px-4 font-semibold text-slate-900">{d.name}</td>
                  <td className="py-3 px-4 font-bold text-emerald-700">
                    {d.discountType === 'PERCENTAGE' ? `${d.discountValue}% OFF` : `₹${d.discountValue} FLAT OFF`}
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-600">
                    ₹{d.minPurchaseAmount.toLocaleString()} (Min {d.minQuantity} qty)
                  </td>
                  <td className="py-3 px-4">
                    <span className="px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700">
                      {d.scope}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-xs">
                    <span className="font-semibold text-slate-900">{d.usedCount}</span> / {d.maxUses} used
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-500">
                    {d.startDate} to {d.endDate}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal: New Tax */}
      {showTaxModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Add Tax Rule</h2>
            <form onSubmit={handleCreateTax} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Tax Code</label>
                  <input
                    type="text"
                    required
                    value={taxForm.code}
                    onChange={(e) => setTaxForm({ ...taxForm, code: e.target.value })}
                    placeholder="GST_12"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Rate (%)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={taxForm.ratePercent}
                    onChange={(e) => setTaxForm({ ...taxForm, ratePercent: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Tax Name</label>
                <input
                  type="text"
                  required
                  value={taxForm.name}
                  onChange={(e) => setTaxForm({ ...taxForm, name: e.target.value })}
                  placeholder="Goods & Services Tax 12%"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowTaxModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-500 rounded-lg"
                >
                  Save Tax Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: New Discount */}
      {showDiscountModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Add Discount Rule</h2>
            <form onSubmit={handleCreateDiscount} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Code</label>
                  <input
                    type="text"
                    required
                    value={discountForm.code}
                    onChange={(e) => setDiscountForm({ ...discountForm, code: e.target.value })}
                    placeholder="FESTIVE30"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Type</label>
                  <select
                    value={discountForm.discountType}
                    onChange={(e: any) => setDiscountForm({ ...discountForm, discountType: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  >
                    <option value="PERCENTAGE">PERCENTAGE (%)</option>
                    <option value="FIXED_AMOUNT">FIXED AMOUNT (₹)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Discount Name</label>
                <input
                  type="text"
                  required
                  value={discountForm.name}
                  onChange={(e) => setDiscountForm({ ...discountForm, name: e.target.value })}
                  placeholder="Diwali Festive Offer 30%"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Value ({discountForm.discountType === 'PERCENTAGE' ? '%' : '₹'})
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={discountForm.discountValue}
                    onChange={(e) => setDiscountForm({ ...discountForm, discountValue: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Max Uses</label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={discountForm.maxUses}
                    onChange={(e) => setDiscountForm({ ...discountForm, maxUses: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowDiscountModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-lg"
                >
                  Save Discount
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
