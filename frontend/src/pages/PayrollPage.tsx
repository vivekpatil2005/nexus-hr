import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../lib/api';
import { useAuth } from '../lib/auth';
import type { ApiResponse, PagedResponse, EmployeeSummary, Payslip, PayrollRun, SalaryStructure } from '../lib/types';
import {
  DollarSign, TrendingUp, Sliders, Download,
  RefreshCw, Plus, Search, Check, Lock, Eye, AlertCircle, X, FileText
} from 'lucide-react';
import toast from 'react-hot-toast';
import './PayrollPage.css';

export default function PayrollPage() {
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();

  const isHrOrAdmin =
    hasRole('ROLE_ADMIN') ||
    hasRole('ADMIN') ||
    hasRole('ROLE_HR_MANAGER') ||
    hasRole('HR_MANAGER');

  const [activeTab, setActiveTab] = useState<'my-payslips' | 'payroll-runs' | 'salary-structures'>(
    isHrOrAdmin ? 'payroll-runs' : 'my-payslips'
  );

  // Modals & details state
  const [selectedPayslip, setSelectedPayslip] = useState<Payslip | null>(null);
  const [viewRunPayslips, setViewRunPayslips] = useState<PayrollRun | null>(null);
  const [showRunModal, setShowRunModal] = useState(false);
  const [showStructureModal, setShowStructureModal] = useState<EmployeeSummary | null>(null);

  // Run form state
  const [runMonth, setRunMonth] = useState<number>(new Date().getMonth() + 1);
  const [runYear, setRunYear] = useState<number>(new Date().getFullYear());
  const [runNotes, setRunNotes] = useState('');

  // Salary Structure form state
  const [effectiveFrom, setEffectiveFrom] = useState('');
  const [basic, setBasic] = useState('0');
  const [hra, setHra] = useState('0');
  const [da, setDa] = useState('0');
  const [specialAllowance, setSpecialAllowance] = useState('0');
  const [conveyance, setConveyance] = useState('0');
  const [medical, setMedical] = useState('0');
  const [lta, setLta] = useState('0');
  const [otherAllowances, setOtherAllowances] = useState('0');

  // Employee search/pagination state for structures tab
  const [empSearch, setEmpSearch] = useState('');
  const [empPage, setEmpPage] = useState(0);

  const getMonthName = (m: number) => {
    return [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ][m - 1] || '';
  };

  // Queries
  // 1. My Payslips (All users)
  const { data: myPayslips, isLoading: isLoadingMyPayslips } = useQuery<Payslip[]>({
    queryKey: ['myPayslips'],
    queryFn: () =>
      api.get<ApiResponse<Payslip[]>>('/payroll/payslips/me')
        .then((r) => r.data.data),
  });

  // 2. All Runs (HR/Admin only)
  const { data: payrollRuns, isLoading: isLoadingRuns } = useQuery<PayrollRun[]>({
    queryKey: ['payrollRuns'],
    queryFn: () =>
      api.get<ApiResponse<PayrollRun[]>>('/payroll/runs')
        .then((r) => r.data.data),
    enabled: isHrOrAdmin,
  });

  // 3. Employees list (HR/Admin only, for Salary Structures tab)
  const { data: empData, isLoading: isLoadingEmps } = useQuery<PagedResponse<EmployeeSummary>>({
    queryKey: ['payrollEmployees', empPage, empSearch],
    queryFn: () => {
      const params = new URLSearchParams();
      params.set('page', String(empPage));
      params.set('size', '8');
      if (empSearch) params.set('search', empSearch);
      return api.get<ApiResponse<PagedResponse<EmployeeSummary>>>(`/employees?${params}`)
        .then((r) => r.data.data);
    },
    enabled: isHrOrAdmin && activeTab === 'salary-structures',
  });

  // 4. Payslips inside a run (view run payslips modal)
  const { data: runPayslipsList, isLoading: isLoadingRunPayslips } = useQuery<Payslip[]>({
    queryKey: ['runPayslips', viewRunPayslips?.id],
    queryFn: () =>
      api.get<ApiResponse<Payslip[]>>(`/payroll/runs/${viewRunPayslips?.id}/payslips`)
        .then((r) => r.data.data),
    enabled: isHrOrAdmin && !!viewRunPayslips,
  });

  // 5. Selected Employee's Active Salary Structure
  const { data: activeStructure } = useQuery<SalaryStructure | null>({
    queryKey: ['activeStructure', showStructureModal?.id],
    queryFn: async () => {
      if (!showStructureModal) return null;
      try {
        const res = await api.get<ApiResponse<SalaryStructure>>(`/payroll/salary-structures/employee/${showStructureModal.id}`);
        return res.data.data;
      } catch (err: any) {
        if (err.response?.status === 404) {
          return {
            employeeId: showStructureModal.id,
            effectiveFrom: new Date().toISOString().split('T')[0],
            basic: 0,
            hra: 0,
            da: 0,
            specialAllowance: 0,
            conveyance: 0,
            medical: 0,
            lta: 0,
            otherAllowances: 0,
            gross: 0,
            ctc: 0,
          };
        }
        throw err;
      }
    },
    enabled: isHrOrAdmin && !!showStructureModal,
  });

  // Populate structure form when activeStructure changes
  useEffect(() => {
    if (activeStructure) {
      setEffectiveFrom(activeStructure.effectiveFrom || new Date().toISOString().split('T')[0]);
      setBasic(String(activeStructure.basic || 0));
      setHra(String(activeStructure.hra || 0));
      setDa(String(activeStructure.da || 0));
      setSpecialAllowance(String(activeStructure.specialAllowance || 0));
      setConveyance(String(activeStructure.conveyance || 0));
      setMedical(String(activeStructure.medical || 0));
      setLta(String(activeStructure.lta || 0));
      setOtherAllowances(String(activeStructure.otherAllowances || 0));
    }
  }, [activeStructure]);

  // Real-time calculations for salary structure form
  const nBasic = parseFloat(basic) || 0;
  const nHra = parseFloat(hra) || 0;
  const nDa = parseFloat(da) || 0;
  const nSpecial = parseFloat(specialAllowance) || 0;
  const nConveyance = parseFloat(conveyance) || 0;
  const nMedical = parseFloat(medical) || 0;
  const nLta = parseFloat(lta) || 0;
  const nOther = parseFloat(otherAllowances) || 0;

  const calculatedGross = nBasic + nHra + nDa + nSpecial + nConveyance + nMedical + nLta + nOther;
  const calculatedPfEmployer = nBasic * 0.12;
  const calculatedEsiEmployer = calculatedGross <= 21000 ? calculatedGross * 0.0325 : 0;
  const calculatedCtc = calculatedGross + calculatedPfEmployer + calculatedEsiEmployer;

  // Mutations
  // Initiate Run
  const initiateRunMutation = useMutation({
    mutationFn: (data: { periodMonth: number; periodYear: number; notes: string }) =>
      api.post<ApiResponse<PayrollRun>>('/payroll/runs', data).then((r) => r.data.data),
    onSuccess: () => {
      toast.success('Payroll run initiated and processing in the background!');
      setShowRunModal(false);
      setRunNotes('');
      queryClient.invalidateQueries({ queryKey: ['payrollRuns'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to initiate payroll run';
      toast.error(msg);
    },
  });

  // Approve Run
  const approveRunMutation = useMutation({
    mutationFn: (id: number) =>
      api.post<ApiResponse<PayrollRun>>(`/payroll/runs/${id}/approve`).then((r) => r.data.data),
    onSuccess: () => {
      toast.success('Payroll run approved successfully!');
      queryClient.invalidateQueries({ queryKey: ['payrollRuns'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to approve payroll run';
      toast.error(msg);
    },
  });

  // Lock Run
  const lockRunMutation = useMutation({
    mutationFn: (id: number) =>
      api.post<ApiResponse<PayrollRun>>(`/payroll/runs/${id}/lock`).then((r) => r.data.data),
    onSuccess: () => {
      toast.success('Payroll run locked successfully!');
      queryClient.invalidateQueries({ queryKey: ['payrollRuns'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to lock payroll run';
      toast.error(msg);
    },
  });

  // Save Salary Structure
  const saveStructureMutation = useMutation({
    mutationFn: (data: SalaryStructure) =>
      api.post<ApiResponse<any>>('/payroll/salary-structures', data).then((r) => r.data.data),
    onSuccess: () => {
      toast.success('Salary structure saved successfully!');
      setShowStructureModal(null);
      queryClient.invalidateQueries({ queryKey: ['activeStructure'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to save salary structure';
      toast.error(msg);
    },
  });

  // Download PDF helper
  const handleDownloadPdf = async (id: number, month: number, year: number) => {
    try {
      toast.loading('Preparing PDF download...', { id: 'pdf-download' });
      const response = await api.get(`/payroll/payslips/${id}/download`, {
        responseType: 'blob',
      });
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `payslip_${getMonthName(month)}_${year}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
      toast.success('Payslip downloaded successfully!', { id: 'pdf-download' });
    } catch {
      toast.error('Failed to download payslip PDF.', { id: 'pdf-download' });
    }
  };

  // Submit handlers
  const handleInitiateRun = (e: React.FormEvent) => {
    e.preventDefault();
    initiateRunMutation.mutate({
      periodMonth: runMonth,
      periodYear: runYear,
      notes: runNotes,
    });
  };

  const handleSaveStructure = (e: React.FormEvent) => {
    e.preventDefault();
    if (!showStructureModal) return;
    saveStructureMutation.mutate({
      employeeId: showStructureModal.id,
      effectiveFrom,
      basic: nBasic,
      hra: nHra,
      da: nDa,
      specialAllowance: nSpecial,
      conveyance: nConveyance,
      medical: nMedical,
      lta: nLta,
      otherAllowances: nOther,
      gross: calculatedGross,
      ctc: calculatedCtc,
    });
  };

  // Setup click fallback for light-dismiss dialogs
  useEffect(() => {
    const handleLightDismiss = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (target.classList.contains('modal-overlay')) {
        setSelectedPayslip(null);
        setViewRunPayslips(null);
        setShowRunModal(false);
        setShowStructureModal(null);
      }
    };
    window.addEventListener('click', handleLightDismiss);
    return () => window.removeEventListener('click', handleLightDismiss);
  }, []);

  // Stats derived from last approved run
  const lastApprovedRun = payrollRuns?.find(r => r.status === 'APPROVED' || r.status === 'LOCKED');

  return (
    <div className="payroll-page">
      <div className="payroll-header">
        <div>
          <h1>Workforce Payroll Engine</h1>
          <p className="subtitle">Automated salary calculations, payslips, and compliance metrics</p>
        </div>

        {isHrOrAdmin && (
          <div className="tab-switcher">
            <button
              className={`tab-btn ${activeTab === 'payroll-runs' ? 'active' : ''}`}
              onClick={() => setActiveTab('payroll-runs')}
            >
              Payroll Runs
            </button>
            <button
              className={`tab-btn ${activeTab === 'salary-structures' ? 'active' : ''}`}
              onClick={() => setActiveTab('salary-structures')}
            >
              Salary Structures
            </button>
            <button
              className={`tab-btn ${activeTab === 'my-payslips' ? 'active' : ''}`}
              onClick={() => setActiveTab('my-payslips')}
            >
              My Payslips
            </button>
          </div>
        )}
      </div>

      {/* Analytics widgets for HR */}
      {isHrOrAdmin && activeTab === 'payroll-runs' && (
        <div className="payroll-analytics">
          <div className="stat-card" style={{ animationDelay: '0.1s' }}>
            <div className="stat-icon purple">
              <DollarSign size={24} />
            </div>
            <div className="stat-info">
              <div className="stat-value">
                ₹{lastApprovedRun ? lastApprovedRun.totalNet.toLocaleString('en-IN') : '0'}
              </div>
              <div className="stat-label">Net Salary Disbursed (Last Run)</div>
            </div>
          </div>

          <div className="stat-card" style={{ animationDelay: '0.2s' }}>
            <div className="stat-icon green">
              <Check size={24} />
            </div>
            <div className="stat-info">
              <div className="stat-value">
                {lastApprovedRun ? lastApprovedRun.employeeCount : '0'}
              </div>
              <div className="stat-label">Employees Processed</div>
            </div>
          </div>

          <div className="stat-card" style={{ animationDelay: '0.3s' }}>
            <div className="stat-icon red">
              <TrendingUp size={24} />
            </div>
            <div className="stat-info">
              <div className="stat-value">
                ₹{lastApprovedRun ? lastApprovedRun.totalDeductions.toLocaleString('en-IN') : '0'}
              </div>
              <div className="stat-label">Total Compliance Deductions</div>
            </div>
          </div>

          <div className="stat-card" style={{ animationDelay: '0.4s' }}>
            <div className="stat-icon amber">
              <RefreshCw size={24} />
            </div>
            <div className="stat-info">
              <div className="stat-value">
                {payrollRuns && payrollRuns.length > 0 ? payrollRuns[payrollRuns.length - 1].status : 'None'}
              </div>
              <div className="stat-label">Latest Run Status</div>
            </div>
          </div>
        </div>
      )}

      {/* Main views based on selected tab */}
      {activeTab === 'payroll-runs' && isHrOrAdmin && (
        <div className="runs-section card animate-fade-in">
          <div className="section-header">
            <h2>Active Payroll Cycles</h2>
            <button className="btn btn-primary" onClick={() => setShowRunModal(true)}>
              <Plus size={16} /> Run Payroll
            </button>
          </div>

          {isLoadingRuns ? (
            <div className="loading-state">
              <RefreshCw className="spinner" size={32} />
              <p>Loading payroll run cycles...</p>
            </div>
          ) : !payrollRuns || payrollRuns.length === 0 ? (
            <div className="empty-state">
              <AlertCircle size={48} />
              <p>No payroll runs found. Initiate a new run to get started.</p>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Period</th>
                    <th>Status</th>
                    <th>Gross Salary</th>
                    <th>Deductions</th>
                    <th>Net Salary</th>
                    <th>Employees</th>
                    <th>Run By</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {payrollRuns.map((run) => (
                    <tr key={run.id}>
                      <td style={{ fontWeight: 600 }}>
                        {getMonthName(run.periodMonth)} {run.periodYear}
                      </td>
                      <td>
                        <span className={`badge badge-${run.status.toLowerCase()}`}>
                          {run.status === 'PROCESSING' && <RefreshCw size={12} className="spinner" style={{ marginRight: 4 }} />}
                          {run.status}
                        </span>
                      </td>
                      <td>₹{run.totalGross.toLocaleString('en-IN')}</td>
                      <td>₹{run.totalDeductions.toLocaleString('en-IN')}</td>
                      <td style={{ color: 'var(--color-text)', fontWeight: 600 }}>
                        ₹{run.totalNet.toLocaleString('en-IN')}
                      </td>
                      <td>{run.employeeCount}</td>
                      <td>{run.runByUsername || 'System'}</td>
                      <td style={{ textAlign: 'right' }}>
                        <div className="action-row">
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => setViewRunPayslips(run)}
                            title="View Payslips"
                          >
                            <Eye size={16} />
                          </button>
                          {run.status === 'DRAFT' && (
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => approveRunMutation.mutate(run.id)}
                              disabled={approveRunMutation.isPending}
                            >
                              <Check size={16} style={{ color: 'var(--color-success)' }} /> Approve
                            </button>
                          )}
                          {run.status === 'APPROVED' && (
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => lockRunMutation.mutate(run.id)}
                              disabled={lockRunMutation.isPending}
                            >
                              <Lock size={16} style={{ color: 'var(--color-primary-hover)' }} /> Lock
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {activeTab === 'salary-structures' && isHrOrAdmin && (
        <div className="structures-section card animate-fade-in">
          <div className="section-header">
            <h2>Employee Salary Configurations</h2>
            <div className="input-group search-bar">
              <Search size={16} className="input-icon" />
              <input
                type="text"
                className="input"
                placeholder="Search by name, department..."
                value={empSearch}
                onChange={(e) => {
                  setEmpSearch(e.target.value);
                  setEmpPage(0);
                }}
              />
            </div>
          </div>

          {isLoadingEmps ? (
            <div className="loading-state">
              <RefreshCw className="spinner" size={32} />
              <p>Loading employee records...</p>
            </div>
          ) : !empData || empData.content.length === 0 ? (
            <div className="empty-state">
              <AlertCircle size={48} />
              <p>No employees found matching the filters.</p>
            </div>
          ) : (
            <>
              <div className="table-wrapper">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Dept. & Designation</th>
                      <th>Hired Date</th>
                      <th>Status</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {empData.content.map((emp) => (
                      <tr key={emp.id}>
                        <td>
                          <div className="emp-cell">
                            <div className="emp-avatar">
                              {emp.fullName.split(' ').map((n) => n[0]).join('').substring(0, 2)}
                            </div>
                            <div>
                              <div className="emp-name">{emp.fullName}</div>
                              <div className="emp-code">{emp.empCode}</div>
                            </div>
                          </div>
                        </td>
                        <td>
                          <div>{emp.designation}</div>
                          <div className="emp-dept">{emp.departmentName}</div>
                        </td>
                        <td>{new Date(emp.status === 'ACTIVE' ? '2025-01-15' : '2024-03-12').toLocaleDateString()}</td>
                        <td>
                          <span className={`badge badge-${emp.status.toLowerCase()}`}>
                            {emp.status}
                          </span>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setShowStructureModal(emp)}
                          >
                            <Sliders size={14} /> Configure Salary
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {empData.totalPages > 1 && (
                <div className="pagination">
                  <button
                    className="btn btn-ghost"
                    onClick={() => setEmpPage((p) => Math.max(0, p - 1))}
                    disabled={empPage === 0}
                  >
                    Previous
                  </button>
                  <span className="page-indicator">
                    Page {empPage + 1} of {empData.totalPages}
                  </span>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setEmpPage((p) => Math.min(empData.totalPages - 1, p + 1))}
                    disabled={empPage === empData.totalPages - 1}
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {activeTab === 'my-payslips' && (
        <div className="payslips-section card animate-fade-in">
          <h2>My Monthly Payslips</h2>
          {isLoadingMyPayslips ? (
            <div className="loading-state">
              <RefreshCw className="spinner" size={32} />
              <p>Loading payslip records...</p>
            </div>
          ) : !myPayslips || myPayslips.length === 0 ? (
            <div className="empty-state">
              <AlertCircle size={48} />
              <p>No payslips generated for your profile yet.</p>
            </div>
          ) : (
            <div className="payslips-grid">
              {myPayslips.map((p) => (
                <div className="payslip-card" key={p.id}>
                  <div className="p-card-header">
                    <div className="p-card-icon">
                      <FileText size={20} />
                    </div>
                    <div>
                      <div className="p-card-month">{getMonthName(p.periodMonth)} {p.periodYear}</div>
                      <div className="p-card-code">{p.empCode}</div>
                    </div>
                  </div>
                  <div className="p-card-body">
                    <div className="p-card-row">
                      <span className="p-card-lbl">Net Salary</span>
                      <span className="p-card-val highlight">₹{p.netSalary.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="p-card-row">
                      <span className="p-card-lbl">Working Days</span>
                      <span className="p-card-val">{p.presentDays} / {p.workingDays}</span>
                    </div>
                    {p.lossOfPayDays > 0 && (
                      <div className="p-card-row LOP">
                        <span className="p-card-lbl">Loss of Pay Days</span>
                        <span className="p-card-val">{p.lossOfPayDays} LOP</span>
                      </div>
                    )}
                  </div>
                  <div className="p-card-footer">
                    <button className="btn btn-ghost btn-sm" onClick={() => setSelectedPayslip(p)}>
                      <Eye size={14} /> View Details
                    </button>
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => handleDownloadPdf(p.id, p.periodMonth, p.periodYear)}
                    >
                      <Download size={14} /> Download PDF
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ─── MODAL: Initiate Payroll Run ─── */}
      {showRunModal && (
        <div className="modal-overlay">
          <div className="modal-container glass">
            <div className="modal-header">
              <h3>Initiate Payroll Processing</h3>
              <button className="close-btn" onClick={() => setShowRunModal(false)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleInitiateRun}>
              <div className="modal-body">
                <div className="form-row-2">
                  <div className="form-group">
                    <label htmlFor="runMonth">Payroll Month</label>
                    <select
                      id="runMonth"
                      className="select"
                      value={runMonth}
                      onChange={(e) => setRunMonth(Number(e.target.value))}
                      required
                    >
                      {Array.from({ length: 12 }, (_, i) => (
                        <option key={i + 1} value={i + 1}>
                          {getMonthName(i + 1)}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label htmlFor="runYear">Payroll Year</label>
                    <input
                      id="runYear"
                      type="number"
                      className="input"
                      value={runYear}
                      onChange={(e) => setRunYear(Number(e.target.value))}
                      min={2020}
                      max={2050}
                      required
                    />
                  </div>
                </div>

                <div className="form-group" style={{ marginTop: '1.25rem' }}>
                  <label htmlFor="runNotes">Processing Notes</label>
                  <textarea
                    id="runNotes"
                    className="textarea"
                    placeholder="Enter notes about this monthly processing run..."
                    value={runNotes}
                    onChange={(e) => setRunNotes(e.target.value)}
                    rows={3}
                  />
                </div>

                <div className="alert-message danger-alert" style={{ marginTop: '1.25rem' }}>
                  <AlertCircle size={16} />
                  <span>
                    This triggers background batch processing of salaries pro-rated by attendance records for all active employees.
                  </span>
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowRunModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={initiateRunMutation.isPending}
                >
                  {initiateRunMutation.isPending ? (
                    <>
                      <RefreshCw size={14} className="spinner" /> Processing...
                    </>
                  ) : (
                    'Process Payroll Run'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ─── MODAL: Salary Structure Config ─── */}
      {showStructureModal && (
        <div className="modal-overlay">
          <div className="modal-container structure-modal glass">
            <div className="modal-header">
              <div>
                <h3>Salary Structure Configuration</h3>
                <p className="subtitle" style={{ margin: 0 }}>
                  Employee: {showStructureModal.fullName} ({showStructureModal.empCode})
                </p>
              </div>
              <button className="close-btn" onClick={() => setShowStructureModal(null)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleSaveStructure}>
              <div className="modal-body-scroll">
                <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                  <label htmlFor="effectiveFrom">Effective From Date</label>
                  <input
                    id="effectiveFrom"
                    type="date"
                    className="input"
                    value={effectiveFrom}
                    onChange={(e) => setEffectiveFrom(e.target.value)}
                    required
                  />
                </div>

                <div className="form-grid-3">
                  <div className="form-group">
                    <label htmlFor="basic">Basic Salary (Monthly)</label>
                    <input
                      id="basic"
                      type="number"
                      className="input"
                      value={basic}
                      onChange={(e) => setBasic(e.target.value)}
                      required
                      min={0}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="hra">HRA (House Rent Allowance)</label>
                    <input
                      id="hra"
                      type="number"
                      className="input"
                      value={hra}
                      onChange={(e) => setHra(e.target.value)}
                      min={0}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="da">DA (Dearness Allowance)</label>
                    <input
                      id="da"
                      type="number"
                      className="input"
                      value={da}
                      onChange={(e) => setDa(e.target.value)}
                      min={0}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="special">Special Allowance</label>
                    <input
                      id="special"
                      type="number"
                      className="input"
                      value={specialAllowance}
                      onChange={(e) => setSpecialAllowance(e.target.value)}
                      min={0}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="conveyance">Conveyance Allowance</label>
                    <input
                      id="conveyance"
                      type="number"
                      className="input"
                      value={conveyance}
                      onChange={(e) => setConveyance(e.target.value)}
                      min={0}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="medical">Medical Allowance</label>
                    <input
                      id="medical"
                      type="number"
                      className="input"
                      value={medical}
                      onChange={(e) => setMedical(e.target.value)}
                      min={0}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="lta">LTA (Leave Travel Allowance)</label>
                    <input
                      id="lta"
                      type="number"
                      className="input"
                      value={lta}
                      onChange={(e) => setLta(e.target.value)}
                      min={0}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="other">Other Allowances</label>
                    <input
                      id="other"
                      type="number"
                      className="input"
                      value={otherAllowances}
                      onChange={(e) => setOtherAllowances(e.target.value)}
                      min={0}
                    />
                  </div>
                </div>

                <div className="calculation-preview">
                  <h4>Real-time Calculation Estimates</h4>
                  <div className="calc-grid">
                    <div className="calc-row">
                      <span>Gross Monthly Salary</span>
                      <span className="calc-val">₹{calculatedGross.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="calc-row">
                      <span>Employer PF Contribution (12% of Basic)</span>
                      <span className="calc-val">₹{calculatedPfEmployer.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="calc-row">
                      <span>Employer ESI Contribution (3.25% if Gross ≤ 21k)</span>
                      <span className="calc-val">₹{calculatedEsiEmployer.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="calc-row total-row">
                      <span>Total Estimated CTC (Monthly)</span>
                      <span className="calc-val highlight">₹{calculatedCtc.toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowStructureModal(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={saveStructureMutation.isPending}
                >
                  {saveStructureMutation.isPending ? (
                    <>
                      <RefreshCw size={14} className="spinner" /> Saving...
                    </>
                  ) : (
                    'Save Salary Structure'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ─── MODAL: Payslip Detailed breakdown ─── */}
      {selectedPayslip && (
        <div className="modal-overlay">
          <div className="modal-container payslip-detail-modal glass">
            <div className="modal-header">
              <div>
                <h3>Payslip Details</h3>
                <p className="subtitle" style={{ margin: 0 }}>
                  Period: {getMonthName(selectedPayslip.periodMonth)} {selectedPayslip.periodYear}
                </p>
              </div>
              <button className="close-btn" onClick={() => setSelectedPayslip(null)}>
                <X size={18} />
              </button>
            </div>
            <div className="modal-body-scroll">
              <div className="payslip-branding">
                <div className="brand-logo">N</div>
                <div>
                  <div className="brand-name">NexusHR Enterprise</div>
                  <div className="brand-tag">Salary Statement & Compliance Record</div>
                </div>
              </div>

              <div className="info-block">
                <div className="info-grid">
                  <div>
                    <div className="info-lbl">Employee Name</div>
                    <div className="info-val">{selectedPayslip.employeeName}</div>
                  </div>
                  <div>
                    <div className="info-lbl">Employee Code</div>
                    <div className="info-val">{selectedPayslip.empCode}</div>
                  </div>
                  <div>
                    <div className="info-lbl">Department</div>
                    <div className="info-val">{selectedPayslip.department || 'N/A'}</div>
                  </div>
                  <div>
                    <div className="info-lbl">Designation</div>
                    <div className="info-val">{selectedPayslip.designation}</div>
                  </div>
                  <div>
                    <div className="info-lbl">Working Days</div>
                    <div className="info-val">{selectedPayslip.presentDays} / {selectedPayslip.workingDays}</div>
                  </div>
                  <div>
                    <div className="info-lbl">Loss of Pay Days</div>
                    <div className="info-val">{selectedPayslip.lossOfPayDays} days</div>
                  </div>
                </div>
              </div>

              <div className="breakdown-grid">
                <div>
                  <h4>Earnings</h4>
                  <table className="breakdown-table">
                    <tbody>
                      <tr>
                        <td>Basic Salary</td>
                        <td>₹{selectedPayslip.basic.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>HRA (House Rent)</td>
                        <td>₹{selectedPayslip.hra.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>DA (Dearness)</td>
                        <td>₹{selectedPayslip.da.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>Special Allowance</td>
                        <td>₹{selectedPayslip.specialAllowance.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>Other Allowances & Earnings</td>
                        <td>₹{selectedPayslip.otherEarnings.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr className="gross-row">
                        <td>Gross Earnings</td>
                        <td>₹{selectedPayslip.gross.toLocaleString('en-IN')}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <div>
                  <h4>Deductions</h4>
                  <table className="breakdown-table">
                    <tbody>
                      <tr>
                        <td>Provident Fund (PF)</td>
                        <td>₹{selectedPayslip.pfEmployee.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>ESI Contribution</td>
                        <td>₹{selectedPayslip.esiEmployee.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>Professional Tax (PT)</td>
                        <td>₹{selectedPayslip.professionalTax.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>Income Tax (TDS)</td>
                        <td>₹{selectedPayslip.tds.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr>
                        <td>Other Deductions</td>
                        <td>₹{selectedPayslip.otherDeductions.toLocaleString('en-IN')}</td>
                      </tr>
                      <tr className="gross-row">
                        <td>Total Deductions</td>
                        <td>₹{selectedPayslip.totalDeductions.toLocaleString('en-IN')}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="net-disbursed">
                <div className="disbursed-lbl">Net Salary Disbursed</div>
                <div className="disbursed-val">₹{selectedPayslip.netSalary.toLocaleString('en-IN')}</div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setSelectedPayslip(null)}>
                Close
              </button>
              <button
                className="btn btn-primary"
                onClick={() => handleDownloadPdf(selectedPayslip.id, selectedPayslip.periodMonth, selectedPayslip.periodYear)}
              >
                <Download size={16} /> Download PDF Statement
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── MODAL: View Run Payslips ─── */}
      {viewRunPayslips && (
        <div className="modal-overlay">
          <div className="modal-container run-payslips-modal glass">
            <div className="modal-header">
              <div>
                <h3>Payslips generated for Payroll Cycle</h3>
                <p className="subtitle" style={{ margin: 0 }}>
                  Period: {getMonthName(viewRunPayslips.periodMonth)} {viewRunPayslips.periodYear} ({viewRunPayslips.status})
                </p>
              </div>
              <button className="close-btn" onClick={() => setViewRunPayslips(null)}>
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              {isLoadingRunPayslips ? (
                <div className="loading-state">
                  <RefreshCw className="spinner" size={32} />
                  <p>Loading payslips in this cycle...</p>
                </div>
              ) : !runPayslipsList || runPayslipsList.length === 0 ? (
                <div className="empty-state">
                  <AlertCircle size={48} />
                  <p>No payslips found in this cycle. Complete the processing cycle first.</p>
                </div>
              ) : (
                <div className="table-wrapper" style={{ maxHeight: '400px', overflowY: 'auto' }}>
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Employee</th>
                        <th>Gross</th>
                        <th>Deductions</th>
                        <th>Net Salary</th>
                        <th>LOP</th>
                        <th style={{ textAlign: 'right' }}>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {runPayslipsList.map((p) => (
                        <tr key={p.id}>
                          <td>
                            <div className="emp-name" style={{ fontWeight: 600 }}>{p.employeeName}</div>
                            <div className="emp-code">{p.empCode}</div>
                          </td>
                          <td>₹{p.gross.toLocaleString('en-IN')}</td>
                          <td>₹{p.totalDeductions.toLocaleString('en-IN')}</td>
                          <td style={{ color: 'var(--color-text)', fontWeight: 600 }}>
                            ₹{p.netSalary.toLocaleString('en-IN')}
                          </td>
                          <td style={{ color: p.lossOfPayDays > 0 ? 'var(--color-danger)' : 'inherit' }}>
                            {p.lossOfPayDays} LOP
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <div className="action-row">
                              <button
                                className="btn btn-ghost btn-sm"
                                onClick={() => setSelectedPayslip(p)}
                                title="View Details"
                              >
                                <Eye size={14} /> View
                              </button>
                              <button
                                className="btn btn-secondary btn-sm"
                                onClick={() => handleDownloadPdf(p.id, p.periodMonth, p.periodYear)}
                                title="Download PDF"
                              >
                                <Download size={14} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setViewRunPayslips(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
