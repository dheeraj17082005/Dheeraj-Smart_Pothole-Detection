import React, { useState } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login, isAuthenticated, user } = useAuth();

  const [activeTab, setActiveTab] = useState<'citizen' | 'officer'>('citizen');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // If already logged in, redirect based on role
  if (isAuthenticated && user) {
    if (user.role === 'ROLE_OFFICER') {
      return <Navigate to="/officer/dashboard" replace />;
    }
    return <Navigate to="/" replace />;
  }

  const handleFillDemoCitizen = () => {
    setActiveTab('citizen');
    setEmail('aman.kumar@example.com');
    setPassword('password123');
    setError(null);
  };

  const handleFillDemoOfficer = () => {
    setActiveTab('officer');
    setEmail('officer.sharma@delhipwd.gov.in');
    setPassword('officerPass123');
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await login({ email, password });
      if (response.role === 'ROLE_OFFICER') {
        navigate('/officer/dashboard');
      } else {
        navigate('/');
      }
    } catch (err: any) {
      setError(err.message || 'Authentication failed. Please check your email and password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card-container">
      <div className="auth-solid-card">
        {/* Card Header & Brand Icon */}
        <div className="auth-card-header">
          <div className="auth-logo-badge">
            <img src="/logo.png" alt="PotholeX Logo" className="auth-logo-img" />
          </div>
          <h1 className="auth-card-title">
            Sign In to <span className="brand-accent-text">PotholeX</span>
          </h1>
          <p className="auth-card-subtitle">
            Municipal Road Maintenance &amp; Public Works Portal
          </p>
        </div>

        {/* Quick Demo Credentials Bar */}
        <div className="auth-demo-banner">
          <span className="demo-banner-label">Demo Quick Fill:</span>
          <div className="demo-pills-row">
            <button
              type="button"
              className={`demo-pill ${activeTab === 'citizen' ? 'active-citizen' : ''}`}
              onClick={handleFillDemoCitizen}
            >
              <svg className="pill-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span>Citizen Account</span>
            </button>
            <button
              type="button"
              className={`demo-pill ${activeTab === 'officer' ? 'active-officer' : ''}`}
              onClick={handleFillDemoOfficer}
            >
              <svg className="pill-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
              <span>Municipal Officer</span>
            </button>
          </div>
        </div>

        {error && <Alert variant="error" message={error} onDismiss={() => setError(null)} />}

        {/* Role Selector Tabs */}
        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${activeTab === 'citizen' ? 'active' : ''}`}
            onClick={() => setActiveTab('citizen')}
          >
            <span>Citizen Portal</span>
          </button>
          <button
            type="button"
            className={`auth-tab ${activeTab === 'officer' ? 'active' : ''}`}
            onClick={() => setActiveTab('officer')}
          >
            <span>Municipal Officer</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          {/* Email Input */}
          <div className="auth-field-group">
            <label className="auth-field-label">
              {activeTab === 'officer' ? 'Official Government Email' : 'Email Address'}
            </label>
            <div className="auth-input-wrapper">
              <svg className="auth-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
              <input
                type="email"
                required
                placeholder={activeTab === 'officer' ? 'officer@pwd.gov.in' : 'aman.kumar@example.com'}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="auth-input"
              />
            </div>
          </div>

          {/* Password Input */}
          <div className="auth-field-group">
            <div className="auth-label-row">
              <label className="auth-field-label">Password</label>
            </div>
            <div className="auth-input-wrapper">
              <svg className="auth-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <input
                type={showPassword ? 'text' : 'password'}
                required
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="auth-input"
              />
              <button
                type="button"
                className="auth-toggle-pwd"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
              >
                {showPassword ? (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                    <line x1="1" y1="1" x2="23" y2="23" />
                  </svg>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                )}
              </button>
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="auth-submit-btn"
          >
            {loading ? (
              <LoadingSpinner size={20} inline text="Signing In..." />
            ) : (
              <>
                <span>Sign In</span>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </>
            )}
          </button>
        </form>

        {/* Footer Registration Links */}
        <div className="auth-card-footer">
          <p className="auth-footer-prompt">Don't have an account?</p>
          <div className="auth-register-links">
            <Link to="/register" className="auth-link citizen">
              Register Citizen Account
            </Link>
            <span className="auth-divider">&bull;</span>
            <Link to="/officer/register" className="auth-link officer">
              Register Municipal Officer
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
