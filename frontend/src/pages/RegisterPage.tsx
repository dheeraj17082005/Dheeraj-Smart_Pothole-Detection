import React, { useState } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { setAuthData, isAuthenticated } = useAuth();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);

    try {
      const response = await apiClient.registerUser({
        email,
        password,
        fullName,
        phone
      });
      setAuthData(response);
      navigate('/');
    } catch (err: any) {
      setError(err.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card-container wider">
      <div className="auth-solid-card">
        <div className="auth-card-header">
          <div className="auth-logo-badge">
            <img src="/logo.png" alt="PotholeX Logo" className="auth-logo-img" />
          </div>
          <h1 className="auth-card-title">
            Citizen Registration
          </h1>
          <p className="auth-card-subtitle">
            Report road hazards and track repair status in real time
          </p>
        </div>

        {error && <Alert variant="error" message={error} onDismiss={() => setError(null)} />}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="auth-field-group">
            <label className="auth-field-label">Full Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Aman Kumar"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="auth-input"
            />
          </div>

          <div className="auth-field-group">
            <label className="auth-field-label">Email Address *</label>
            <input
              type="email"
              required
              placeholder="aman.kumar@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="auth-input"
            />
          </div>

          <div className="auth-field-group">
            <label className="auth-field-label">Phone Number (Optional)</label>
            <input
              type="tel"
              placeholder="+91 9876543210"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="auth-input"
            />
          </div>

          <div className="auth-field-row-2">
            <div className="auth-field-group">
              <label className="auth-field-label">Password *</label>
              <input
                type="password"
                required
                minLength={6}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="auth-input"
              />
            </div>
            <div className="auth-field-group">
              <label className="auth-field-label">Confirm Password *</label>
              <input
                type="password"
                required
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="auth-input"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="auth-submit-btn"
          >
            {loading ? <LoadingSpinner size={20} inline text="Registering..." /> : 'Register Citizen Account'}
          </button>
        </form>

        <div className="auth-card-footer">
          <p className="auth-footer-prompt">Already have an account? <Link to="/login" className="auth-link citizen">Sign In</Link></p>
          <p style={{ marginTop: '0.4rem' }}>Are you a municipal officer? <Link to="/officer/register" className="auth-link officer">Register Officer Profile</Link></p>
        </div>
      </div>
    </div>
  );
};
