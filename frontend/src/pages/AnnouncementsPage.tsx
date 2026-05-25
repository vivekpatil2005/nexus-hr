import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../lib/auth';
import api from '../lib/api';
import type { ApiResponse } from '../lib/types';
import {
  Megaphone, Plus, Edit2, Trash2, Clock, Calendar,
  Send, X, Users
} from 'lucide-react';
import toast from 'react-hot-toast';
import './NotificationsAnnouncements.css';

interface AnnouncementData {
  id: number;
  title: string;
  content: string;
  authorId: number | null;
  authorName: string;
  departmentId: number | null;
  departmentName: string;
  priority: string;
  published: boolean;
  publishedAt: string | null;
  expiresAt: string | null;
  scheduledAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export default function AnnouncementsPage() {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();
  const canManage = hasRole('ADMIN') || hasRole('HR_MANAGER') || hasRole('MANAGER');

  const [showModal, setShowModal] = useState(false);
  const [editingAnn, setEditingAnn] = useState<AnnouncementData | null>(null);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [priority, setPriority] = useState('NORMAL');
  const [publishNow, setPublishNow] = useState(true);
  const [scheduleDate, setScheduleDate] = useState('');
  const [broadcastNotif, setBroadcastNotif] = useState(true);

  // Fetch announcements
  const { data: announcements, isLoading } = useQuery<AnnouncementData[]>({
    queryKey: ['announcements'],
    queryFn: () => {
      const url = canManage ? '/announcements/all' : '/announcements';
      return api.get<ApiResponse<AnnouncementData[]>>(url).then(r => r.data.data);
    },
  });

  // Create / Update
  const saveMutation = useMutation({
    mutationFn: async (data: any) => {
      if (editingAnn) {
        return api.put(`/announcements/${editingAnn.id}`, data);
      }
      const result = await api.post('/announcements', data);

      // If broadcasting notifications AND publishing now
      if (broadcastNotif && data.published) {
        try {
          // Get all user IDs
          const usersResp = await api.get('/auth/users');
          const users = usersResp.data.data || [];
          const recipientIds = users.map((u: any) => u.id);

          if (recipientIds.length > 0) {
            await api.post('/notifications/broadcast', {
              subject: `📢 ${data.title}`,
              message: data.content.substring(0, 200) + (data.content.length > 200 ? '...' : ''),
              recipientIds,
            });
          }
        } catch (e) {
          console.warn('Could not broadcast notification:', e);
        }
      }
      return result;
    },
    onSuccess: () => {
      toast.success(editingAnn ? 'Announcement updated!' : 'Announcement published!');
      closeModal();
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to save announcement');
    },
  });

  // Delete
  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/announcements/${id}`),
    onSuccess: () => {
      toast.success('Announcement deleted');
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
    },
    onError: () => toast.error('Failed to delete'),
  });

  const resetForm = () => {
    setTitle('');
    setContent('');
    setPriority('NORMAL');
    setPublishNow(true);
    setScheduleDate('');
    setBroadcastNotif(true);
    setEditingAnn(null);
  };

  const closeModal = () => {
    setShowModal(false);
    resetForm();
  };

  const openEditModal = (ann: AnnouncementData) => {
    setEditingAnn(ann);
    setTitle(ann.title);
    setContent(ann.content);
    setPriority(ann.priority);
    setPublishNow(ann.published);
    setScheduleDate(ann.scheduledAt ? ann.scheduledAt.substring(0, 16) : '');
    setBroadcastNotif(false);
    setShowModal(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      toast.error('Title and content are required');
      return;
    }

    const isScheduled = !publishNow && scheduleDate;
    const data: any = {
      title,
      content,
      priority,
      authorId: user?.employeeId || user?.id,
      published: publishNow && !isScheduled,
      publishedAt: publishNow ? new Date().toISOString() : null,
      scheduledAt: isScheduled ? scheduleDate : null,
    };

    saveMutation.mutate(data);
  };

  const getStatusBadge = (ann: AnnouncementData) => {
    if (ann.scheduledAt) return <span className="ann-badge scheduled"><Clock size={10} /> Scheduled</span>;
    if (ann.published) return <span className="ann-badge published">Published</span>;
    return <span className="ann-badge draft">Draft</span>;
  };

  const getPriorityBadge = (priority: string) => {
    const cls = priority.toLowerCase();
    return <span className={`ann-badge ${cls}`}>{priority}</span>;
  };

  return (
    <div className="page-content announcements-page">
      <div className="announcements-header">
        <div>
          <h1>📢 Announcements</h1>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: '0.25rem' }}>
            Company-wide announcements and team messages
          </p>
        </div>
        {canManage && (
          <button className="btn btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
            <Plus size={16} /> New Announcement
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="empty-state"><div className="spinner" /></div>
      ) : !announcements || announcements.length === 0 ? (
        <div className="card empty-state" style={{ padding: '4rem 2rem', textAlign: 'center' }}>
          <Megaphone size={48} strokeWidth={1} />
          <h3 style={{ marginTop: '1rem' }}>No announcements yet</h3>
          <p style={{ color: 'var(--color-text-secondary)' }}>
            {canManage ? 'Create your first announcement to get started' : 'Check back soon for updates'}
          </p>
        </div>
      ) : (
        <div>
          {announcements.map((ann) => (
            <div key={ann.id} className="ann-card">
              <div className="ann-card-header">
                <h3 className="ann-card-title">{ann.title}</h3>
                <div className="ann-card-badges">
                  {getStatusBadge(ann)}
                  {getPriorityBadge(ann.priority)}
                </div>
              </div>

              <div className="ann-card-body">
                {ann.content.length > 300 ? ann.content.substring(0, 300) + '...' : ann.content}
              </div>

              <div className="ann-card-footer">
                <div className="ann-card-meta">
                  <span><Users size={12} /> {ann.authorName}</span>
                  <span><Calendar size={12} /> {new Date(ann.createdAt).toLocaleDateString()}</span>
                  {ann.departmentName && ann.departmentName !== 'All Departments' && (
                    <span>🏢 {ann.departmentName}</span>
                  )}
                  {ann.scheduledAt && (
                    <span><Clock size={12} /> Scheduled: {new Date(ann.scheduledAt).toLocaleString()}</span>
                  )}
                </div>

                {canManage && (
                  <div className="ann-card-actions">
                    <button className="btn-ghost btn-sm" onClick={() => openEditModal(ann)}>
                      <Edit2 size={14} />
                    </button>
                    <button
                      className="btn-ghost btn-sm"
                      style={{ color: 'var(--color-danger)' }}
                      onClick={() => {
                        if (confirm('Delete this announcement?')) {
                          deleteMutation.mutate(ann.id);
                        }
                      }}
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create/Edit Modal */}
      {showModal && (
        <div className="ann-modal-overlay" onClick={(e) => e.target === e.currentTarget && closeModal()}>
          <div className="ann-modal">
            <div className="ann-modal-header">
              <h2>{editingAnn ? 'Edit Announcement' : 'New Announcement'}</h2>
              <button className="btn-ghost" onClick={closeModal}><X size={18} /></button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="ann-modal-body">
                <div>
                  <label className="form-label">Title *</label>
                  <input
                    type="text"
                    className="input"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Announcement title..."
                    required
                  />
                </div>

                <div>
                  <label className="form-label">Content *</label>
                  <textarea
                    className="textarea"
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="Write your announcement..."
                    rows={5}
                    required
                    style={{ minHeight: '120px' }}
                  />
                </div>

                <div>
                  <label className="form-label">Priority</label>
                  <select className="select" value={priority} onChange={(e) => setPriority(e.target.value)}>
                    <option value="LOW">Low</option>
                    <option value="NORMAL">Normal</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </div>

                <div>
                  <label className="form-label">Publishing</label>
                  <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                      <input
                        type="radio"
                        name="publishMode"
                        checked={publishNow}
                        onChange={() => { setPublishNow(true); setScheduleDate(''); }}
                      />
                      <Send size={14} /> Publish Now
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                      <input
                        type="radio"
                        name="publishMode"
                        checked={!publishNow}
                        onChange={() => setPublishNow(false)}
                      />
                      <Clock size={14} /> Schedule for Later
                    </label>
                  </div>
                </div>

                {!publishNow && (
                  <div className="ann-schedule-row">
                    <label>Scheduled Date & Time:</label>
                    <input
                      type="datetime-local"
                      className="input"
                      value={scheduleDate}
                      onChange={(e) => setScheduleDate(e.target.value)}
                      min={new Date().toISOString().slice(0, 16)}
                      required
                    />
                  </div>
                )}

                {!editingAnn && publishNow && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <input
                      type="checkbox"
                      id="broadcastCheck"
                      checked={broadcastNotif}
                      onChange={(e) => setBroadcastNotif(e.target.checked)}
                    />
                    <label htmlFor="broadcastCheck" style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                      🔔 Send push notification to all users
                    </label>
                  </div>
                )}
              </div>

              <div className="ann-modal-footer">
                <button type="button" className="btn btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={saveMutation.isPending}>
                  {saveMutation.isPending ? 'Saving...' : editingAnn ? 'Update' : publishNow ? 'Publish & Send' : 'Schedule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
