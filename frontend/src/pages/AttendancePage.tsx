import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../lib/api';
import { useAuth } from '../lib/auth';
import type { AttendanceRecord, LeaveBalance, LeaveRequest, ApiResponse } from '../lib/types';
import {
  Clock, Calendar, CheckCircle2, Send,
  Play, Square, UserCheck, RefreshCw, ClipboardList, Trash2
} from 'lucide-react';
import toast from 'react-hot-toast';
import './AttendancePage.css';

export default function AttendancePage() {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();
  const [currentTime, setCurrentTime] = useState(new Date());
  const [notes, setNotes] = useState('');
  const [activeTab, setActiveTab] = useState<'personal' | 'manager'>('personal');
  const [managerSubTab, setManagerSubTab] = useState<'approvals' | 'live'>('approvals');
  const [liveEvents, setLiveEvents] = useState<AttendanceRecord[]>([]);
  const [rejectionReason, setRejectionReason] = useState<Record<number, string>>({});

  // Leave form state
  const [leaveTypeId, setLeaveTypeId] = useState<number>(1);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [reason, setReason] = useState('');

  // SSE Ref
  const sseRef = useRef<EventSource | null>(null);

  const isManager =
    hasRole('ROLE_ADMIN') ||
    hasRole('ADMIN') ||
    hasRole('ROLE_HR_MANAGER') ||
    hasRole('HR_MANAGER') ||
    hasRole('ROLE_MANAGER') ||
    hasRole('MANAGER');

  // Clock ticking
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Today's attendance
  const { data: todayRecord, isLoading: isLoadingToday } = useQuery<AttendanceRecord | null>({
    queryKey: ['todayAttendance'],
    queryFn: () =>
      api.get<ApiResponse<AttendanceRecord | null>>('/attendance/today')
        .then((r) => r.data.data),
  });

  // Attendance history
  const { data: history, isLoading: isLoadingHistory } = useQuery<AttendanceRecord[]>({
    queryKey: ['attendanceHistory'],
    queryFn: () =>
      api.get<ApiResponse<AttendanceRecord[]>>('/attendance/me')
        .then((r) => r.data.data),
  });

  // Leave balances
  const { data: balances, isLoading: isLoadingBalances } = useQuery<LeaveBalance[]>({
    queryKey: ['leaveBalances'],
    queryFn: () =>
      api.get<ApiResponse<LeaveBalance[]>>('/leave/balance/me')
        .then((r) => r.data.data),
  });

  // Leave history
  const { data: leaveRequests, isLoading: isLoadingLeaves } = useQuery<{ content: LeaveRequest[] }>({
    queryKey: ['leaveRequests'],
    queryFn: () =>
      api.get<ApiResponse<{ content: LeaveRequest[] }>>('/leave/requests')
        .then((r) => r.data.data),
  });

  // Manager: Pending approvals
  const { data: pendingApprovals, isLoading: isLoadingPending } = useQuery<LeaveRequest[]>({
    queryKey: ['pendingApprovals'],
    queryFn: () =>
      api.get<ApiResponse<LeaveRequest[]>>('/leave/pending')
        .then((r) => r.data.data),
    enabled: isManager,
  });

  // Check In Mutation
  const checkInMutation = useMutation({
    mutationFn: (notes: string) =>
      api.post<ApiResponse<AttendanceRecord>>('/attendance/check-in', { notes }),
    onSuccess: () => {
      toast.success('Successfully checked in!');
      setNotes('');
      queryClient.invalidateQueries({ queryKey: ['todayAttendance'] });
      queryClient.invalidateQueries({ queryKey: ['attendanceHistory'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to check in';
      toast.error(msg);
    },
  });

  // Check Out Mutation
  const checkOutMutation = useMutation({
    mutationFn: (notes: string) =>
      api.post<ApiResponse<AttendanceRecord>>('/attendance/check-out', { notes }),
    onSuccess: () => {
      toast.success('Successfully checked out!');
      setNotes('');
      queryClient.invalidateQueries({ queryKey: ['todayAttendance'] });
      queryClient.invalidateQueries({ queryKey: ['attendanceHistory'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to check out';
      toast.error(msg);
    },
  });

  // Leave Request Mutation
  const applyLeaveMutation = useMutation({
    mutationFn: (body: { leaveTypeId: number; fromDate: string; toDate: string; reason: string }) =>
      api.post<ApiResponse<LeaveRequest>>('/leave/request', body),
    onSuccess: () => {
      toast.success('Leave request submitted successfully');
      setFromDate('');
      setToDate('');
      setReason('');
      queryClient.invalidateQueries({ queryKey: ['leaveRequests'] });
      queryClient.invalidateQueries({ queryKey: ['leaveBalances'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to submit leave request';
      toast.error(msg);
    },
  });

  // Cancel Leave Mutation
  const cancelLeaveMutation = useMutation({
    mutationFn: (id: number) =>
      api.put<ApiResponse<LeaveRequest>>(`/leave/${id}/cancel`),
    onSuccess: () => {
      toast.success('Leave request cancelled');
      queryClient.invalidateQueries({ queryKey: ['leaveRequests'] });
      queryClient.invalidateQueries({ queryKey: ['leaveBalances'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to cancel request');
    },
  });

  // Review Leave Mutation (Approve/Reject)
  const reviewLeaveMutation = useMutation({
    mutationFn: ({ id, approved, reason }: { id: number; approved: boolean; reason?: string }) =>
      api.put<ApiResponse<LeaveRequest>>(`/leave/${id}/approve`, {
        approved,
        rejectionReason: reason || null,
      }),
    onSuccess: (_, variables) => {
      toast.success(variables.approved ? 'Leave request approved' : 'Leave request rejected');
      queryClient.invalidateQueries({ queryKey: ['pendingApprovals'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to review leave request');
    },
  });

  // SSE Connection
  useEffect(() => {
    if (!isManager) return;

    const token = localStorage.getItem('accessToken');
    if (!token) return;

    const baseApiUrl = import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080');
    const eventSourceUrl = baseApiUrl.startsWith('http') 
      ? `${baseApiUrl}/api/attendance/stream?token=${token}`
      : `${window.location.origin}${baseApiUrl}/api/attendance/stream?token=${token}`;

    const eventSource = new EventSource(eventSourceUrl);
    sseRef.current = eventSource;

    eventSource.addEventListener('init', (e: MessageEvent) => {
      console.log('SSE connection initialized:', e.data);
    });

    eventSource.addEventListener('attendance-event', (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data) as AttendanceRecord;
        setLiveEvents((prev) => [data, ...prev].slice(0, 50));
        toast(`${data.employeeName} checked ${data.checkOut ? 'out' : 'in'}!`, {
          icon: '⏰',
          style: {
            border: '1px solid var(--color-primary)',
            background: 'var(--color-surface-elevated)',
            color: 'var(--color-text)',
          },
        });
      } catch (err) {
        console.error('Failed to parse SSE attendance event:', err);
      }
    });

    eventSource.onerror = (e) => {
      console.error('SSE connection error, closing...', e);
      eventSource.close();
    };

    return () => {
      if (sseRef.current) {
        sseRef.current.close();
      }
    };
  }, [user, isManager]);

  const handleCheckInOut = () => {
    if (!todayRecord) {
      checkInMutation.mutate(notes);
    } else if (todayRecord && !todayRecord.checkOut) {
      checkOutMutation.mutate(notes);
    }
  };

  const handleApplyLeave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!fromDate || !toDate || !reason) {
      toast.error('Please fill in all fields');
      return;
    }
    applyLeaveMutation.mutate({
      leaveTypeId,
      fromDate,
      toDate,
      reason,
    });
  };

  const formatTime = (isoString: string | null) => {
    if (!isoString) return '--:--';
    const date = new Date(isoString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'PRESENT': return 'status-present';
      case 'LATE': return 'status-late';
      case 'HALF_DAY': return 'status-half-day';
      case 'ABSENT': return 'status-absent';
      case 'ON_LEAVE': return 'status-leave';
      case 'PENDING': return 'status-pending';
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  };

  const calculateWorkDuration = () => {
    if (!todayRecord || !todayRecord.checkIn) return null;
    const start = new Date(todayRecord.checkIn);
    const end = todayRecord.checkOut ? new Date(todayRecord.checkOut) : currentTime;
    const diffMs = end.getTime() - start.getTime();
    if (diffMs < 0) return '0h 0m';
    const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    return `${diffHrs}h ${diffMins}m`;
  };

  return (
    <div className="attendance-page">
      <div className="attendance-header">
        <div>
          <h1>Attendance & Leave</h1>
          <p className="subtitle">Track your daily clock-ins, leave balances, and submissions</p>
        </div>
        {isManager && (
          <div className="tab-switcher glass">
            <button
              className={`tab-btn ${activeTab === 'personal' ? 'active' : ''}`}
              onClick={() => setActiveTab('personal')}
            >
              Personal Portal
            </button>
            <button
              className={`tab-btn ${activeTab === 'manager' ? 'active' : ''}`}
              onClick={() => setActiveTab('manager')}
            >
              Manager Console
            </button>
          </div>
        )}
      </div>

      {activeTab === 'personal' ? (
        <div className="portal-grid">
          {/* Left Column: Clock and Request Leave */}
          <div className="portal-left">
            {/* Clock Widget */}
            <div className="clock-widget card glass">
              <div className="clock-header">
                <div className="time-display">{currentTime.toLocaleTimeString()}</div>
                <div className="date-display">
                  <Calendar size={16} />
                  {currentTime.toLocaleDateString(undefined, {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </div>
              </div>

              <div className="clock-status-container">
                {isLoadingToday ? (
                  <div className="skeleton-loader" style={{ height: 60 }} />
                ) : (
                  <div className="status-banner">
                    <span className="status-label">Today's Status:</span>
                    <span className={`status-badge ${todayRecord ? getStatusClass(todayRecord.status) : 'status-absent'}`}>
                      {todayRecord ? todayRecord.status : 'NOT LOGGED'}
                    </span>
                  </div>
                )}

                {todayRecord && (
                  <div className="clock-details-grid">
                    <div className="detail-item">
                      <span className="detail-label">Check-In</span>
                      <span className="detail-value">{formatTime(todayRecord.checkIn)}</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">Check-Out</span>
                      <span className="detail-value">{formatTime(todayRecord.checkOut)}</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">Work Duration</span>
                      <span className="detail-value">{calculateWorkDuration() || '--'}</span>
                    </div>
                    {todayRecord.overtimeHours > 0 && (
                      <div className="detail-item text-glow-green">
                        <span className="detail-label">Overtime</span>
                        <span className="detail-value">+{todayRecord.overtimeHours} hrs</span>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="clock-notes-wrapper">
                <label htmlFor="clock-notes">Notes / Reason</label>
                <textarea
                  id="clock-notes"
                  className="input notes-textarea"
                  placeholder="Any notes for today's log..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  disabled={!!(todayRecord && todayRecord.checkOut)}
                />
              </div>

              <button
                className={`btn clock-btn ${todayRecord ? (todayRecord.checkOut ? 'btn-disabled' : 'btn-checkout') : 'btn-checkin'}`}
                onClick={handleCheckInOut}
                disabled={!!(todayRecord && todayRecord.checkOut) || checkInMutation.isPending || checkOutMutation.isPending}
              >
                {checkInMutation.isPending || checkOutMutation.isPending ? (
                  <>
                    <RefreshCw size={18} className="spin-icon" />
                    Processing...
                  </>
                ) : !todayRecord ? (
                  <>
                    <Play size={18} /> Clock In
                  </>
                ) : !todayRecord.checkOut ? (
                  <>
                    <Square size={18} /> Clock Out
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={18} /> Checked Out
                  </>
                )}
              </button>
            </div>

            {/* Leave Balances */}
            <div className="balances-section">
              <h2>Leave Balances</h2>
              {isLoadingBalances ? (
                <div className="balances-grid">
                  {Array.from({ length: 4 }).map((_, i) => (
                    <div key={i} className="card skeleton-loader" style={{ height: 100 }} />
                  ))}
                </div>
              ) : (
                <div className="balances-grid">
                  {balances?.map((bal) => {
                    const total = bal.totalDays || 0;
                    const used = bal.usedDays || 0;
                    const pending = bal.pendingDays || 0;
                    const remaining = Math.max(0, total - used);
                    const pct = total > 0 ? (used / total) * 100 : 0;

                    return (
                      <div key={bal.id} className="card balance-card glass">
                        <div className="balance-info">
                          <span className="leave-name">{bal.leaveTypeName}</span>
                          <span className="leave-days">{remaining} Left</span>
                        </div>
                        <div className="progress-bar-container">
                          <div className="progress-bar-fill" style={{ width: `${Math.min(100, pct)}%` }} />
                        </div>
                        <div className="balance-breakdown">
                          <span>Total: {total}</span>
                          <span>Used: {used}</span>
                          {pending > 0 && <span className="text-warning">Pending: {pending}</span>}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Leave Request Form */}
            <div className="apply-leave-section card glass">
              <h2>Apply for Leave</h2>
              <form onSubmit={handleApplyLeave} className="leave-form">
                <div className="form-group">
                  <label>Leave Type</label>
                  <select
                    className="input"
                    value={leaveTypeId}
                    onChange={(e) => setLeaveTypeId(Number(e.target.value))}
                  >
                    {balances?.map((b) => (
                      <option key={b.leaveTypeId} value={b.leaveTypeId}>
                        {b.leaveTypeName} (Available: {b.totalDays - b.usedDays})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>From Date</label>
                    <input
                      type="date"
                      className="input"
                      value={fromDate}
                      onChange={(e) => setFromDate(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label>To Date</label>
                    <input
                      type="date"
                      className="input"
                      value={toDate}
                      onChange={(e) => setToDate(e.target.value)}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>Reason</label>
                  <textarea
                    className="input reason-textarea"
                    placeholder="Provide a reason for the leave..."
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                  />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary submit-btn"
                  disabled={applyLeaveMutation.isPending}
                >
                  {applyLeaveMutation.isPending ? (
                    <>
                      <RefreshCw size={16} className="spin-icon" />
                      Submitting...
                    </>
                  ) : (
                    <>
                      <Send size={16} /> Submit Request
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>

          {/* Right Column: Attendance & Leave History */}
          <div className="portal-right">
            {/* Leave Request History */}
            <div className="requests-section card glass">
              <h2>Leave Requests</h2>
              {isLoadingLeaves ? (
                <div className="skeleton-loader" style={{ height: 200 }} />
              ) : leaveRequests?.content?.length ? (
                <div className="table-wrapper">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Type</th>
                        <th>Dates</th>
                        <th>Days</th>
                        <th>Reason</th>
                        <th>Status</th>
                        <th>Approver</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {leaveRequests.content.map((req) => (
                        <tr key={req.id}>
                          <td>{req.leaveTypeName}</td>
                          <td>
                            <div className="date-cell">
                              <span>{req.fromDate}</span>
                              <span className="date-sep">to</span>
                              <span>{req.toDate}</span>
                            </div>
                          </td>
                          <td>{req.totalDays}</td>
                          <td className="reason-cell" title={req.reason}>
                            {req.reason}
                          </td>
                          <td>
                            <span className={`badge ${getStatusClass(req.status)}`}>
                              {req.status}
                            </span>
                            {req.rejectionReason && (
                              <div className="rejection-hint" title={req.rejectionReason}>
                                Reason: {req.rejectionReason}
                              </div>
                            )}
                          </td>
                          <td>{req.approverName || 'N/A'}</td>
                          <td>
                            {req.status === 'PENDING' && (
                              <button
                                className="btn-icon text-danger"
                                onClick={() => cancelLeaveMutation.mutate(req.id)}
                                disabled={cancelLeaveMutation.isPending}
                                title="Cancel Request"
                              >
                                <Trash2 size={16} />
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty-state">
                  <ClipboardList size={32} />
                  <p>No leave requests found</p>
                </div>
              )}
            </div>

            {/* Daily Logs History */}
            <div className="history-section card glass">
              <h2>Recent Clock Logs</h2>
              {isLoadingHistory ? (
                <div className="skeleton-loader" style={{ height: 200 }} />
              ) : history?.length ? (
                <div className="table-wrapper">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Check-In</th>
                        <th>Check-Out</th>
                        <th>Duration</th>
                        <th>Overtime</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((rec) => {
                        const start = rec.checkIn ? new Date(rec.checkIn) : null;
                        const end = rec.checkOut ? new Date(rec.checkOut) : null;
                        let duration = '--';
                        if (start && end) {
                          const diff = end.getTime() - start.getTime();
                          duration = `${Math.floor(diff / (1000 * 60 * 60))}h ${Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))}m`;
                        }
                        return (
                          <tr key={rec.id}>
                            <td>{rec.date}</td>
                            <td>{formatTime(rec.checkIn)}</td>
                            <td>{formatTime(rec.checkOut)}</td>
                            <td>{duration}</td>
                            <td className={rec.overtimeHours > 0 ? 'text-glow-green' : ''}>
                              {rec.overtimeHours > 0 ? `+${rec.overtimeHours} hrs` : '--'}
                            </td>
                            <td>
                              <span className={`badge ${getStatusClass(rec.status)}`}>
                                {rec.status}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty-state">
                  <Clock size={32} />
                  <p>No recent clock logs found</p>
                </div>
              )}
            </div>
          </div>
        </div>
      ) : (
        /* Manager Console Tab */
        <div className="manager-grid">
          <div className="manager-header-nav glass">
            <button
              className={`nav-btn ${managerSubTab === 'approvals' ? 'active' : ''}`}
              onClick={() => setManagerSubTab('approvals')}
            >
              Pending Leave Approvals ({pendingApprovals?.length ?? 0})
            </button>
            <button
              className={`nav-btn ${managerSubTab === 'live' ? 'active' : ''}`}
              onClick={() => setManagerSubTab('live')}
            >
              Real-time Live Stream ({liveEvents.length})
            </button>
          </div>

          <div className="manager-content">
            {managerSubTab === 'approvals' ? (
              <div className="card glass">
                <h2>Awaiting Review</h2>
                {isLoadingPending ? (
                  <div className="skeleton-loader" style={{ height: 200 }} />
                ) : pendingApprovals?.length ? (
                  <div className="table-wrapper">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Employee</th>
                          <th>Leave Type</th>
                          <th>Dates</th>
                          <th>Days</th>
                          <th>Reason</th>
                          <th>Rejection Comment (Optional)</th>
                          <th className="actions-header">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {pendingApprovals.map((req) => (
                          <tr key={req.id}>
                            <td>
                              <div className="emp-cell">
                                <div className="avatar avatar-sm">
                                  {req.employeeName.split(' ').map(n => n[0]).join('').substring(0, 2)}
                                </div>
                                <span className="emp-name">{req.employeeName}</span>
                              </div>
                            </td>
                            <td>{req.leaveTypeName}</td>
                            <td>
                              <div className="date-cell">
                                <span>{req.fromDate}</span>
                                <span className="date-sep">to</span>
                                <span>{req.toDate}</span>
                              </div>
                            </td>
                            <td>{req.totalDays}</td>
                            <td className="reason-cell" title={req.reason}>
                              {req.reason}
                            </td>
                            <td>
                              <input
                                type="text"
                                className="input table-input"
                                placeholder="Reason if rejecting..."
                                value={rejectionReason[req.id] || ''}
                                onChange={(e) =>
                                  setRejectionReason((prev) => ({
                                    ...prev,
                                    [req.id]: e.target.value,
                                  }))
                                }
                              />
                            </td>
                            <td>
                              <div className="action-buttons">
                                <button
                                  className="btn btn-sm btn-success"
                                  onClick={() =>
                                    reviewLeaveMutation.mutate({
                                      id: req.id,
                                      approved: true,
                                    })
                                  }
                                  disabled={reviewLeaveMutation.isPending}
                                >
                                  Approve
                                </button>
                                <button
                                  className="btn btn-sm btn-danger"
                                  onClick={() => {
                                    if (!rejectionReason[req.id]?.trim()) {
                                      toast.error('Rejection reason is required to reject leave');
                                      return;
                                    }
                                    reviewLeaveMutation.mutate({
                                      id: req.id,
                                      approved: false,
                                      reason: rejectionReason[req.id],
                                    });
                                  }}
                                  disabled={reviewLeaveMutation.isPending}
                                >
                                  Reject
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">
                    <UserCheck size={32} />
                    <p>All clear! No pending leave approvals</p>
                  </div>
                )}
              </div>
            ) : (
              /* Live Stream Tab */
              <div className="card glass">
                <div className="live-stream-header">
                  <h2>Live Attendance stream</h2>
                  <div className="live-indicator">
                    <span className="pulse-dot" />
                    <span>Real-time Connection Active</span>
                  </div>
                </div>

                {liveEvents.length ? (
                  <div className="table-wrapper">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Time</th>
                          <th>Employee</th>
                          <th>Action</th>
                          <th>Notes</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {liveEvents.map((evt, idx) => {
                          const actionTime = evt.checkOut ? evt.checkOut : evt.checkIn;
                          const formattedTime = new Date(actionTime).toLocaleTimeString();
                          return (
                            <tr key={idx} className="fade-in-row">
                              <td style={{ fontFamily: 'monospace' }}>{formattedTime}</td>
                              <td>
                                <div className="emp-cell">
                                  <div className="avatar avatar-sm">
                                    {evt.employeeName.split(' ').map(n => n[0]).join('').substring(0, 2)}
                                  </div>
                                  <div>
                                    <span className="emp-name">{evt.employeeName}</span>
                                    <span className="emp-code">{evt.empCode}</span>
                                  </div>
                                </div>
                              </td>
                              <td>
                                <span className={`badge ${evt.checkOut ? 'status-absent' : 'status-present'}`}>
                                  {evt.checkOut ? 'CLOCKED OUT' : 'CLOCKED IN'}
                                </span>
                              </td>
                              <td>{evt.notes || '--'}</td>
                              <td>
                                <span className={`badge ${getStatusClass(evt.status)}`}>
                                  {evt.status}
                                </span>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">
                    <Clock size={32} />
                    <p>Waiting for live events... Check-ins will appear here in real-time.</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
