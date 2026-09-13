import React, { useState } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const OfficerRegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { setAuthData, isAuthenticated } = useAuth();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [department, setDepartment] = useState('Delhi PWD Central Circle');
  const [officerIdCode, setOfficerIdCode] = useState('');
  const [jurisdictionName, setJurisdictionName] = useState('Delhi Central Division');
  const [latitude, setLatitude] = useState('28.6200');
  const [longitude, setLongitude] = useState('77.2200');
  const [radiusKm, setRadiusKm] = useState('10.0');
  const [idCardFile, setIdCardFile] = useState<File | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (isAuthenticated) {
    return <Navigate to="/officer/dashboard" replace />;
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setIdCardFile(e.target.files[0]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!idCardFile) {
      setError('Please upload your official government ID card document.');
      return;
    }

    setLoading(true);

    try {
      const formData = new FormData();
      const officerData = {
        email,
        password,
        fullName,
        phone,
        department,
        officerIdCode,
        jurisdictionName,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        radiusKm: parseFloat(radiusKm)
      };

      formData.append('data', new Blob([JSON.stringify(officerData)], { type: 'application/json' }));
      formData.append('idCard', idCardFile);

      const response = await apiClient.registerOfficer(formData);
      setAuthData(response);
      navigate('/officer/dashboard');
    } catch (err: any) {
      setError(err.message || 'Officer registration failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card-container wider-lg">
      <div className="auth-solid-card">
        <div className="auth-card-header">
          <div className="auth-logo-badge">
            <img src="/logo.png" alt="PotholeX Logo" className="auth-logo-img" />
          </div>
          <h1 className="auth-card-title">
            Municipal Officer Registration
          </h1>
          <p className="auth-card-subtitle">
            Official Verification Required • Municipal Jurisdiction Assignment
          </p>
        </div>

        {error && <Alert variant="error" message={error} onDismiss={() => setError(null)} />}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="auth-section-title">
            1. Personal Credentials
          </div>

          <div className="auth-field-row-2">
            <div className="auth-field-group">
              <label className="auth-field-label">Full Name *</label>
              <input
                type="text"
                required
                placeholder="e.g. Officer Vikram Sharma"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="auth-input"
              />
            </div>
            <div className="auth-field-group">
              <label className="auth-field-label">Official Email *</label>
              <input
                type="email"
                required
                placeholder="officer@pwd.gov.in"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="auth-input"
              />
            </div>
          </div>

          <div className="auth-field-row-2">
            <div className="auth-field-group">
              <label className="auth-field-label">Phone Number</label>
              <input
                type="tel"
                placeholder="+91 9876543210"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="auth-input"
              />
            </div>
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
          </div>

          <div className="auth-section-title">
            2. Government Identification &amp; Document Verification
          </div>

          <div className="auth-field-row-2">
            <div className="auth-field-group">
              <label className="auth-field-label">Department *</label>
              <input
                type="text"
                required
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                className="auth-input"
              />
            </div>
            <div className="auth-field-group">
              <label className="auth-field-label">Officer ID Badge Code *</label>
              <input
                type="text"
                required
                placeholder="e.g. PWD-DL-8891"
                value={officerIdCode}
                onChange={(e) => setOfficerIdCode(e.target.value)}
                className="auth-input"
              />
            </div>
          </div>

          <div className="auth-field-group">
            <label className="auth-field-label">Upload Official ID Card (PDF or Image) *</label>
            <input
              type="file"
              required
              accept="image/*,.pdf"
              onChange={handleFileChange}
              className="auth-input-file"
            />
            <span className="auth-field-hint">
              🔒 Document is stored in secure encrypted storage. Access restricted to administration.
            </span>
          </div>

          <div className="auth-section-title">
            3. Municipal Jurisdiction Assignment
          </div>

          <div className="auth-field-group">
            <label className="auth-field-label">Jurisdiction Circle Name *</label>
            <input
              type="text"
              required
              value={jurisdictionName}
              onChange={(e) => setJurisdictionName(e.target.value)}
              className="auth-input"
            />
          </div>

          <div className="auth-field-row-3">
            <div className="auth-field-group">
              <label className="auth-field-label">Office Lat *</label>
              <input
                type="number"
                step="any"
                required
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                className="auth-input"
              />
            </div>
            <div className="auth-field-group">
              <label className="auth-field-label">Office Lng *</label>
              <input
                type="number"
                step="any"
                required
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                className="auth-input"
              />
            </div>
            <div className="auth-field-group">
              <label className="auth-field-label">Radius (km) *</label>
              <input
                type="number"
                step="0.5"
                required
                value={radiusKm}
                onChange={(e) => setRadiusKm(e.target.value)}
                className="auth-input"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="auth-submit-btn"
          >
            {loading ? <LoadingSpinner size={20} inline text="Submitting Officer Registration..." /> : 'Submit Officer Application'}
          </button>
        </form>

        <div className="auth-card-footer">
          <p className="auth-footer-prompt">Already registered? <Link to="/login" className="auth-link citizen">Sign In</Link></p>
        </div>
      </div>
    </div>
  );
};
