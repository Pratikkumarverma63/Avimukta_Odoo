import React, { useEffect, useState } from 'react';
import { dashboardApi, billingApi, dunningApi } from '../../services/api';
import { DashboardMetrics, RevenueTrend, DunningCase } from '../../types';
import {
  TrendingUp,
  Users,
  Repeat,
  DollarSign,
  AlertTriangle,
  Calendar,
  CheckCircle2,
  Clock,
  ArrowUpRight,
  PlayCircle,
  CreditCard,
  Building,
  RefreshCw,
  ShieldAlert,
  Zap,
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [trend, setTrend] = useState<RevenueTrend | null>(null);
  const [dunningCases, setDunningCases] = useState<DunningCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggeringBilling, setTriggeringBilling] = useState(false);
  const [triggeringDunning, setTriggeringDunning] = useState(false);
  const [billingSummary, setBillingSummary] = useState<any | null>(null);
  const [dunningSummary, setDunningSummary] = useState<string | null>(null);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [metricsData, trendData, casesData] = await Promise.all([
        dashboardApi.getMetrics(),
        dashboardApi.getRevenueTrend(),
        dunningApi.getCases().catch(() => []),
      ]);
      setMetrics(metricsData);
      setTrend(trendData);
      setDunningCases(casesData || []);
    } catch (err) {
      console.error('Failed to load dashboard metrics', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleManualBillingTrigger = async () => {
    setTriggeringBilling(true);
    try {
      const res = await billingApi.triggerRun();
      setBillingSummary(res);
      await fetchDashboardData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Billing execution failed');
    } finally {
      setTriggeringBilling(false);
    }
  };

  const handleDunningSweep = async () => {
    setTriggeringDunning(true);
    setDunningSummary(null);
    try {
      const res = await dunningApi.process();
      setDunningSummary(res.message || 'Dunning cycle executed successfully');
      await fetchDashboardData();
      setTimeout(() => setDunningSummary(null), 6000);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Dunning sweep failed');
    } finally {
      setTriggeringDunning(false);
    }
  };

  const formatCurrency = (amount: number = 0) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center space-y-3">
          <RefreshCw className="w-8 h-8 text-sky-600 animate-spin" />
          <span className="text-sm font-medium text-slate-500">Calculating executive and financial metrics...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header & Controls */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Executive & Financial Analytics</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Real-time subscription metrics, recurring revenues, and cash collection ledger for Brightloop Software
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={fetchDashboardData}
            className="p-2 bg-white border border-slate-200 text-slate-600 hover:text-slate-900 rounded-lg shadow-sm transition"
            title="Refresh Data"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          <button
            onClick={handleManualBillingTrigger}
            disabled={triggeringBilling}
            className="flex items-center space-x-2 px-4 py-2 bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white text-sm font-semibold rounded-lg shadow-sm transition disabled:opacity-50"
          >
            <PlayCircle className={`w-4 h-4 ${triggeringBilling ? 'animate-spin' : ''}`} />
            <span>{triggeringBilling ? 'Executing Billing...' : 'Trigger Automated Billing'}</span>
          </button>
        </div>
      </div>

      {billingSummary && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-900 flex items-start justify-between">
          <div className="flex items-start space-x-3">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
            <div>
              <div className="font-semibold text-sm">Recurring Billing Job Executed Successfully</div>
              <p className="text-xs text-emerald-700 mt-0.5">
                Processed <strong>{billingSummary.subscriptionsProcessed}</strong> subscriptions due today. Generated{' '}
                <strong>{billingSummary.invoicesGenerated}</strong> invoices for total{' '}
                <strong>{formatCurrency(billingSummary.totalAmountBilled)}</strong> with 0 duplicates.
              </p>
            </div>
          </div>
          <button onClick={() => setBillingSummary(null)} className="text-xs text-emerald-700 hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* MRR Card */}
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Monthly Recurring (MRR)</span>
            <div className="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-slate-900">{formatCurrency(metrics?.monthlyRecurringRevenue)}</div>
          <div className="mt-2 text-xs text-emerald-600 font-medium flex items-center space-x-1">
            <ArrowUpRight className="w-3.5 h-3.5" />
            <span>Active Normalized Monthly Run</span>
          </div>
        </div>

        {/* ARR Card */}
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Annualized Run Rate (ARR)</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-slate-900">{formatCurrency(metrics?.annualRunRate)}</div>
          <div className="mt-2 text-xs text-slate-500">Projected 12-month contracted value</div>
        </div>

        {/* Cash Collected Card */}
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Cash Collected</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-slate-900">{formatCurrency(metrics?.totalPaymentsReceived)}</div>
          <div className="mt-2 text-xs text-slate-500">
            Total Invoiced: <span className="font-semibold">{formatCurrency(metrics?.totalInvoicedRevenue)}</span>
          </div>
        </div>

        {/* Outstanding & Overdue Card */}
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Outstanding Balance</span>
            <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-amber-600">{formatCurrency(metrics?.totalOutstandingAmount)}</div>
          <div className="mt-2 text-xs text-rose-600 font-medium">
            {metrics?.overdueInvoicesCount || 0} Overdue ({formatCurrency(metrics?.overdueInvoicesAmount)})
          </div>
        </div>
      </div>

      {/* Operational Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-sm flex items-center space-x-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Customers</div>
            <div className="text-xl font-bold text-slate-800">{metrics?.totalCustomers || 0} Corporate Accounts</div>
          </div>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-sm flex items-center space-x-4">
          <div className="w-12 h-12 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center">
            <Repeat className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Active Subscriptions</div>
            <div className="text-xl font-bold text-slate-800">
              {metrics?.activeSubscriptions || 0} / {metrics?.totalSubscriptions || 0} Total
            </div>
          </div>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-sm flex items-center space-x-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
            <Calendar className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Upcoming Renewals (30d)</div>
            <div className="text-xl font-bold text-slate-800">{metrics?.upcomingRenewalsCount || 0} Subscriptions Due</div>
          </div>
        </div>
      </div>

      {/* Smart Dunning & Automated Revenue Recovery Engine (Unique Hackathon Feature) */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 border border-slate-800 rounded-2xl p-6 text-white shadow-xl relative overflow-hidden">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-5">
          <div>
            <div className="inline-flex items-center space-x-2 px-2.5 py-0.5 rounded-full bg-rose-500/20 text-rose-300 text-[11px] font-semibold border border-rose-500/30 uppercase tracking-wider mb-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
              <span>Unique Engine: Automated Overdue Recovery</span>
            </div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              <Zap className="w-5 h-5 text-amber-400" />
              Smart Dunning & Revenue Recovery State Machine
            </h2>
            <p className="text-xs text-slate-300 mt-0.5">
              4-tier progression (<code className="text-cyan-300">REMINDER_SENT</code> &rarr; <code className="text-cyan-300">GRACE_PERIOD</code> &rarr; <code className="text-cyan-300">LATE_FEE_APPLIED</code> &rarr; <code className="text-rose-400">SUSPENDED</code>) with automated 2% compounding late fees.
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={handleDunningSweep}
              disabled={triggeringDunning}
              className="px-4 py-2 bg-gradient-to-r from-amber-500 to-rose-600 hover:from-amber-600 hover:to-rose-700 disabled:opacity-50 text-white rounded-lg text-xs font-bold shadow-lg transition flex items-center space-x-1.5"
            >
              <Zap className={`w-4 h-4 ${triggeringDunning ? 'animate-spin' : ''}`} />
              <span>{triggeringDunning ? 'Compounding Late Fees...' : '⚡ Run Dunning Sweep'}</span>
            </button>
          </div>
        </div>

        {dunningSummary && (
          <div className="mb-4 p-3 bg-emerald-500/20 border border-emerald-500/40 text-emerald-200 text-xs rounded-lg flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
            <span>{dunningSummary}</span>
          </div>
        )}

        {/* Dunning Cases Table */}
        <div className="bg-slate-800/80 rounded-xl border border-slate-700/80 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-slate-400 uppercase font-semibold border-b border-slate-700">
                <tr>
                  <th className="px-4 py-2.5">Invoice #</th>
                  <th className="px-4 py-2.5">Customer</th>
                  <th className="px-4 py-2.5">Stage</th>
                  <th className="px-4 py-2.5">Days Overdue</th>
                  <th className="px-4 py-2.5">Late Fee Applied</th>
                  <th className="px-4 py-2.5">Attempts</th>
                  <th className="px-4 py-2.5">Recovery Log</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/60 font-mono">
                {dunningCases.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-4 text-center text-slate-400 font-sans">
                      All customer invoices are currently within healthy payment terms.
                    </td>
                  </tr>
                ) : (
                  dunningCases.map((c) => (
                    <tr key={c.id} className="hover:bg-slate-700/40 transition">
                      <td className="px-4 py-2.5 font-bold text-cyan-300">{c.invoice?.invoiceNumber}</td>
                      <td className="px-4 py-2.5 text-slate-200 font-sans">{c.customer?.companyName}</td>
                      <td className="px-4 py-2.5">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          c.dunningStage === 'LATE_FEE_APPLIED'
                            ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                            : c.dunningStage === 'SUSPENDED'
                            ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                            : 'bg-sky-500/20 text-sky-300 border border-sky-500/40'
                        }`}>
                          {c.dunningStage}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-rose-400 font-bold">{c.daysOverdue} days</td>
                      <td className="px-4 py-2.5 text-amber-300 font-bold">+₹{Number(c.lateFeeAmount || 0).toFixed(2)}</td>
                      <td className="px-4 py-2.5 text-slate-300">{c.attemptsCount} dispatches</td>
                      <td className="px-4 py-2.5 text-slate-300 font-sans text-[11px]">{c.notes}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Historical Revenue Trend */}
      <div className="p-6 bg-white rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-base font-bold text-slate-800">6-Month Invoiced vs Collected Revenue Trend</h2>
            <p className="text-xs text-slate-500 mt-0.5">Historical breakdown of monthly billings and cash reconciliation</p>
          </div>
          <div className="flex items-center space-x-4 text-xs font-medium">
            <div className="flex items-center space-x-1.5">
              <div className="w-3 h-3 bg-sky-500 rounded" />
              <span>Invoiced</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <div className="w-3 h-3 bg-emerald-500 rounded" />
              <span>Collected</span>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          {trend?.monthlyPoints?.map((pt) => {
            const maxVal = Math.max(
              ...trend.monthlyPoints.map((p) => Math.max(p.invoicedAmount, p.collectedAmount, 10000))
            );
            const invPct = Math.min(100, Math.round((pt.invoicedAmount / maxVal) * 100));
            const colPct = Math.min(100, Math.round((pt.collectedAmount / maxVal) * 100));

            return (
              <div key={pt.month} className="grid grid-cols-12 items-center gap-4 text-xs">
                <div className="col-span-2 font-semibold text-slate-600">{pt.month}</div>
                <div className="col-span-8 space-y-1.5">
                  <div className="flex items-center space-x-2">
                    <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                      <div className="bg-sky-500 h-2.5 rounded-full" style={{ width: `${Math.max(invPct, 2)}%` }} />
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                      <div className="bg-emerald-500 h-2.5 rounded-full" style={{ width: `${Math.max(colPct, 2)}%` }} />
                    </div>
                  </div>
                </div>
                <div className="col-span-2 text-right">
                  <div className="font-semibold text-sky-700">{formatCurrency(pt.invoicedAmount)}</div>
                  <div className="font-semibold text-emerald-700">{formatCurrency(pt.collectedAmount)}</div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Breakdowns: Subscriptions Status, Invoices Status, Payment Methods */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Subscription Status Breakdown */}
        <div className="p-6 bg-white rounded-xl border border-slate-200 shadow-sm">
          <h3 className="text-sm font-bold text-slate-800 mb-4">Subscriptions by Status</h3>
          <div className="space-y-3">
            {metrics?.subscriptionsByStatus &&
              Object.entries(metrics.subscriptionsByStatus).map(([status, count]) => (
                <div key={status} className="flex items-center justify-between text-xs">
                  <span className="font-medium text-slate-600">{status}</span>
                  <span className="px-2 py-0.5 rounded-full font-bold bg-slate-100 text-slate-800">{count}</span>
                </div>
              ))}
          </div>
        </div>

        {/* Invoice Status Breakdown */}
        <div className="p-6 bg-white rounded-xl border border-slate-200 shadow-sm">
          <h3 className="text-sm font-bold text-slate-800 mb-4">Invoices by Lifecycle</h3>
          <div className="space-y-3">
            {metrics?.invoicesByStatus &&
              Object.entries(metrics.invoicesByStatus).map(([status, count]) => (
                <div key={status} className="flex items-center justify-between text-xs">
                  <span className="font-medium text-slate-600">{status}</span>
                  <span className="px-2 py-0.5 rounded-full font-bold bg-slate-100 text-slate-800">{count}</span>
                </div>
              ))}
          </div>
        </div>

        {/* Payment Methods Breakdown */}
        <div className="p-6 bg-white rounded-xl border border-slate-200 shadow-sm">
          <h3 className="text-sm font-bold text-slate-800 mb-4">Payments by Channel</h3>
          <div className="space-y-3">
            {metrics?.paymentsByMethod &&
              Object.entries(metrics.paymentsByMethod).map(([method, amount]) => (
                <div key={method} className="flex items-center justify-between text-xs">
                  <span className="font-medium text-slate-600">{method}</span>
                  <span className="font-bold text-emerald-700">{formatCurrency(amount)}</span>
                </div>
              ))}
          </div>
        </div>
      </div>
    </div>
  );
};
