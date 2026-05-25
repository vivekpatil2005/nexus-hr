import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useAuth } from '../lib/auth';
import api from '../lib/api';
import type { EmployeeSummary, Department, PagedResponse } from '../lib/types';
import { Search, Plus, Users, ChevronLeft, ChevronRight, X } from 'lucide-react';
import './EmployeesPage.css';

interface EmployeeFormData {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  designation: string;
  departmentId: string;
  managerId: string;
  ctc: string;
  hireDate: string;
  gender: string;
  dateOfBirth: string;
}

const initialFormData: EmployeeFormData = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  designation: '',
  departmentId: '',
  managerId: '',
  ctc: '',
  hireDate: new Date().toISOString().split('T')[0],
  gender: 'MALE',
  dateOfBirth: '',
};

function getStatusBadgeClass(status: string) {
  const s = status.toLowerCase().replace('_', '-');
  return `badge badge-${s}`;
}

export default function EmployeesPage() {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [departmentId, setDepartmentId] = useState<string>('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const size = 15;

  const [formData, setFormData] = useState<EmployeeFormData>(initialFormData);

  const canAddEmployee = hasRole('ROLE_ADMIN') || hasRole('ROLE_HR_MANAGER') || hasRole('ADMIN') || hasRole('HR_MANAGER');
  const isAddModalOpen = searchParams.get('add') === 'true';

  const openAddModal = () => setSearchParams({ add: 'true' });
  const closeAddModal = () => {
    const newParams = new URLSearchParams(searchParams);
    newParams.delete('add');
    setSearchParams(newParams);
  };

  const { data: departments } = useQuery({
    queryKey: ['departments'],
    queryFn: () => api.get<{ data: Department[] }>('/departments').then(r => r.data.data),
  });

  const { data: managers } = useQuery({
    queryKey: ['employees-list-all'],
    queryFn: () => api.get<{ data: PagedResponse<EmployeeSummary> }>('/employees?size=500').then(r => r.data.data),
    enabled: canAddEmployee,
  });

  const { data, isLoading } = useQuery({
    queryKey: ['employees', page, search, departmentId, status],
    queryFn: () => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      if (search) params.set('search', search);
      if (departmentId) params.set('departmentId', departmentId);
      if (status) params.set('status', status);
      return api.get<{ data: PagedResponse<EmployeeSummary> }>(`/employees?${params}`)
        .then(r => r.data.data);
    },
  });

  const mutation = useMutation({
    mutationFn: (newEmployee: any) => api.post('/employees', newEmployee),
    onSuccess: () => {
      toast.success('Employee added successfully!');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      queryClient.invalidateQueries({ queryKey: ['employees-list-all'] });
      queryClient.invalidateQueries({ queryKey: ['department-stats'] });
      setFormData(initialFormData);
      closeAddModal();
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to add employee');
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const payload = {
      ...formData,
      departmentId: Number(formData.departmentId),
      managerId: formData.managerId ? Number(formData.managerId) : null,
      ctc: Number(formData.ctc),
      status: 'ACTIVE',
    };
    mutation.mutate(payload);
  };

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>Employees</h1>
          <p>{data?.totalElements ?? 0} total employees</p>
        </div>
        {canAddEmployee && (
          <button className="btn btn-primary" onClick={openAddModal}>
            <Plus size={16} /> Add Employee
          </button>
        )}
      </div>

      {/* Filters */}
      <div className="filters-bar card">
        <div className="input-group filter-search">
          <Search size={16} className="input-icon" />
          <input
            type="text"
            className="input"
            placeholder="Search by name, email, or code..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          />
        </div>
        <select
          className="input filter-select"
          value={departmentId}
          onChange={(e) => { setDepartmentId(e.target.value); setPage(0); }}
        >
          <option value="">All Departments</option>
          {departments?.map(d => (
            <option key={d.id} value={d.id}>{d.name}</option>
          ))}
        </select>
        <select
          className="input filter-select"
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0); }}
        >
          <option value="">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="ON_LEAVE">On Leave</option>
          <option value="PROBATION">Probation</option>
          <option value="NOTICE_PERIOD">Notice Period</option>
          <option value="TERMINATED">Terminated</option>
          <option value="RESIGNED">Resigned</option>
        </select>
      </div>

      {/* Table */}
      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th>Emp Code</th>
              <th>Name</th>
              <th>Email</th>
              <th>Department</th>
              <th>Designation</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>
                  {Array.from({ length: 6 }).map((_, j) => (
                    <td key={j}><div className="skeleton" style={{ height: 16, width: `${60 + Math.random() * 40}%` }} /></td>
                  ))}
                </tr>
              ))
            ) : data?.content?.length ? (
              data.content.map((emp) => (
                <tr
                  key={emp.id}
                  className="clickable-row"
                  onClick={() => navigate(`/employees/${emp.id}`)}
                >
                  <td style={{ fontFamily: 'monospace', color: 'var(--color-primary-hover)' }}>{emp.empCode}</td>
                  <td>
                    <div className="emp-name-cell">
                      <div className="avatar avatar-sm">
                        {emp.fullName.split(' ').map(n => n[0]).join('').substring(0, 2)}
                      </div>
                      <span style={{ fontWeight: 500, color: 'var(--color-text)' }}>{emp.fullName}</span>
                    </div>
                  </td>
                  <td>{emp.email}</td>
                  <td>{emp.departmentName}</td>
                  <td>{emp.designation}</td>
                  <td><span className={getStatusBadgeClass(emp.status)}>{emp.status.replace('_', ' ')}</span></td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6}>
                  <div className="empty-state">
                    <Users size={40} />
                    <p>No employees found</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => p - 1)} disabled={page === 0}>
            <ChevronLeft size={14} />
          </button>
          {Array.from({ length: Math.min(data.totalPages, 7) }).map((_, i) => (
            <button
              key={i}
              className={page === i ? 'active' : ''}
              onClick={() => setPage(i)}
            >
              {i + 1}
            </button>
          ))}
          <button onClick={() => setPage(p => p + 1)} disabled={data.last}>
            <ChevronRight size={14} />
          </button>
        </div>
      )}

      {isAddModalOpen && canAddEmployee && (
        <div className="modal-backdrop" onClick={closeAddModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Add New Employee</h2>
              <button className="modal-close-btn" onClick={closeAddModal} aria-label="Close modal">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-grid">
                  <div className="form-group">
                    <label className="form-label required">First Name</label>
                    <input
                      type="text"
                      className="input"
                      required
                      value={formData.firstName}
                      onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label required">Last Name</label>
                    <input
                      type="text"
                      className="input"
                      required
                      value={formData.lastName}
                      onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label required">Email</label>
                    <input
                      type="email"
                      className="input"
                      required
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Phone</label>
                    <input
                      type="tel"
                      className="input"
                      value={formData.phone}
                      onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label required">Designation</label>
                    <input
                      type="text"
                      className="input"
                      required
                      value={formData.designation}
                      onChange={(e) => setFormData({ ...formData, designation: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label required">Department</label>
                    <select
                      className="input"
                      required
                      value={formData.departmentId}
                      onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                    >
                      <option value="">Select Department</option>
                      {departments?.map((d) => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Manager</label>
                    <select
                      className="input"
                      value={formData.managerId}
                      onChange={(e) => setFormData({ ...formData, managerId: e.target.value })}
                    >
                      <option value="">Select Manager (None)</option>
                      {managers?.content?.map((m) => (
                        <option key={m.id} value={m.id}>{m.fullName} ({m.designation})</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label required">CTC (Annual Salary)</label>
                    <input
                      type="number"
                      className="input"
                      required
                      min="0"
                      value={formData.ctc}
                      onChange={(e) => setFormData({ ...formData, ctc: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label required">Hire Date</label>
                    <input
                      type="date"
                      className="input"
                      required
                      value={formData.hireDate}
                      onChange={(e) => setFormData({ ...formData, hireDate: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Gender</label>
                    <select
                      className="input"
                      value={formData.gender}
                      onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
                    >
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Date of Birth</label>
                    <input
                      type="date"
                      className="input"
                      value={formData.dateOfBirth}
                      onChange={(e) => setFormData({ ...formData, dateOfBirth: e.target.value })}
                    />
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={closeAddModal} disabled={mutation.isPending}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={mutation.isPending}>
                  {mutation.isPending ? 'Saving...' : 'Add Employee'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
