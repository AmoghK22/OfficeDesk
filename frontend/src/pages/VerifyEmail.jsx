import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import api from '../api/api';

export default function VerifyEmail() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [email, setEmail] = useState(location.state?.email || sessionStorage.getItem('verifyEmail') || '');
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [success, setSuccess] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const [showEmailInput, setShowEmailInput] = useState(!location.state?.email && !sessionStorage.getItem('verifyEmail'));

  useEffect(() => {
    if (!email && !showEmailInput) {
      setShowEmailInput(true);
    }
  }, [email, showEmailInput]);

  useEffect(() => {
    if (resendTimer > 0) {
      const timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendTimer]);

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/auth/verify-email', { email, code });
      sessionStorage.removeItem('verifyEmail');
      toast.success(res.data.message);
      setSuccess(true);
    } catch (err) {
      const msg = err.response?.data?.message || 'Verification failed';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setResending(true);
    try {
      const res = await api.post('/auth/resend-verification', { email });
      toast.success(res.data.message);
      setResendTimer(60);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to resend code';
      setError(msg);
      toast.error(msg);
    } finally {
      setResending(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="max-w-md w-full">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-indigo-600">OfficeDesk</h1>
          </div>
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Email Verified!</h2>
            <p className="text-gray-500 text-sm mb-6">Your email has been verified successfully.</p>
            <Link to="/login"
              className="inline-block bg-indigo-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors">
              Go to Sign In
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-indigo-600">OfficeDesk</h1>
          <p className="text-gray-500 mt-2">Verify your email address</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <div className="w-16 h-16 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>

          {showEmailInput ? (
            <>
              <h2 className="text-xl font-semibold text-gray-900 mb-2 text-center">Enter Your Email</h2>
              <p className="text-gray-500 text-sm text-center mb-6">
                Enter the email address you used to register
              </p>
              <form onSubmit={(e) => { e.preventDefault(); setShowEmailInput(false); sessionStorage.setItem('verifyEmail', email); }}
                className="space-y-4">
                <input type="email" required value={email} onChange={e => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                <button type="submit"
                  className="w-full bg-indigo-600 text-white py-2.5 rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors">
                  Continue
                </button>
              </form>
            </>
          ) : (
            <>
              <h2 className="text-xl font-semibold text-gray-900 mb-2 text-center">Check Your Email</h2>
              <p className="text-gray-500 text-sm text-center mb-6">
                We sent a 6-digit verification code to<br />
                <span className="font-medium text-gray-700">{email}</span>
                {' '}
                <button onClick={() => setShowEmailInput(true)} className="text-indigo-600 hover:text-indigo-800 text-xs font-medium">
                  (change)
                </button>
              </p>

              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg mb-4">
                  {error}
                </div>
              )}

              <form onSubmit={handleVerify} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Verification Code</label>
                  <input
                    type="text" required maxLength={6} pattern="[0-9]{6}"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm text-center text-2xl tracking-[0.5em] font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    placeholder="000000"
                    value={code}
                    onChange={e => setCode(e.target.value.replace(/\D/g, ''))}
                    autoFocus
                  />
                </div>
                <button type="submit" disabled={loading || code.length !== 6}
                  className="w-full bg-indigo-600 text-white py-2.5 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 transition-colors">
                  {loading ? 'Verifying...' : 'Verify Email'}
                </button>
              </form>

              <div className="mt-4 text-center">
                <p className="text-sm text-gray-500">
                  Didn't receive the code?{' '}
                  {resendTimer > 0 ? (
                    <span className="text-gray-400">Resend in {resendTimer}s</span>
                  ) : (
                    <button onClick={handleResend} disabled={resending}
                      className="text-indigo-600 hover:text-indigo-800 font-medium disabled:opacity-50">
                      {resending ? 'Sending...' : 'Resend Code'}
                    </button>
                  )}
                </p>
              </div>
            </>
          )}

          <p className="text-sm text-gray-500 mt-4 text-center">
            <Link to="/register" className="text-indigo-600 hover:text-indigo-800 font-medium">
              Back to Register
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
