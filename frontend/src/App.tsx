import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { Analytics } from '@vercel/analytics/react';
import { AuthProvider } from './lib/auth';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import EmployeesPage from './pages/EmployeesPage';
import EmployeeDetailPage from './pages/EmployeeDetailPage';
import DepartmentsPage from './pages/DepartmentsPage';
import OrgChartPage from './pages/OrgChartPage';
import AttendancePage from './pages/AttendancePage';
import PayrollPage from './pages/PayrollPage';
import PerformancePage from './pages/PerformancePage';
import AiPage from './pages/AiPage';
import SettingsPage from './pages/SettingsPage';
import SignupPage from './pages/SignupPage';
import AnnouncementsPage from './pages/AnnouncementsPage';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route index element={<DashboardPage />} />
              <Route path="employees" element={<EmployeesPage />} />
              <Route path="employees/:id" element={<EmployeeDetailPage />} />
              <Route path="departments" element={<DepartmentsPage />} />
              <Route path="org-chart" element={<OrgChartPage />} />
              <Route path="attendance" element={<AttendancePage />} />
              <Route path="payroll" element={<PayrollPage />} />
              <Route path="performance" element={<PerformancePage />} />
              <Route path="announcements" element={<AnnouncementsPage />} />
              <Route path="ai" element={<AiPage />} />
              <Route path="settings" element={<SettingsPage />} />
            </Route>
          </Routes>
          <Toaster
            position="top-right"
            toastOptions={{
              style: {
                background: '#18181b',
                color: '#fafafa',
                border: '1px solid #27272a',
                borderRadius: '10px',
                fontSize: '0.875rem',
              },
            }}
          />
          <Analytics />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
