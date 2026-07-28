import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isNotVerified, setIsNotVerified] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsNotVerified(false);
    setLoading(true);
    try {
      await login(form.email, form.password);
      toast.success('Welcome back!');
      navigate('/');
    } catch (err) {
      const status = err.response?.status;
      const msg = err.response?.data?.message || 'Invalid email or password';
      if (status === 403 && err.response?.data?.error === 'EMAIL_NOT_VERIFIED') {
        setIsNotVerified(true);
      }
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-indigo-600">OfficeDesk</h1>
          <p className="text-gray-500 mt-2">Corporate Grievance & Support Platform</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Sign In</h2>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg mb-4">
              {error}
              {isNotVerified && (
                <div className="mt-2">
                  <Link to="/verify-email" state={{ email: form.email }}
                    className="text-indigo-600 hover:text-indigo-800 font-medium">
                    Resend verification code
                  </Link>
                </div>
              )}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email" required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="you@officedesk.com"
                value={form.email}
                onChange={e => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <input
                type="password" required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="Min 6 characters"
                value={form.password}
                onChange={e => setForm({ ...form, password: e.target.value })}
              />
              <div className="text-right mt-1">
                <Link to="/forgot-password" className="text-xs text-indigo-600 hover:text-indigo-800">
                  Forgot Password?
                </Link>
              </div>
            </div>
            <button
              type="submit" disabled={loading}
              className="w-full bg-indigo-600 text-white py-2.5 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <p className="text-sm text-gray-500 mt-4 text-center">
            Don't have an account?{' '}
            <Link to="/register" className="text-indigo-600 hover:text-indigo-800 font-medium">Register</Link>
          </p>
        </div>

        <div className="mt-6 bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <p className="text-xs font-medium text-gray-500 mb-2">Demo Accounts (password: pass123)</p>
          <div className="grid grid-cols-2 gap-2 text-xs text-gray-600">
            <div>rahul@officedesk.com <span className="text-gray-400">(Employee)</span></div>
            <div>vikram@officedesk.com <span className="text-gray-400">(Agent-IT)</span></div>
            <div>deepak@officedesk.com <span className="text-gray-400">(Head-IT)</span></div>
            <div>admin@officedesk.com <span className="text-gray-400">(Admin)</span></div>
          </div>
        </div>
      </div>
    </div>
  );
}
