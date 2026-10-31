import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Layout } from './components/layout/Layout';
import { Login } from './pages/auth/Login';
import { Register } from './pages/auth/Register';
import { ForgotPassword } from './pages/auth/ForgotPassword';
import { ResetPassword } from './pages/auth/ResetPassword';
import { AdminDashboard } from './pages/dashboard/AdminDashboard';
import { StaffDashboard } from './pages/dashboard/StaffDashboard';
import { ProductsPage } from './pages/products/ProductsPage';
import { PlansPage } from './pages/plans/PlansPage';
import { QuotationsPage } from './pages/quotations/QuotationsPage';
import { SubscriptionsPage } from './pages/subscriptions/SubscriptionsPage';
import { InvoicesPage } from './pages/invoices/InvoicesPage';
import { PaymentsPage } from './pages/payments/PaymentsPage';
import { TaxesAndDiscountsPage } from './pages/taxes-discounts/TaxesAndDiscountsPage';
import { StaffManagementPage } from './pages/staff/StaffManagementPage';
import { DatabaseExplorerPage } from './pages/database/DatabaseExplorerPage';
import { CustomerPortal } from './pages/portal/CustomerPortal';
import { Role } from './types';

const ProtectedRoute: React.FC<{
  children: React.ReactNode;
  allowedRoles?: Role[];
}> = ({ children, allowedRoles }) => {
  const { user, token, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white text-sm">
        Authenticating session...
      </div>
    );
  }

  if (!token || !user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    if (user.role === 'ROLE_CUSTOMER') {
      return <Navigate to="/portal" replace />;
    }
    if (user.role === 'ROLE_STAFF') {
      return <Navigate to="/staff/dashboard" replace />;
    }
    return <Navigate to="/admin/dashboard" replace />;
  }

  return <>{children}</>;
};

const RootRedirect: React.FC = () => {
  const { user, token, loading } = useAuth();

  if (loading) return null;
  if (!token || !user) return <Navigate to="/login" replace />;

  if (user.role === 'ROLE_CUSTOMER') {
    return <Navigate to="/portal" replace />;
  }
  if (user.role === 'ROLE_STAFF') {
    return <Navigate to="/staff/dashboard" replace />;
  }
  return <Navigate to="/admin/dashboard" replace />;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Auth Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          {/* Root Redirect */}
          <Route path="/" element={<RootRedirect />} />

          {/* Admin & Staff App */}
          <Route
            element={
              <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_STAFF']}>
                <Layout />
              </ProtectedRoute>
            }
          >
            {/* Dedicated Dashboards */}
            <Route
              path="/admin/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <AdminDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/staff/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STAFF', 'ROLE_ADMIN']}>
                  <StaffDashboard />
                </ProtectedRoute>
              }
            />

            {/* Shared Operations */}
            <Route path="/admin/products" element={<ProductsPage />} />
            <Route path="/admin/plans" element={<PlansPage />} />
            <Route path="/admin/quotations" element={<QuotationsPage />} />
            <Route path="/admin/subscriptions" element={<SubscriptionsPage />} />
            <Route path="/admin/invoices" element={<InvoicesPage />} />
            <Route path="/admin/payments" element={<PaymentsPage />} />

            {/* Admin Only Modules */}
            <Route
              path="/admin/taxes-discounts"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <TaxesAndDiscountsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/staff"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <StaffManagementPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/database"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <DatabaseExplorerPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Customer Portal App */}
          <Route
            element={
              <ProtectedRoute allowedRoles={['ROLE_CUSTOMER']}>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/portal" element={<CustomerPortal />} />
            <Route path="/portal/subscriptions" element={<CustomerPortal />} />
            <Route path="/portal/invoices" element={<CustomerPortal />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
