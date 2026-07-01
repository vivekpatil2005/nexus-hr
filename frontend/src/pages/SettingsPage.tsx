import { useState } from 'react';
import { useAuth } from '../lib/auth';
import api from '../lib/api';
import { Shield, Key, User, CheckCircle2, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import './SettingsPage.css';

export default function SettingsPage() {
  const { user } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Profile Edit State
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [firstName, setFirstName] = useState(user?.fullName?.split(' ')[0] || '');
  const [lastName, setLastName] = useState(user?.fullName?.split(' ').slice(1).join(' ') || '');
  const [email, setEmail] = useState(user?.email || '');
  const [phone, setPhone] = useState('');

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileLoading(true);
    try {
      await api.put('/employees/me', {
        firstName,
        lastName,
        email,
        phone,
      });
      toast.success('Profile updated successfully! You may need to log back in to see name changes everywhere.');
      setIsEditingProfile(false);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to update profile.');
    } finally {
      setProfileLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/auth/change-password', {
        currentPassword,
        newPassword,
      });
      toast.success('Password changed successfully!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to change password. Please try again.';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-content settings-page animate-fade-in">
      <div className="page-header">
        <div>
          <h1>Account Settings</h1>
          <p>Manage your account credentials and personal preferences</p>
        </div>
      </div>

      <div className="settings-grid">
        {/* Profile Card */}
        <div className="card settings-card profile-section">
          <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>
              <User size={18} /> User Profile
            </h3>
            {!isEditingProfile && (
              <button className="btn btn-secondary btn-sm" onClick={() => setIsEditingProfile(true)}>
                Edit Profile
              </button>
            )}
          </div>
          
          {isEditingProfile ? (
            <form onSubmit={handleProfileSubmit} className="settings-form" style={{ marginTop: '1rem' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="firstName">First Name</label>
                <input id="firstName" className="input" value={firstName} onChange={e => setFirstName(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="lastName">Last Name</label>
                <input id="lastName" className="input" value={lastName} onChange={e => setLastName(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="email">Email</label>
                <input id="email" type="email" className="input" value={email} onChange={e => setEmail(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="phone">Phone</label>
                <input id="phone" className="input" value={phone} onChange={e => setPhone(e.target.value)} />
              </div>
              <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                <button type="submit" className="btn btn-primary" disabled={profileLoading}>
                  {profileLoading ? 'Saving...' : 'Save Profile'}
                </button>
                <button type="button" className="btn btn-ghost" onClick={() => setIsEditingProfile(false)} disabled={profileLoading}>
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <>
              <div className="profile-details">
                <div className="avatar-large">
                  {user?.fullName
                    ?.split(' ')
                    .map((n) => n[0])
                    .join('')
                    .substring(0, 2) ?? 'U'}
                </div>
                <div className="profile-info">
                  <h2>{user?.fullName}</h2>
                  <p className="email">{user?.email}</p>
                  <div className="roles-badges">
                    {user?.roles?.map((role) => (
                      <span key={role} className="badge badge-role">
                        {role.replace('ROLE_', '')}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              <div className="details-list">
                <div className="details-item">
                  <span className="label">Username</span>
                  <span className="value">{user?.username}</span>
                </div>
                <div className="details-item">
                  <span className="label">Employee ID</span>
                  <span className="value">{user?.employeeId ? `NEX-${String(user.employeeId).padStart(4, '0')}` : 'N/A'}</span>
                </div>
                <div className="details-item">
                  <span className="label">Status</span>
                  <span className="value status-active">
                    <CheckCircle2 size={14} /> Active Account
                  </span>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Change Password Card */}
        <div className="card settings-card password-section">
          <div className="card-header">
            <h3>
              <Shield size={18} /> Credentials & Security
            </h3>
          </div>

          <form onSubmit={handleSubmit} className="settings-form">
            {error && (
              <div className="alert alert-danger">
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="currentPassword">
                Current Password
              </label>
              <div className="input-with-icon">
                <Key size={16} className="input-icon" />
                <input
                  id="currentPassword"
                  type="password"
                  className="input"
                  placeholder="••••••••"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="newPassword">
                New Password
              </label>
              <div className="input-with-icon">
                <Key size={16} className="input-icon" />
                <input
                  id="newPassword"
                  type="password"
                  className="input"
                  placeholder="•••••••• (Min 8 characters)"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="confirmPassword">
                Confirm New Password
              </label>
              <div className="input-with-icon">
                <Key size={16} className="input-icon" />
                <input
                  id="confirmPassword"
                  type="password"
                  className="input"
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <button type="submit" className="btn btn-primary settings-btn" disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16 }} /> : 'Update Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
