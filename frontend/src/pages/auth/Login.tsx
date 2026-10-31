import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { BrandLogo } from '../../components/common/BrandLogo';
import { Shield, Lock, Mail, ArrowRight, CheckCircle2 } from 'lucide-react';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const loggedUser = await login({ email, password });
      if (loggedUser.role === 'ROLE_CUSTOMER') {
        navigate('/portal');
      } else if (loggedUser.role === 'ROLE_STAFF') {
        navigate('/staff/dashboard');
      } else {
        navigate('/admin/dashboard');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoFill = (role: 'admin' | 'staff' | 'customer') => {
    if (role === 'admin') {
      setEmail('admin@brightloop.com');
      setPassword('Admin@12345');
    } else if (role === 'staff') {
      setEmail('staff@brightloop.com');
      setPassword('Staff@12345');
    } else {
      setEmail('customer@brightloop.com');
      setPassword('Customer@12345');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-indigo-950 flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl p-8 border border-slate-100">
        <div className="text-center mb-8 flex flex-col items-center">
          <BrandLogo size="lg" variant="light" className="mb-2" />
          <p className="text-xs text-slate-500 font-medium">Enterprise Subscription Management Platform</p>
        </div>

        {error && (
          <div className="mb-6 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-sm rounded-lg flex items-center space-x-2">
            <Shield className="w-4 h-4 text-rose-600 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Email Address
            </label>
            <div className="relative">
              <Mail className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@brightloop.com"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:bg-white transition"
              />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Password
              </label>
              <Link to="/forgot-password" className="text-xs text-sky-600 hover:underline">
                Forgot password?
              </Link>
            </div>
            <div className="relative">
              <Lock className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:bg-white transition"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-semibold rounded-lg shadow-md transition disabled:opacity-50 flex items-center justify-center space-x-2 text-sm mt-2"
          >
            <span>{loading ? 'Signing in...' : 'Sign In'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="mt-6 pt-6 border-t border-slate-100">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2 text-center">
            One-Click Demo Credentials
          </p>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => handleDemoFill('admin')}
              className="py-1.5 px-2 text-xs font-medium bg-purple-50 hover:bg-purple-100 text-purple-700 rounded-lg border border-purple-200 transition text-center"
            >
              Admin
            </button>
            <button
              type="button"
              onClick={() => handleDemoFill('staff')}
              className="py-1.5 px-2 text-xs font-medium bg-blue-50 hover:bg-blue-100 text-blue-700 rounded-lg border border-blue-200 transition text-center"
            >
              Staff
            </button>
            <button
              type="button"
              onClick={() => handleDemoFill('customer')}
              className="py-1.5 px-2 text-xs font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-lg border border-emerald-200 transition text-center"
            >
              Customer
            </button>
          </div>
        </div>

        <div className="mt-6 text-center text-xs text-slate-500">
          New enterprise customer?{' '}
          <Link to="/register" className="text-sky-600 font-semibold hover:underline">
            Register your company account
          </Link>
        </div>
      </div>
    </div>
  );
};
