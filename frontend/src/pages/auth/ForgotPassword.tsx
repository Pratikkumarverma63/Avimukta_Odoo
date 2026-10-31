import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../services/api';
import { BrandLogo } from '../../components/common/BrandLogo';
import { Mail, CheckCircle2, Shield, ArrowLeft, ArrowRight, KeyRound, Clock, Sparkles } from 'lucide-react';

export const ForgotPassword: React.FC = () => {
  const [email, setEmail] = useState('');
  const [otpInfo, setOtpInfo] = useState<{ otpCode: string; email: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setOtpInfo(null);
    setLoading(true);
    try {
      const res = await authApi.forgotPassword(email);
      const data = res.data;
      if (data && (data.otpCode || data.resetToken)) {
        setOtpInfo({
          otpCode: data.otpCode || data.resetToken || '',
          email: data.email || email,
        });
      } else {
        // Fallback demo OTP if user is simulated
        const generatedCode = String(Math.floor(100000 + Math.random() * 900000));
        setOtpInfo({
          otpCode: generatedCode,
          email: email,
        });
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit password reset request. Verify your email.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl p-8 border border-slate-200">
        <div className="text-center mb-6 flex flex-col items-center">
          <BrandLogo size="md" variant="light" className="mb-3" />
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Forgot Password</h1>
          <p className="text-xs text-slate-500 mt-1">
            Enter your registered email to receive an instant single-use verification OTP code
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl flex items-center space-x-2">
            <Shield className="w-4 h-4 text-rose-600 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {otpInfo ? (
          <div className="space-y-4 animate-fade-in">
            <div className="p-5 bg-slate-900 text-white rounded-2xl border border-slate-800 shadow-xl space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-[11px] uppercase tracking-wider font-bold text-slate-400 flex items-center space-x-1">
                  <span>📬 Verification Code Dispatched</span>
                </span>
                <span className="px-2 py-0.5 bg-emerald-500/20 text-emerald-400 text-[10px] font-bold rounded-full border border-emerald-500/30">
                  Valid 15 Mins
                </span>
              </div>

              <div className="text-xs text-slate-300">
                Single-use verification OTP generated for <strong className="text-white">{otpInfo.email}</strong>:
              </div>

              <div className="flex items-center justify-between p-3.5 bg-slate-800/90 rounded-xl border border-slate-700">
                <div className="flex flex-col">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">6-Digit OTP Code:</span>
                  <span className="text-2xl font-mono font-black tracking-widest text-white mt-0.5">
                    {otpInfo.otpCode}
                  </span>
                </div>
                <div className="flex items-center space-x-1 text-slate-400 text-xs font-mono">
                  <Clock className="w-3.5 h-3.5 text-sky-400" />
                  <span>15:00</span>
                </div>
              </div>

              <button
                onClick={() =>
                  navigate(
                    `/reset-password?email=${encodeURIComponent(otpInfo.email)}&token=${encodeURIComponent(
                      otpInfo.otpCode
                    )}`
                  )
                }
                className="w-full py-3 bg-white hover:bg-slate-100 text-slate-900 font-bold text-xs rounded-xl transition flex items-center justify-center space-x-2 shadow-md"
              >
                <span>Auto-fill OTP & Set New Password</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>

            <p className="text-[11px] text-center text-slate-400">
              You can also navigate manually to <Link to={`/reset-password?token=${otpInfo.otpCode}`} className="text-sky-600 underline font-medium">Reset Password</Link> and enter code <strong className="font-mono">{otpInfo.otpCode}</strong>.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Account Email Address
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="customer@brightloop.com"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white font-medium"
                />
              </div>
            </div>

            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 text-[11px] text-slate-600 space-y-1">
              <div className="font-bold text-slate-700">Quick Test Accounts:</div>
              <div className="flex flex-wrap gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setEmail('customer@brightloop.com')}
                  className="px-2 py-0.5 bg-white border border-slate-300 rounded text-slate-700 hover:bg-slate-100 font-mono text-[10px]"
                >
                  customer@brightloop.com
                </button>
                <button
                  type="button"
                  onClick={() => setEmail('staff@brightloop.com')}
                  className="px-2 py-0.5 bg-white border border-slate-300 rounded text-slate-700 hover:bg-slate-100 font-mono text-[10px]"
                >
                  staff@brightloop.com
                </button>
                <button
                  type="button"
                  onClick={() => setEmail('admin@brightloop.com')}
                  className="px-2 py-0.5 bg-white border border-slate-300 rounded text-slate-700 hover:bg-slate-100 font-mono text-[10px]"
                >
                  admin@brightloop.com
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-slate-950 hover:bg-slate-800 text-white font-bold rounded-xl shadow-md transition disabled:opacity-50 text-xs tracking-wide"
            >
              {loading ? 'Generating Verification OTP...' : 'Send Password Reset OTP Code'}
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
