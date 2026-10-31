import React, { useEffect, useState } from 'react';
import { staffApi } from '../../services/api';
import { User } from '../../types';
import { UserCheck, Plus, Shield, CheckCircle2, XCircle } from 'lucide-react';

export const StaffManagementPage: React.FC = () => {
  const [staffList, setStaffList] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });

  const loadStaff = async () => {
    setLoading(true);
    try {
      const res = await staffApi.getAll();
      setStaffList(res);
    } catch (err) {
      console.error('Failed to load staff accounts', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStaff();
  }, []);

  const handleCreateStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await staffApi.create(form);
      setShowAddModal(false);
      setForm({ firstName: '', lastName: '', email: '', password: '' });
      await loadStaff();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create staff account');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Staff Account Management</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Admin-restricted portal: provision operational staff accounts with day-to-day sales & invoicing rights
          </p>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center space-x-2 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-lg shadow-sm transition"
        >
          <Plus className="w-4 h-4" />
          <span>New Staff Member</span>
        </button>
      </div>

      {/* Staff Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
              <th className="py-3 px-4">Staff ID</th>
              <th className="py-3 px-4">Full Name</th>
              <th className="py-3 px-4">Corporate Email</th>
              <th className="py-3 px-4">System Role</th>
              <th className="py-3 px-4">Account Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {staffList.map((st) => (
              <tr key={st.id} className="hover:bg-slate-50/70">
                <td className="py-3.5 px-4 font-mono font-medium text-slate-500">#{st.id}</td>
                <td className="py-3.5 px-4 font-semibold text-slate-900">
                  {st.firstName} {st.lastName}
                </td>
                <td className="py-3.5 px-4 text-sky-700 font-medium">{st.email}</td>
                <td className="py-3.5 px-4">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 border border-blue-200">
                    ROLE_STAFF
                  </span>
                </td>
                <td className="py-3.5 px-4">
                  <span className="inline-flex items-center space-x-1 text-xs text-emerald-700 font-semibold">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Active</span>
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal: New Staff */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Provision Staff Account</h2>

            {error && (
              <div className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg">
                {error}
              </div>
            )}

            <form onSubmit={handleCreateStaff} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">First Name</label>
                  <input
                    type="text"
                    required
                    value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                    placeholder="Sarah"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Last Name</label>
                  <input
                    type="text"
                    required
                    value={form.lastName}
                    onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                    placeholder="Sales"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Corporate Email</label>
                <input
                  type="email"
                  required
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder="sarah.sales@brightloop.com"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Initial Password</label>
                <input
                  type="password"
                  required
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder="Staff@12345"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
                <span className="text-[11px] text-slate-400 mt-1 block">
                  Must be &gt;8 chars, 1 uppercase, 1 lowercase, 1 special character.
                </span>
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
                  Create Staff Account
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
