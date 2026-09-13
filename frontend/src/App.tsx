import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { Layout } from './components/layout/Layout';
import { AuthLayout } from './components/layout/AuthLayout';

import { DashboardPage } from './pages/DashboardPage';
import { UploadPage } from './pages/UploadPage';
import { PotholesListPage } from './pages/PotholesListPage';
import { PotholeDetailPage } from './pages/PotholeDetailPage';
import { MapPage } from './pages/MapPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { OfficerRegisterPage } from './pages/OfficerRegisterPage';
import { OfficerDashboardPage } from './pages/OfficerDashboardPage';

const DashboardRouter: React.FC = () => {
  const { user } = useAuth();
  if (user?.role === 'ROLE_OFFICER') {
    return <OfficerDashboardPage />;
  }
  return <DashboardPage />;
};

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            {/* Public Auth Routes wrapped in AuthLayout */}
            <Route
              path="/login"
              element={
                <AuthLayout>
                  <LoginPage />
                </AuthLayout>
              }
            />
            <Route
              path="/register"
              element={
                <AuthLayout>
                  <RegisterPage />
                </AuthLayout>
              }
            />
            <Route
              path="/officer/register"
              element={
                <AuthLayout>
                  <OfficerRegisterPage />
                </AuthLayout>
              }
            />

            {/* Protected Application Routes wrapped in Layout & ProtectedRoute */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout>
                    <DashboardRouter />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Layout>
                    <DashboardRouter />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/officer/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ROLE_OFFICER']}>
                  <Layout>
                    <OfficerDashboardPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/potholes"
              element={
                <ProtectedRoute>
                  <Layout>
                    <PotholesListPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/potholes/:id"
              element={
                <ProtectedRoute>
                  <Layout>
                    <PotholeDetailPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/upload"
              element={
                <ProtectedRoute allowedRoles={['ROLE_USER']}>
                  <Layout>
                    <UploadPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/map"
              element={
                <ProtectedRoute>
                  <Layout>
                    <MapPage />
                  </Layout>
                </ProtectedRoute>
              }
            />

            {/* Fallback route */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
};

export default App;
