import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../lib/api';
import type { Employee, EmployeeSummary } from '../lib/types';
import { ArrowLeft, Mail, Phone, MapPin, Calendar, Briefcase, DollarSign, User } from 'lucide-react';
import './EmployeeDetailPage.css';

export default function EmployeeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('profile');

  const { data: employee, isLoading } = useQuery({
    queryKey: ['employee', id],
    queryFn: () => api.get<{ data: Employee }>(`/employees/${id}`).then(r => r.data.data),
    enabled: !!id,
  });

  const { data: reports } = useQuery({
    queryKey: ['reports', id],
    queryFn: () => api.get<{ data: EmployeeSummary[] }>(`/employees/${id}/reports`).then(r => r.data.data),
    enabled: activeTab === 'reports' && !!id,
  });

  if (isLoading) {
    return (
      <div className="page-content">
        <div style={{ display: 'flex', justifyContent: 'center', padding: '3rem' }}>
          <div className="spinner" />
        </div>
      </div>
    );
  }

  if (!employee) {
    return (
      <div className="page-content">
        <div className="empty-state"><p>Employee not found</p></div>
      </div>
    );
  }

  const statusClass = `badge badge-${employee.status.toLowerCase().replace('_', '-')}`;
  const initials = `${employee.firstName[0]}${employee.lastName[0]}`;

  return (
    <div className="page-content">
      <button className="btn btn-ghost" onClick={() => navigate('/employees')}>
        <ArrowLeft size={16} /> Back to Employees
      </button>

      {/* Hero */}
      <div className="employee-hero card">
        <div className="hero-left">
          <div className="avatar avatar-lg">{initials}</div>
          <div>
            <h1>{employee.fullName}</h1>
            <p className="hero-meta">{employee.designation} · {employee.departmentName}</p>
            <span className={statusClass}>{employee.status.replace('_', ' ')}</span>
          </div>
        </div>
        <div className="hero-code">{employee.empCode}</div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        {['profile', 'skills', 'reports'].map(tab => (
          <button
            key={tab}
            className={`tab ${activeTab === tab ? 'active' : ''}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {activeTab === 'profile' && (
        <div className="detail-grid">
          <div className="card detail-section">
            <h3>Personal Information</h3>
            <div className="info-list">
              <InfoRow icon={<Mail size={16} />} label="Email" value={employee.email} />
              <InfoRow icon={<Phone size={16} />} label="Phone" value={employee.phone || '—'} />
              <InfoRow icon={<Calendar size={16} />} label="Date of Birth" value={employee.dateOfBirth ? new Date(employee.dateOfBirth).toLocaleDateString() : '—'} />
              <InfoRow icon={<User size={16} />} label="Gender" value={employee.gender || '—'} />
              <InfoRow icon={<MapPin size={16} />} label="Location" value={[employee.city, employee.state].filter(Boolean).join(', ') || '—'} />
            </div>
          </div>
          <div className="card detail-section">
            <h3>Work Information</h3>
            <div className="info-list">
              <InfoRow icon={<Briefcase size={16} />} label="Emp Code" value={employee.empCode} />
              <InfoRow icon={<Briefcase size={16} />} label="Department" value={employee.departmentName} />
              <InfoRow icon={<User size={16} />} label="Manager" value={employee.managerName || 'None'} />
              <InfoRow icon={<Calendar size={16} />} label="Hire Date" value={new Date(employee.hireDate).toLocaleDateString()} />
              <InfoRow icon={<DollarSign size={16} />} label="CTC" value={employee.ctc ? `₹${employee.ctc.toLocaleString()}` : '—'} />
            </div>
          </div>
        </div>
      )}

      {activeTab === 'skills' && (
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Skills & Competencies</h3>
          {employee.skills && Object.keys(employee.skills).length > 0 ? (
            <div className="skills-list">
              {Object.entries(employee.skills).map(([skill, level]) => (
                <div key={skill} className="skill-item">
                  <div className="skill-header">
                    <span className="skill-name">{skill}</span>
                    <span className="skill-level">{level}/10</span>
                  </div>
                  <div className="skill-bar-bg">
                    <div
                      className="skill-bar-fill"
                      style={{ width: `${(Number(level) / 10) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state"><p>No skills data available</p></div>
          )}
        </div>
      )}

      {activeTab === 'reports' && (
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Direct Reports</h3>
          {reports?.length ? (
            <div className="reports-list">
              {reports.map(r => (
                <div
                  key={r.id}
                  className="report-item"
                  onClick={() => navigate(`/employees/${r.id}`)}
                >
                  <div className="avatar avatar-sm">
                    {r.fullName.split(' ').map(n => n[0]).join('').substring(0, 2)}
                  </div>
                  <div className="report-info">
                    <div className="recent-name">{r.fullName}</div>
                    <div className="recent-detail">{r.designation} · {r.departmentName}</div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state"><p>No direct reports</p></div>
          )}
        </div>
      )}
    </div>
  );
}

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="info-row">
      <div className="info-icon">{icon}</div>
      <div>
        <div className="info-label">{label}</div>
        <div className="info-value">{value}</div>
      </div>
    </div>
  );
}
