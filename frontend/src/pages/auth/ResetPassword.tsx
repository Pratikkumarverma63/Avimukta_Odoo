import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { authApi } from '../../services/api';
import { BrandLogo } from '../../components/common/BrandLogo';
import { KeyRound, Lock, CheckCircle2, Shield, ArrowLeft, Check, Sparkles } from 'lucide-react';

export const ResetPassword: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const urlToken = searchParams.get('token') || searchParams.get('otp') || searchParams.get('code');
    if (urlToken) {
      setToken(urlToken.trim());
    }
  }, [searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword({ token: token.trim(), newPassword });
      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 2500);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid or expired 6-digit OTP verification code');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl p-8 border border-slate-200">
        <div className="text-center mb-6 flex flex-col items-center">
          <BrandLogo size="md" variant="light" className="mb-3" />
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Set New Password</h1>
          <p className="text-xs text-slate-500 mt-1">Provide your 6-digit verification OTP code and choose a secure password</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl flex items-center space-x-2">
            <Shield className="w-4 h-4 text-rose-600 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {success ? (
          <div className="p-6 bg-emerald-50 border border-emerald-300 rounded-2xl text-center space-y-2 animate-fade-in">
            <CheckCircle2 className="w-10 h-10 text-emerald-600 mx-auto" />
            <div className="font-extrabold text-slate-900 text-base">Password Reset Successfully!</div>
            <p className="text-xs text-emerald-700">Your account credentials have been updated. Redirecting to sign in...</p>
            <div className="pt-2">
              <Link to="/login" className="inline-block px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-md transition">
                Sign in now &rarr;
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                  6-Digit Verification OTP Code
                </label>
                {token && token.length === 6 && (
                  <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 flex items-center space-x-1">
                    <Check className="w-3 h-3" />
                    <span>OTP Loaded</span>
                  </span>
                )}
              </div>
              <div className="relative">
                <KeyRound className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="e.g. 849201"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-base text-slate-900 font-mono font-bold tracking-widest focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white text-center"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                New Password
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Min 8 chars (1 uppercase, 1 symbol)"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Confirm New Password
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter password"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-slate-950 hover:bg-slate-800 text-white font-bold rounded-xl shadow-md transition disabled:opacity-50 text-xs tracking-wide"
            >
              {loading ? 'Updating Password...' : 'Reset Password & Secure Account'}
            </button>
          </form>
        )}

        <div className="mt-6 text-center">
          <Link to="/login" className="inline-flex items-center space-x-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900">
            <ArrowLeft className="w-4 h-4" />
            <span>Back to sign in</span>
          </Link>
        </div>
      </div>
    </div>
  );
};
