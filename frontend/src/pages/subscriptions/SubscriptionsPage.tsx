import React, { useEffect, useState } from 'react';
import { subscriptionApi, planApi, prorationApi } from '../../services/api';
import { Subscription, SubscriptionStatus, Plan, ProrationCalculationResponse } from '../../types';
import {
  Repeat,
  PauseCircle,
  PlayCircle,
  XCircle,
  RefreshCw,
  Search,
  Filter,
  Calendar,
  AlertCircle,
  CheckCircle2,
  Zap,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';

export const SubscriptionsPage: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // Proration Modal State (Unique Hackathon Engine)
  const [prorationModalOpen, setProrationModalOpen] = useState(false);
  const [selectedSubForProration, setSelectedSubForProration] = useState<Subscription | null>(null);
  const [targetPlanId, setTargetPlanId] = useState<number>(0);
  const [effectiveDate, setEffectiveDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [prorationPreview, setProrationPreview] = useState<ProrationCalculationResponse | null>(null);
  const [calculatingProration, setCalculatingProration] = useState(false);
  const [applyingProration, setApplyingProration] = useState(false);
  const [prorationNotice, setProrationNotice] = useState<string | null>(null);

  const loadSubscriptions = async () => {
    setLoading(true);
    try {
      const params = statusFilter !== 'ALL' ? { status: statusFilter } : {};
      const [subsRes, plansRes] = await Promise.all([
        subscriptionApi.getAll(params),
        planApi.getAll().catch(() => ({ content: [] })),
      ]);
      setSubscriptions(subsRes.content);
      setPlans(plansRes.content || []);
    } catch (err) {
      console.error('Failed to load subscriptions', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSubscriptions();
  }, [statusFilter]);

  const handleOpenProration = (sub: Subscription) => {
    setSelectedSubForProration(sub);
    const otherPlan = plans.find((p) => p.name !== sub.planName);
    if (otherPlan) {
      setTargetPlanId(otherPlan.id);
    }
    setEffectiveDate(new Date().toISOString().split('T')[0]);
    setProrationPreview(null);
    setProrationModalOpen(true);
  };

  const handleSimulateProration = async () => {
    if (!selectedSubForProration || !targetPlanId) return;
    setCalculatingProration(true);
    try {
      const res = await prorationApi.calculate(selectedSubForProration.id, {
        newPlanId: targetPlanId,
        effectiveDate,
      });
      setProrationPreview(res);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to simulate proration');
    } finally {
      setCalculatingProration(false);
    }
  };

  const handleApplyProration = async () => {
    if (!selectedSubForProration || !targetPlanId) return;
    if (!confirm('Apply this mid-cycle plan switch? This will update the contract and write an immutable proration ledger entry.')) {
      return;
    }
    setApplyingProration(true);
    try {
      const res = await prorationApi.apply(selectedSubForProration.id, {
        newPlanId: targetPlanId,
        effectiveDate,
      });
      setProrationNotice(`Proration applied: ${res.summary}`);
      setProrationModalOpen(false);
      await loadSubscriptions();
      setTimeout(() => setProrationNotice(null), 6000);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to apply proration');
    } finally {
      setApplyingProration(false);
    }
  };

  const handlePause = async (id: number) => {
    if (!confirm('Pause recurring billing for this subscription?')) return;
    try {
      await subscriptionApi.pause(id);
      await loadSubscriptions();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to pause subscription');
    }
  };

  const handleResume = async (id: number) => {
    if (!confirm('Resume this subscription and re-enable automated recurring billing?')) return;
    try {
      await subscriptionApi.resume(id);
      await loadSubscriptions();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to resume subscription');
    }
  };

  const handleClose = async (id: number) => {
    if (!confirm('Are you sure you want to CLOSE this contract? Billing will cease permanently.')) return;
    try {
      await subscriptionApi.close(id);
      await loadSubscriptions();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to close subscription');
    }
  };

  const handleRenew = async (id: number) => {
    if (!confirm('Renew contract for another billing cycle?')) return;
    try {
      await subscriptionApi.renew(id);
      await loadSubscriptions();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to renew subscription');
    }
  };

  const getStatusBadge = (status: SubscriptionStatus) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">ACTIVE</span>;
      case 'PAUSED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">PAUSED</span>;
      case 'CLOSED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">CLOSED</span>;
      case 'RENEWED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 border border-purple-200">RENEWED</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-100 text-sky-800 border border-sky-200">{status}</span>;
    }
  };

  const filteredSubs = subscriptions.filter(
    (s) =>
      s.subscriptionNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.customerName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Active Subscriptions & Contracts</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Strict lifecycle state machine: DRAFT &rarr; QUOTATION &rarr; CONFIRMED &rarr; ACTIVE &rarr; PAUSED &rarr; CLOSED &rarr; RENEWED
          </p>
        </div>

        <button
          onClick={loadSubscriptions}
          className="p-2 bg-white border border-slate-200 text-slate-600 hover:text-slate-900 rounded-lg shadow-sm self-start sm:self-auto"
          title="Refresh List"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search subscription or customer..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-xs font-medium text-slate-500">Status:</span>
          {['ALL', 'ACTIVE', 'PAUSED', 'CLOSED'].map((st) => (
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
                <th className="py-3 px-4">Subscription Number</th>
                <th className="py-3 px-4">Customer Account</th>
                <th className="py-3 px-4">Plan & Product</th>
                <th className="py-3 px-4">Cadence</th>
                <th className="py-3 px-4">Periodic Amount</th>
                <th className="py-3 px-4">Next Billing Date</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Lifecycle Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredSubs.map((sub) => (
                <tr key={sub.id} className="hover:bg-slate-50/70 transition">
                  <td className="py-3.5 px-4 font-mono font-medium text-sky-700">{sub.subscriptionNumber}</td>
                  <td className="py-3.5 px-4">
                    <div className="font-semibold text-slate-900">{sub.customerName}</div>
                    <div className="text-xs text-slate-400">Terms: {sub.paymentTerms}</div>
                  </td>
                  <td className="py-3.5 px-4 text-slate-700 font-medium">{sub.planName}</td>
                  <td className="py-3.5 px-4 text-slate-500 text-xs">{sub.startDate} &rarr; {sub.expiryDate || 'Open'}</td>
                  <td className="py-3.5 px-4 font-bold text-slate-900">₹{sub.totalAmount.toLocaleString()}</td>
                  <td className="py-3.5 px-4 font-medium text-amber-700 text-xs flex items-center space-x-1 mt-3">
                    <Calendar className="w-3.5 h-3.5" />
                    <span>{sub.nextBillingDate}</span>
                  </td>
                  <td className="py-3.5 px-4">{getStatusBadge(sub.status)}</td>
                  <td className="py-3.5 px-4 text-right space-x-1.5">
                    {sub.status === 'ACTIVE' && (
                      <button
                        onClick={() => handlePause(sub.id)}
                        className="p-1.5 text-amber-600 hover:bg-amber-50 rounded border border-amber-200 transition"
                        title="Pause Subscription"
                      >
                        <PauseCircle className="w-4 h-4" />
                      </button>
                    )}
                    {sub.status === 'PAUSED' && (
                      <button
                        onClick={() => handleResume(sub.id)}
                        className="p-1.5 text-emerald-600 hover:bg-emerald-50 rounded border border-emerald-200 transition"
                        title="Resume Subscription"
                      >
                        <PlayCircle className="w-4 h-4" />
                      </button>
                    )}
                    {sub.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleClose(sub.id)}
                        className="p-1.5 text-rose-600 hover:bg-rose-50 rounded border border-rose-200 transition"
                        title="Early Closure"
                      >
                        <XCircle className="w-4 h-4" />
                      </button>
                    )}
                    {sub.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleOpenProration(sub)}
                        className="p-1.5 text-cyan-600 hover:bg-cyan-50 rounded border border-cyan-200 transition"
                        title="Simulate Mid-Cycle Proration / Upgrade"
                      >
                        <Zap className="w-4 h-4" />
                      </button>
                    )}
                    {(sub.status === 'ACTIVE' || sub.status === 'CLOSED') && (
                      <button
                        onClick={() => handleRenew(sub.id)}
                        className="p-1.5 text-purple-600 hover:bg-purple-50 rounded border border-purple-200 transition"
                        title="Renew Subscription"
                      >
                        <RefreshCw className="w-4 h-4" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Proration Notice Banner */}
      {prorationNotice && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs flex items-center space-x-2 animate-bounce">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
          <span>{prorationNotice}</span>
        </div>
      )}

      {/* Mid-Cycle Proration Engine Modal (Unique Hackathon Feature) */}
      {prorationModalOpen && selectedSubForProration && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-100 overflow-hidden">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 p-5 text-white flex items-center justify-between">
              <div>
                <div className="text-[10px] font-bold text-cyan-400 uppercase tracking-widest flex items-center gap-1.5">
                  <Zap className="w-3.5 h-3.5" />
                  Day-Level Mathematical Proration Engine
                </div>
                <h3 className="text-base font-bold mt-0.5">
                  Mid-Cycle Plan Switch: {selectedSubForProration.subscriptionNumber}
                </h3>
              </div>
              <button
                onClick={() => setProrationModalOpen(false)}
                className="text-slate-400 hover:text-white p-1"
              >
                ✕
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div className="p-3 bg-slate-50 rounded-lg text-xs space-y-1">
                <div>
                  Customer: <strong>{selectedSubForProration.customerName}</strong>
                </div>
                <div>
                  Current Tier: <strong>{selectedSubForProration.planName}</strong> (₹{selectedSubForProration.totalAmount.toLocaleString()}/cycle)
                </div>
                <div>
                  Active Cycle: {selectedSubForProration.startDate} &rarr; {selectedSubForProration.nextBillingDate}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Target Plan</label>
                  <select
                    value={targetPlanId}
                    onChange={(e) => {
                      setTargetPlanId(Number(e.target.value));
                      setProrationPreview(null);
                    }}
                    className="w-full text-xs p-2 bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-cyan-500"
                  >
                    {plans.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} (₹{p.price})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Effective Switch Date</label>
                  <input
                    type="date"
                    value={effectiveDate}
                    onChange={(e) => {
                      setEffectiveDate(e.target.value);
                      setProrationPreview(null);
                    }}
                    className="w-full text-xs p-2 bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-cyan-500"
                  />
                </div>
              </div>

              <button
                onClick={handleSimulateProration}
                disabled={calculatingProration}
                className="w-full py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-bold transition flex items-center justify-center space-x-1.5"
              >
                <TrendingUp className="w-4 h-4 text-cyan-400" />
                <span>{calculatingProration ? 'Simulating Credit...' : 'Simulate Pro-Rata Credit & Charge'}</span>
              </button>

              {prorationPreview && (
                <div className="p-4 bg-gradient-to-br from-cyan-50 to-indigo-50 border border-cyan-200 rounded-xl space-y-2 text-xs">
                  <div className="font-bold text-slate-800 text-sm">Proration Calculation Result:</div>
                  <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-600">
                    <div>Days Consumed: <strong>{prorationPreview.daysUsed} / {prorationPreview.daysInCycle}</strong></div>
                    <div>Days Remaining: <strong>{prorationPreview.daysRemaining} days</strong></div>
                    <div>Unutilized Credit: <strong className="text-emerald-700">₹{prorationPreview.proratedCredit.toFixed(2)}</strong></div>
                    <div>Pro-rata Charge: <strong className="text-sky-700">₹{prorationPreview.proratedCharge.toFixed(2)}</strong></div>
                  </div>
                  <div className="pt-2 border-t border-cyan-200 flex items-center justify-between font-bold text-sm">
                    <span>Net Adjustment:</span>
                    <span className={prorationPreview.netAdjustment >= 0 ? 'text-indigo-700' : 'text-emerald-700'}>
                      {prorationPreview.netAdjustment >= 0
                        ? `+₹${prorationPreview.netAdjustment.toFixed(2)} (Payable)`
                        : `-₹${Math.abs(prorationPreview.netAdjustment).toFixed(2)} (Credit Ledger)`}
                    </span>
                  </div>
                  <p className="text-[10px] text-slate-500 italic mt-1">{prorationPreview.summary}</p>
                </div>
              )}
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex justify-end space-x-2">
              <button
                onClick={() => setProrationModalOpen(false)}
                className="px-4 py-2 text-xs font-medium text-slate-600 hover:bg-slate-200 rounded-lg transition"
              >
                Cancel
              </button>
              <button
                onClick={handleApplyProration}
                disabled={!prorationPreview || applyingProration}
                className="px-4 py-2 text-xs font-bold bg-gradient-to-r from-cyan-600 to-indigo-600 hover:from-cyan-700 hover:to-indigo-700 disabled:opacity-50 text-white rounded-lg shadow-md transition"
              >
                {applyingProration ? 'Applying Switch...' : 'Confirm & Apply Plan Switch'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
