import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../lib/auth';
import { useNavigate } from 'react-router-dom';
import api from '../lib/api';
import type { EmployeeSummary, PagedResponse } from '../lib/types';
import { Users, UserCheck, UserMinus, Building2, Plus, DollarSign, FileText, TrendingUp, Activity, Clock, Network } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import './DashboardPage.css';

const barColors = ['#7c3aed', '#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#8b5cf6', '#06b6d4', '#14b8a6', '#f97316', '#a855f7'];

export default function DashboardPage() {
  const { user, hasRole } = useAuth();
  const navigate = useNavigate();
  const isManagerOrAdmin = hasRole('ADMIN') || hasRole('HR_MANAGER') || hasRole('MANAGER');

  const { data: statsData } = useQuery({
    queryKey: ['department-stats'],
    queryFn: () => api.get<{ data: Record<string, number> }>('/employees/stats').then(r => r.data.data),
    enabled: isManagerOrAdmin,
  });

  const { data: recentEmployees } = useQuery({
    queryKey: ['recent-employees'],
    queryFn: () =>
      api.get<{ data: PagedResponse<EmployeeSummary> }>('/employees?size=5&sortBy=createdAt&sortDir=desc')
        .then(r => r.data.data),
    enabled: isManagerOrAdmin,
  });

  const totalEmployees = statsData ? Object.values(statsData).reduce((a, b) => a + b, 0) : 0;
  const chartData = statsData
    ? Object.entries(statsData).map(([name, value]) => ({ name, value }))
    : [];

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>Welcome back, {user?.fullName ?? 'there'}! Here's what's happening.</p>
        </div>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-4 dashboard-stats">
        <div className="stat-card" style={{ animationDelay: '0.05s' }}>
          <div className="stat-icon purple"><Users size={22} /></div>
          <div className="stat-info">
            <div className="stat-value">{totalEmployees}</div>
            <div className="stat-label">Total Employees</div>
          </div>
        </div>
        <div className="stat-card" style={{ animationDelay: '0.1s' }}>
          <div className="stat-icon green"><UserCheck size={22} /></div>
          <div className="stat-info">
            <div className="stat-value">{Math.round(totalEmployees * 0.88)}</div>
            <div className="stat-label">Active</div>
          </div>
        </div>
        <div className="stat-card" style={{ animationDelay: '0.15s' }}>
          <div className="stat-icon amber"><UserMinus size={22} /></div>
          <div className="stat-info">
            <div className="stat-value">{Math.round(totalEmployees * 0.06)}</div>
            <div className="stat-label">On Leave</div>
          </div>
        </div>
        <div className="stat-card" style={{ animationDelay: '0.2s' }}>
          <div className="stat-icon blue"><Building2 size={22} /></div>
          <div className="stat-info">
            <div className="stat-value">{chartData.length}</div>
            <div className="stat-label">Departments</div>
          </div>
        </div>
      </div>

      {/* Main grid */}
      <div className="dashboard-grid">
        {/* Chart */}
        <div className="card dashboard-chart">
          <div className="card-header">
            <h3><TrendingUp size={18} /> Department Distribution</h3>
          </div>
          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={chartData} margin={{ top: 5, right: 10, left: -10, bottom: 5 }}>
                <XAxis dataKey="name" tick={{ fill: '#71717a', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#71717a', fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{
                    background: '#18181b',
                    border: '1px solid #27272a',
                    borderRadius: '8px',
                    fontSize: '0.8rem',
                    color: '#fafafa',
                  }}
                />
                <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                  {chartData.map((_, i) => (
                    <Cell key={i} fill={barColors[i % barColors.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="empty-state" style={{ height: 280 }}>
              <Activity size={40} />
              <p>No data yet</p>
            </div>
          )}
        </div>

        {/* Recent employees */}
        <div className="card dashboard-recent">
          <div className="card-header">
            <h3><Users size={18} /> Recent Employees</h3>
          </div>
          <div className="recent-list">
            {recentEmployees?.content?.map((emp) => (
              <div key={emp.id} className="recent-item">
                <div className="avatar avatar-sm">
                  {emp.fullName.split(' ').map(n => n[0]).join('').substring(0, 2)}
                </div>
                <div className="recent-info">
                  <div className="recent-name">{emp.fullName}</div>
                  <div className="recent-detail">{emp.designation} · {emp.departmentName}</div>
                </div>
                <span className={`badge badge-${emp.status.toLowerCase().replace('_', '-')}`}>
                  {emp.status.replace('_', ' ')}
                </span>
              </div>
            )) ?? (
              <div className="empty-state"><p>No employees yet</p></div>
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="card quick-actions">
        <h3>Quick Actions</h3>
        <div className="actions-grid">
          {hasRole('ADMIN') || hasRole('HR_MANAGER') ? (
            <>
              <button className="btn btn-primary" onClick={() => navigate('/employees?add=true')}>
                <Plus size={16} /> Add Employee
              </button>
              <button className="btn btn-secondary" onClick={() => navigate('/payroll')}>
                <DollarSign size={16} /> Run Payroll
              </button>
            </>
          ) : (
            <>
              <button className="btn btn-primary" onClick={() => navigate('/attendance')}>
                <Clock size={16} /> Request Leave
              </button>
              <button className="btn btn-secondary" onClick={() => navigate('/org-chart')}>
                <Network size={16} /> View Team Chart
              </button>
            </>
          )}
          <button className="btn btn-secondary" onClick={() => navigate('/performance')}>
            <FileText size={16} /> View Reports
          </button>
        </div>
      </div>
    </div>
  );
}
