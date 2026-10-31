import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Package,
  Layers,
  FileSpreadsheet,
  Repeat,
  Receipt,
  CreditCard,
  Percent,
  Users,
  ShieldCheck,
  UserCheck,
  Database,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { isAdmin, isStaff, isCustomer } = useAuth();

  const adminLinks = [
    { to: '/admin/dashboard', label: 'Finance & Analytics', icon: LayoutDashboard },
    { to: '/admin/products', label: 'Products & Variants', icon: Package },
    { to: '/admin/plans', label: 'Subscription Plans', icon: Layers },
    { to: '/admin/quotations', label: 'Quotes & Templates', icon: FileSpreadsheet },
    { to: '/admin/subscriptions', label: 'Subscriptions', icon: Repeat },
    { to: '/admin/invoices', label: 'Invoices & Billing', icon: Receipt },
    { to: '/admin/payments', label: 'Payment Ledger', icon: CreditCard },
    { to: '/admin/taxes-discounts', label: 'Taxes & Discounts', icon: Percent },
    { to: '/admin/database', label: 'Database & ERD', icon: Database },
  ];

  if (isAdmin) {
    adminLinks.push({ to: '/admin/staff', label: 'Staff Accounts', icon: UserCheck });
  }

  const staffLinks = [
    { to: '/staff/dashboard', label: 'Operations Hub', icon: LayoutDashboard },
    { to: '/admin/quotations', label: 'Quotes & Proposals', icon: FileSpreadsheet },
    { to: '/admin/subscriptions', label: 'Subscriptions Desk', icon: Repeat },
    { to: '/admin/invoices', label: 'Invoices & Billing', icon: Receipt },
    { to: '/admin/payments', label: 'Payment Collections', icon: CreditCard },
    { to: '/admin/products', label: 'Products Catalog', icon: Package },
    { to: '/admin/plans', label: 'Subscription Plans', icon: Layers },
  ];

  const customerLinks = [
    { to: '/portal', label: 'My Portal', icon: LayoutDashboard },
    { to: '/portal/subscriptions', label: 'My Subscriptions', icon: Repeat },
    { to: '/portal/invoices', label: 'My Invoices & Receipts', icon: Receipt },
  ];

  let links = adminLinks;
  let sectionTitle = 'Executive Administration';

  if (isCustomer) {
    links = customerLinks;
    sectionTitle = 'Customer Portal';
  } else if (isStaff) {
    links = staffLinks;
    sectionTitle = 'Operations & Servicing';
  }

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 min-h-[calc(100vh-4rem)] flex flex-col justify-between p-4 border-r border-slate-800">
      <div>
        <div className="px-3 py-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
          {sectionTitle}
        </div>
        <nav className="mt-2 space-y-1">
          {links.map((link) => {
            const Icon = link.icon;
            return (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                    isActive
                      ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30'
                      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                  }`
                }
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                <span>{link.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </div>

      <div className="p-3 bg-slate-800/60 rounded-xl border border-slate-700/50 text-xs">
        <div className="flex items-center space-x-2 text-sky-400 font-semibold mb-1">
          <ShieldCheck className="w-4 h-4" />
          <span>Brightloop Security</span>
        </div>
        <p className="text-slate-400">
          Strict role-based isolation & audit trail enabled for all subscription lifecycle events.
        </p>
      </div>
    </aside>
  );
};
