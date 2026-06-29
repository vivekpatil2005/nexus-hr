import { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import {
  LayoutDashboard, Users, Building2, Network, Clock,
  DollarSign, TrendingUp, Settings, ChevronLeft,
  Search, LogOut, Menu, Brain, Megaphone
} from 'lucide-react';
import NotificationDropdown from './NotificationDropdown';
import './Layout.css';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/employees', icon: Users, label: 'Employees' },
  { to: '/departments', icon: Building2, label: 'Departments' },
  { to: '/org-chart', icon: Network, label: 'Org Chart' },
  { to: '/attendance', icon: Clock, label: 'Attendance' },
  { to: '/payroll', icon: DollarSign, label: 'Payroll' },
  { to: '/performance', icon: TrendingUp, label: 'Performance' },
  { to: '/announcements', icon: Megaphone, label: 'Announcements' },
  { to: '/ai', icon: Brain, label: 'AI Insights' },
];

export default function Layout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  const filteredNavItems = navItems.filter((item) => {
    if (item.to === '/employees' || item.to === '/ai' || item.to === '/announcements') {
      return hasRole('ADMIN') || hasRole('HR_MANAGER') || hasRole('MANAGER');
    }
    if (item.to === '/departments') {
      return hasRole('ADMIN') || hasRole('HR_MANAGER');
    }
    return true;
  });

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user?.fullName
    ?.split(' ')
    .map((n) => n[0])
    .join('')
    .substring(0, 2) ?? 'U';

  return (
    <div className={`app-layout ${collapsed ? 'sidebar-collapsed' : ''}`}>
      {/* Mobile overlay */}
      {mobileOpen && (
        <div className="sidebar-overlay" onClick={() => setMobileOpen(false)} />
      )}

      {/* Sidebar */}
      <aside className={`sidebar ${mobileOpen ? 'mobile-open' : ''}`}>
        {/* Logo */}
        <div className="sidebar-header">
          <div className="logo">
            <div className="logo-icon">N</div>
            {!collapsed && <span className="logo-text">NexusHR</span>}
          </div>
          <button
            className="btn-ghost collapse-btn"
            onClick={() => setCollapsed(!collapsed)}
            aria-label="Toggle sidebar"
          >
            <ChevronLeft size={18} />
          </button>
        </div>

        {/* Navigation */}
        <nav className="sidebar-nav">
          {filteredNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `nav-item ${isActive ? 'active' : ''}`
              }
              onClick={() => setMobileOpen(false)}
              title={collapsed ? item.label : undefined}
            >
              <item.icon size={20} />
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          ))}
        </nav>

        {/* Divider */}
        <div className="sidebar-divider" />

        <NavLink
          to="/settings"
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
          title={collapsed ? 'Settings' : undefined}
        >
          <Settings size={20} />
          {!collapsed && <span>Settings</span>}
        </NavLink>

        {/* User */}
        <div className="sidebar-user">
          <div className="avatar">{initials}</div>
          {!collapsed && (
            <div className="user-info">
              <div className="user-name">{user?.fullName ?? 'User'}</div>
              <div className="user-role">
                {user?.roles?.[0]?.replace('ROLE_', '') ?? 'Employee'}
              </div>
            </div>
          )}
          {!collapsed && (
            <button
              className="btn-ghost logout-btn"
              onClick={handleLogout}
              title="Logout"
            >
              <LogOut size={16} />
            </button>
          )}
        </div>
      </aside>

      {/* Main content */}
      <div className="main-wrapper">
        {/* Top header */}
        <header className="top-header glass">
          <div className="header-left">
            <button
              className="btn-ghost mobile-menu-btn"
              onClick={() => setMobileOpen(true)}
            >
              <Menu size={20} />
            </button>
            <div className="input-group header-search">
              <Search size={16} className="input-icon" />
              <input
                type="text"
                className="input"
                placeholder="Search employees, departments..."
              />
            </div>
          </div>
          <div className="header-right">
            <NotificationDropdown />
            <div className="avatar avatar-sm">{initials}</div>
          </div>
        </header>

        {/* Page content */}
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
