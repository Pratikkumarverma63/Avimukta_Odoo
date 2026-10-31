import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { billingApi } from '../../services/api';
import { BrandLogo } from '../common/BrandLogo';
import {
  Bell,
  LogOut,
  PlayCircle,
  Shield,
  User as UserIcon,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout, isAdmin, isStaff } = useAuth();
  const [billingRunning, setBillingRunning] = useState(false);
  const [billingResult, setBillingResult] = useState<string | null>(null);

  const handleTriggerBilling = async () => {
    if (!confirm('Run automated recurring billing now? This will identify subscriptions due today and generate invoices.')) {
      return;
    }
    setBillingRunning(true);
    setBillingResult(null);
    try {
      const summary = await billingApi.triggerRun();
      setBillingResult(`Billing executed: ${summary.invoicesGenerated} invoices generated, ₹${summary.totalAmountBilled} billed.`);
      setTimeout(() => setBillingResult(null), 5000);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to execute billing run');
    } finally {
      setBillingRunning(false);
    }
  };

  const getRoleBadge = (role?: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-purple-100 text-purple-700 border border-purple-200">Admin</span>;
      case 'ROLE_STAFF':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-blue-100 text-blue-700 border border-blue-200">Staff</span>;
      default:
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-700 border border-emerald-200">Customer</span>;
    }
  };

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between sticky top-0 z-30 shadow-sm">
      <div className="flex items-center space-x-4">
        <BrandLogo size="sm" variant="light" />
      </div>

      <div className="flex items-center space-x-4">
        {billingResult && (
          <div className="flex items-center space-x-1.5 text-xs bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-lg border border-emerald-200 animate-pulse">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>{billingResult}</span>
          </div>
        )}

        {isAdmin && (
          <button
            onClick={handleTriggerBilling}
            disabled={billingRunning}
            className="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 rounded-lg shadow-sm transition disabled:opacity-50"
            title="Execute automated recurring billing scheduler now (Admin Only)"
          >
            <PlayCircle className={`w-4 h-4 ${billingRunning ? 'animate-spin' : ''}`} />
            <span>{billingRunning ? 'Running Engine...' : 'Run Billing Job'}</span>
          </button>
        )}

        {isStaff && (
          <span className="hidden sm:inline-flex items-center space-x-1 px-3 py-1 rounded-lg bg-sky-50 border border-sky-200 text-sky-700 text-xs font-semibold">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span>Staff Operations Desk</span>
          </span>
        )}

        <div className="h-6 w-px bg-slate-200 mx-1" />

        <div className="flex items-center space-x-3">
          <div className="text-right">
            <div className="text-sm font-semibold text-slate-800 flex items-center justify-end space-x-2">
              <span>{user?.firstName} {user?.lastName}</span>
              {getRoleBadge(user?.role)}
            </div>
            <div className="text-xs text-slate-500">{user?.email}</div>
          </div>

          <button
            onClick={logout}
            className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
            title="Sign out"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
