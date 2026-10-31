import React, { useState } from 'react';
import {
  Database,
  Table as TableIcon,
  Key,
  ShieldCheck,
  FileCode,
  Download,
  Copy,
  CheckCircle2,
  ExternalLink,
  Layers,
  Sparkles,
} from 'lucide-react';

interface ColumnDef {
  name: string;
  type: string;
  constraints: string;
  description: string;
}

interface TableDef {
  name: string;
  module: string;
  description: string;
  primaryKey: string;
  foreignKeys: string[];
  indexes: string[];
  columns: ColumnDef[];
}

export const DatabaseExplorerPage: React.FC = () => {
  const [selectedModule, setSelectedModule] = useState<string>('ALL');
  const [copied, setCopied] = useState(false);

  const modules = [
    'ALL',
    'Core Identity & Customers',
    'Product Catalog & Pricing',
    'Subscription & Contracts',
    'Billing Engine & Invoices',
    'Payments & Ledger',
    'Unique Hackathon Engines',
  ];

  const tables: TableDef[] = [
    {
      name: 'proration_records',
      module: 'Unique Hackathon Engines',
      description: 'Day-level pro-rata credit & charge ledger for mid-cycle plan upgrades, downgrades, and pauses.',
      primaryKey: 'id (BIGINT AUTO_INCREMENT)',
      foreignKeys: ['subscription_id -> subscriptions(id)', 'customer_id -> customers(id)'],
      indexes: ['idx_proration_sub', 'idx_proration_cust'],
      columns: [
        { name: 'id', type: 'BIGINT', constraints: 'PRIMARY KEY AUTO_INCREMENT', description: 'Surrogate primary key' },
        { name: 'subscription_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Associated subscription' },
        { name: 'customer_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Customer reference' },
        { name: 'change_type', type: 'VARCHAR(50)', constraints: 'NOT NULL', description: 'UPGRADE, DOWNGRADE, EARLY_TERMINATION' },
        { name: 'effective_date', type: 'DATE', constraints: 'NOT NULL', description: 'Calendar date adjustment takes effect' },
        { name: 'old_plan_price', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Prior contracted price' },
        { name: 'new_plan_price', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'New contracted price' },
        { name: 'days_used', type: 'INT', constraints: 'NOT NULL', description: 'Days consumed in active billing cycle' },
        { name: 'days_in_cycle', type: 'INT', constraints: 'NOT NULL', description: 'Total days in cadence (e.g. 30, 365)' },
        { name: 'prorated_credit_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL DEFAULT 0.00', description: 'Unutilized credit on old plan' },
        { name: 'prorated_charge_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL DEFAULT 0.00', description: 'Pro-rata charge on new plan' },
        { name: 'net_adjustment_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL DEFAULT 0.00', description: 'Net balance applied to invoice' },
      ],
    },
    {
      name: 'dunning_cases',
      module: 'Unique Hackathon Engines',
      description: 'Automated 4-tier revenue recovery state machine tracking overdue invoices and 2% compound late fees.',
      primaryKey: 'id (BIGINT AUTO_INCREMENT)',
      foreignKeys: ['invoice_id -> invoices(id)', 'customer_id -> customers(id)'],
      indexes: ['idx_dunning_invoice', 'idx_dunning_stage'],
      columns: [
        { name: 'id', type: 'BIGINT', constraints: 'PRIMARY KEY AUTO_INCREMENT', description: 'Surrogate primary key' },
        { name: 'invoice_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Delinquent invoice reference' },
        { name: 'customer_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Customer entity reference' },
        { name: 'dunning_stage', type: 'VARCHAR(50)', constraints: 'NOT NULL DEFAULT HEALTHY', description: 'REMINDER_SENT, GRACE_PERIOD, LATE_FEE_APPLIED, SUSPENDED' },
        { name: 'days_overdue', type: 'INT', constraints: 'NOT NULL DEFAULT 0', description: 'Days past invoice due_date' },
        { name: 'late_fee_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL DEFAULT 0.00', description: 'Compounded overdue penalty' },
        { name: 'last_action_date', type: 'TIMESTAMP', constraints: 'NULL', description: 'Timestamp of last recovery event' },
        { name: 'next_action_date', type: 'TIMESTAMP', constraints: 'NULL', description: 'Next scheduled auto-escalation' },
        { name: 'attempts_count', type: 'INT', constraints: 'NOT NULL DEFAULT 0', description: 'Number of recovery dispatches' },
        { name: 'notes', type: 'TEXT', constraints: 'NULL', description: 'Recovery trail audit log' },
      ],
    },
    {
      name: 'subscriptions',
      module: 'Subscription & Contracts',
      description: 'Core subscription contracts with lifecycle state machine (DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE -> PAUSED/CLOSED).',
      primaryKey: 'id (BIGINT AUTO_INCREMENT)',
      foreignKeys: ['customer_id -> customers(id)', 'plan_id -> plans(id)'],
      indexes: ['idx_sub_billing', 'idx_sub_cust', 'uq_subscription_number'],
      columns: [
        { name: 'id', type: 'BIGINT', constraints: 'PRIMARY KEY AUTO_INCREMENT', description: 'Primary ID' },
        { name: 'subscription_number', type: 'VARCHAR(50)', constraints: 'UNIQUE, NOT NULL', description: 'Formatted reference SUB-YYYY-XXXXX' },
        { name: 'customer_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Contracted client' },
        { name: 'plan_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Base subscription tier' },
        { name: 'status', type: 'VARCHAR(30)', constraints: 'NOT NULL', description: 'State machine status' },
        { name: 'start_date', type: 'DATE', constraints: 'NOT NULL', description: 'Contract initiation date' },
        { name: 'next_billing_date', type: 'DATE', constraints: 'NOT NULL', description: 'Automated scheduler execution trigger' },
        { name: 'last_billed_date', type: 'DATE', constraints: 'NULL', description: 'Most recent cycle processed' },
        { name: 'payment_terms', type: 'VARCHAR(50)', constraints: 'NOT NULL DEFAULT NET_30', description: 'Settlement window' },
        { name: 'total_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Periodic contract billing commitment' },
        { name: 'version', type: 'BIGINT', constraints: 'NOT NULL DEFAULT 0', description: 'Optimistic lock for concurrency safety' },
      ],
    },
    {
      name: 'invoices',
      module: 'Billing Engine & Invoices',
      description: 'Tax and discount calculated billing statements generated by the automated recurring engine.',
      primaryKey: 'id (BIGINT AUTO_INCREMENT)',
      foreignKeys: ['customer_id -> customers(id)', 'subscription_id -> subscriptions(id)'],
      indexes: ['idx_inv_status_due', 'idx_inv_cust', 'idx_inv_period', 'uq_invoice_number'],
      columns: [
        { name: 'id', type: 'BIGINT', constraints: 'PRIMARY KEY AUTO_INCREMENT', description: 'Primary ID' },
        { name: 'invoice_number', type: 'VARCHAR(50)', constraints: 'UNIQUE, NOT NULL', description: 'Unique ledger invoice number' },
        { name: 'customer_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Billed customer' },
        { name: 'subscription_id', type: 'BIGINT', constraints: 'NULL, FK', description: 'Originating subscription' },
        { name: 'invoice_date', type: 'DATE', constraints: 'NOT NULL', description: 'Issue date' },
        { name: 'due_date', type: 'DATE', constraints: 'NOT NULL', description: 'Net settlement deadline' },
        { name: 'status', type: 'VARCHAR(30)', constraints: 'NOT NULL', description: 'DRAFT, CONFIRMED, PAID' },
        { name: 'subtotal', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Sum before tax/discounts' },
        { name: 'tax_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Calculated GST/VAT' },
        { name: 'total_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Final payable' },
        { name: 'amount_paid', type: 'DECIMAL(14,2)', constraints: 'NOT NULL DEFAULT 0.00', description: 'Reconciled receipts' },
        { name: 'outstanding_amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Remaining balance' },
      ],
    },
    {
      name: 'payments',
      module: 'Payments & Ledger',
      description: 'Immutable financial reconciliation receipts across UPI, NEFT, RTGS, Cards, and Cash settlements.',
      primaryKey: 'id (BIGINT AUTO_INCREMENT)',
      foreignKeys: ['invoice_id -> invoices(id)'],
      indexes: ['idx_pay_inv', 'idx_pay_date', 'idx_pay_method', 'uq_payment_number'],
      columns: [
        { name: 'id', type: 'BIGINT', constraints: 'PRIMARY KEY AUTO_INCREMENT', description: 'Primary ID' },
        { name: 'payment_number', type: 'VARCHAR(50)', constraints: 'UNIQUE, NOT NULL', description: 'Immutable receipt reference' },
        { name: 'invoice_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Reconciled invoice' },
        { name: 'amount', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Exact settlement amount' },
        { name: 'payment_method', type: 'VARCHAR(40)', constraints: 'NOT NULL', description: 'UPI, BANK_TRANSFER, CARD, CASH' },
        { name: 'transaction_reference', type: 'VARCHAR(150)', constraints: 'NULL', description: 'Bank UTR / gateway transaction ID' },
        { name: 'payment_date', type: 'TIMESTAMP', constraints: 'NOT NULL', description: 'Timestamp of verified receipt' },
        { name: 'status', type: 'VARCHAR(30)', constraints: 'NOT NULL DEFAULT COMPLETED', description: 'COMPLETED or REVERSED' },
      ],
    },
    {
      name: 'products',
      module: 'Product Catalog & Pricing',
      description: 'Service and software catalog items with cost, sales price, and dynamic variant extensibility.',
      primaryKey: 'id (BIGINT AUTO_INCREMENT)',
      foreignKeys: [],
      indexes: ['idx_products_type', 'uq_product_code'],
      columns: [
        { name: 'id', type: 'BIGINT', constraints: 'PRIMARY KEY AUTO_INCREMENT', description: 'Primary ID' },
        { name: 'code', type: 'VARCHAR(50)', constraints: 'UNIQUE, NOT NULL', description: 'Product SKU/Code' },
        { name: 'name', type: 'VARCHAR(150)', constraints: 'NOT NULL', description: 'Display name' },
        { name: 'product_type', type: 'VARCHAR(50)', constraints: 'NOT NULL', description: 'SOFTWARE, SERVICE, ADD_ON' },
        { name: 'sales_price', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Base catalog unit price' },
        { name: 'cost_price', type: 'DECIMAL(14,2)', constraints: 'NOT NULL', description: 'Unit cost of delivery' },
        { name: 'active', type: 'BOOLEAN', constraints: 'NOT NULL DEFAULT TRUE', description: 'Catalog availability flag' },
      ],
    },
    {
      name: 'product_variants',
      module: 'Product Catalog & Pricing',
      description: 'Configurable price adjustments (e.g. Odoo brand edition adds ₹560, Enterprise adds ₹1500).',
      primaryKey: 'id (BIGINT AUTO_INCREMENT)',
      foreignKeys: ['product_id -> products(id)'],
      indexes: ['idx_pv_lookup', 'uq_variant_sku'],
      columns: [
        { name: 'id', type: 'BIGINT', constraints: 'PRIMARY KEY AUTO_INCREMENT', description: 'Primary ID' },
        { name: 'product_id', type: 'BIGINT', constraints: 'NOT NULL, FK', description: 'Parent product reference' },
        { name: 'attribute_name', type: 'VARCHAR(80)', constraints: 'NOT NULL', description: 'Brand, Tier, Edition' },
        { name: 'variant_value', type: 'VARCHAR(80)', constraints: 'NOT NULL', description: 'Standard, Odoo, Enterprise' },
        { name: 'price_adjustment', type: 'DECIMAL(14,2)', constraints: 'NOT NULL DEFAULT 0.00', description: 'Add-on delta amount' },
        { name: 'sku', type: 'VARCHAR(80)', constraints: 'UNIQUE', description: 'Variant SKU code' },
      ],
    },
  ];

  const filteredTables = selectedModule === 'ALL' ? tables : tables.filter((t) => t.module === selectedModule);

  const handleCopyCmd = () => {
    navigator.clipboard.writeText('mysql -u root -p brightloop_db < database/mysql_schema_and_seed.sql');
    setCopied(true);
    setTimeout(() => setCopied(false), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 border border-slate-800 rounded-2xl p-6 text-white shadow-xl relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-8 -mr-8 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-cyan-500/20 text-cyan-300 text-xs font-semibold uppercase tracking-wider border border-cyan-400/30 mb-2">
              <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
              <span>Production Relational Architecture</span>
            </div>
            <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2">
              <Database className="w-7 h-7 text-cyan-400" />
              Enterprise Database & ERD Schema Explorer
            </h1>
            <p className="text-xs text-slate-300 mt-1 max-w-2xl">
              25 normalized tables (3NF/BCNF) with strict financial precision (<code className="text-cyan-300">DECIMAL(14,2)</code>),
              foreign key integrity, day-level proration ledger, and an automated 4-tier dunning state machine.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <button
              onClick={handleCopyCmd}
              className="inline-flex items-center space-x-2 px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-lg text-xs font-medium border border-slate-700 shadow-sm transition"
            >
              {copied ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-cyan-400" />}
              <span>{copied ? 'Command Copied!' : 'Copy MySQL Import Command'}</span>
            </button>
            <a
              href="/database/mysql_schema_and_seed.sql"
              download
              className="inline-flex items-center space-x-2 px-3.5 py-2 bg-gradient-to-r from-cyan-500 to-sky-600 hover:from-cyan-600 hover:to-sky-700 text-white rounded-lg text-xs font-semibold shadow-md transition"
            >
              <Download className="w-4 h-4" />
              <span>Download MySQL 8.0 DDL</span>
            </a>
          </div>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Normalized Tables</div>
          <div className="text-2xl font-black text-slate-800 mt-1">25 Tables</div>
          <div className="text-[11px] text-emerald-600 font-medium mt-1">Third Normal Form (3NF / BCNF)</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Dual RDBMS Engines</div>
          <div className="text-2xl font-black text-slate-800 mt-1">MySQL + Postgres</div>
          <div className="text-[11px] text-sky-600 font-medium mt-1">Native Dialects + H2 Dev Mode</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Monetary Precision</div>
          <div className="text-2xl font-black text-slate-800 mt-1">DECIMAL(14,2)</div>
          <div className="text-[11px] text-purple-600 font-medium mt-1">Zero Floating-Point Error</div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Unique Engine Tables</div>
          <div className="text-2xl font-black text-slate-800 mt-1">Proration + Dunning</div>
          <div className="text-[11px] text-cyan-600 font-medium mt-1">Real-time Pro-rata & Recovery</div>
        </div>
      </div>

      {/* Module Filters */}
      <div className="flex items-center space-x-2 overflow-x-auto pb-2">
        <span className="text-xs font-semibold text-slate-500 uppercase mr-2 flex items-center">
          <Layers className="w-4 h-4 mr-1 text-slate-400" />
          Module:
        </span>
        {modules.map((m) => (
          <button
            key={m}
            onClick={() => setSelectedModule(m)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition whitespace-nowrap ${
              selectedModule === m
                ? 'bg-slate-900 text-white shadow-sm'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {m}
          </button>
        ))}
      </div>

      {/* Tables Explorer */}
      <div className="space-y-6">
        {filteredTables.map((tbl) => (
          <div key={tbl.name} className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            {/* Table Header */}
            <div className="bg-slate-50 border-b border-slate-200 px-6 py-4 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
              <div>
                <div className="flex items-center space-x-3">
                  <TableIcon className="w-5 h-5 text-sky-600" />
                  <h3 className="font-mono font-bold text-slate-900 text-base">{tbl.name}</h3>
                  <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-sky-100 text-sky-800 border border-sky-200">
                    {tbl.module}
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-1">{tbl.description}</p>
              </div>

              <div className="flex flex-wrap items-center gap-2 text-xs font-mono">
                <span className="px-2 py-1 bg-amber-50 text-amber-800 rounded border border-amber-200 flex items-center gap-1">
                  <Key className="w-3.5 h-3.5 text-amber-600" />
                  PK: {tbl.primaryKey}
                </span>
                {tbl.foreignKeys.length > 0 && (
                  <span className="px-2 py-1 bg-indigo-50 text-indigo-700 rounded border border-indigo-200">
                    FK: {tbl.foreignKeys.length} Relations
                  </span>
                )}
                {tbl.indexes.length > 0 && (
                  <span className="px-2 py-1 bg-emerald-50 text-emerald-700 rounded border border-emerald-200">
                    {tbl.indexes.length} Indexes
                  </span>
                )}
              </div>
            </div>

            {/* Table Columns List */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-100/75 text-slate-600 uppercase font-semibold border-b border-slate-200">
                  <tr>
                    <th className="px-6 py-3">Column Name</th>
                    <th className="px-6 py-3">Data Type</th>
                    <th className="px-6 py-3">Constraints & Modifiers</th>
                    <th className="px-6 py-3">Architectural Role</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 font-mono">
                  {tbl.columns.map((col) => (
                    <tr key={col.name} className="hover:bg-slate-50/75 transition">
                      <td className="px-6 py-2.5 font-bold text-slate-800 flex items-center space-x-1.5">
                        {col.constraints.includes('PRIMARY KEY') && <Key className="w-3.5 h-3.5 text-amber-500" />}
                        <span>{col.name}</span>
                      </td>
                      <td className="px-6 py-2.5 text-sky-700 font-semibold">{col.type}</td>
                      <td className="px-6 py-2.5">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-medium ${
                          col.constraints.includes('PRIMARY KEY')
                            ? 'bg-amber-100 text-amber-800 font-bold'
                            : col.constraints.includes('FK')
                            ? 'bg-indigo-100 text-indigo-800'
                            : 'bg-slate-100 text-slate-700'
                        }`}>
                          {col.constraints}
                        </span>
                      </td>
                      <td className="px-6 py-2.5 font-sans text-slate-600 text-xs">{col.description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* FK Details footer */}
            {tbl.foreignKeys.length > 0 && (
              <div className="px-6 py-2.5 bg-slate-50/50 border-t border-slate-100 text-[11px] text-slate-500 font-mono flex items-center space-x-2">
                <strong className="text-slate-700">Foreign Key Cascades:</strong>
                <span>{tbl.foreignKeys.join(' • ')}</span>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};
