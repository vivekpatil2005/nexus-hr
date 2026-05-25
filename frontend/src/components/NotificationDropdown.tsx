import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../lib/auth';
import api from '../lib/api';
import { Bell, CheckCheck, Megaphone, Mail, Info } from 'lucide-react';
import '../pages/NotificationsAnnouncements.css';

interface NotificationItem {
  id: number;
  recipientId: number;
  type: string;
  channel: string;
  subject: string;
  body: string;
  status: string;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

function timeAgo(dateStr: string): string {
  const now = new Date();
  const date = new Date(dateStr);
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDay = Math.floor(diffHr / 24);
  if (diffDay < 7) return `${diffDay}d ago`;
  return date.toLocaleDateString();
}

function getNotifIcon(type: string) {
  switch (type) {
    case 'ANNOUNCEMENT': return <Megaphone size={16} />;
    case 'EMAIL': return <Mail size={16} />;
    default: return <Info size={16} />;
  }
}

function getNotifClass(type: string) {
  switch (type) {
    case 'ANNOUNCEMENT': return 'announcement';
    case 'EMAIL': return 'email';
    default: return 'system';
  }
}

export default function NotificationDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { user } = useAuth();
  const queryClient = useQueryClient();

  // Fetch unread count
  const { data: unreadCount } = useQuery<number>({
    queryKey: ['notifUnreadCount'],
    queryFn: () => api.get('/notifications/me/unread-count').then(r => r.data.data),
    enabled: !!user,
    refetchInterval: 30000, // Poll every 30 seconds
  });

  // Fetch notifications when dropdown is open
  const { data: notifications } = useQuery<NotificationItem[]>({
    queryKey: ['notifList'],
    queryFn: () => api.get('/notifications/me').then(r => r.data.data),
    enabled: !!user && isOpen,
    refetchInterval: isOpen ? 15000 : false,
  });

  // Mark single as read
  const markReadMutation = useMutation({
    mutationFn: (id: number) => api.patch(`/notifications/${id}/read`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifList'] });
      queryClient.invalidateQueries({ queryKey: ['notifUnreadCount'] });
    },
  });

  // Mark all as read
  const markAllReadMutation = useMutation({
    mutationFn: () => api.patch('/notifications/me/read-all'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifList'] });
      queryClient.invalidateQueries({ queryKey: ['notifUnreadCount'] });
    },
  });

  // Close on click outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const count = unreadCount ?? 0;
  const displayNotifications = (notifications ?? []).slice(0, 20);

  return (
    <div className="notification-btn-wrapper" ref={dropdownRef}>
      <button
        className="btn-ghost notification-btn"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Notifications"
      >
        <Bell size={18} />
        {count > 0 && (
          <span className="notif-count-badge">{count > 99 ? '99+' : count}</span>
        )}
      </button>

      {isOpen && (
        <div className="notification-dropdown">
          <div className="notif-header">
            <h3>
              Notifications
              {count > 0 && <span className="notif-unread-badge">{count} new</span>}
            </h3>
            {count > 0 && (
              <button
                className="notif-mark-all"
                onClick={() => markAllReadMutation.mutate()}
                disabled={markAllReadMutation.isPending}
              >
                <CheckCheck size={14} /> Mark all read
              </button>
            )}
          </div>

          <div className="notif-list">
            {displayNotifications.length === 0 ? (
              <div className="notif-empty">
                <Bell size={36} strokeWidth={1} />
                <p>No notifications yet</p>
              </div>
            ) : (
              displayNotifications.map((n) => (
                <div
                  key={n.id}
                  className={`notif-item ${!n.read ? 'unread' : ''}`}
                  onClick={() => {
                    if (!n.read) markReadMutation.mutate(n.id);
                  }}
                >
                  <div className={`notif-icon-wrapper ${getNotifClass(n.type)}`}>
                    {getNotifIcon(n.type)}
                  </div>
                  <div className="notif-content">
                    <div className="notif-title">{n.subject || n.type}</div>
                    <div className="notif-message">{n.body?.replace(/<[^>]*>/g, '').substring(0, 120)}</div>
                    <div className="notif-time">{timeAgo(n.createdAt)}</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
