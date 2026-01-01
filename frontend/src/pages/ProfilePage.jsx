/**
 * ProfilePage.jsx - PROFILE DETAILS PAGE
 * =======================================
 * Shows detailed information about a single profile.
 * 
 * NEW CONCEPTS:
 * 
 * useParams Hook:
 * - Extracts URL parameters (like @PathVariable in Spring)
 * - If URL is /profile/123, useParams gives us { id: "123" }
 * 
 * Dynamic Routes:
 * - Routes like /profile/:id where :id is a variable
 * - Similar to @GetMapping("/profile/{id}") in Spring
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getProfile } from '../services/api';
import './ProfilePage.css';

function ProfilePage() {
  // Extract the profile ID from the URL (e.g., /profile/123 → id = "123")
  const { id } = useParams();
  const navigate = useNavigate();
  
  // State
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  /**
   * Load profile data when component mounts or ID changes.
   * The [id] dependency means this runs again if the URL ID changes.
   */
  useEffect(() => {
    loadProfile();
  }, [id]);
  
  const loadProfile = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getProfile(id);
      setProfile(data);
    } catch (err) {
      setError(err.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };
  
  // Loading state
  if (loading) {
    return (
      <div className="page-container">
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <span>Loading profile...</span>
        </div>
      </div>
    );
  }
  
  // Error state
  if (error) {
    return (
      <div className="page-container">
        <div className="error-state">
          <div className="error-icon">⚠️</div>
          <h2>Error Loading Profile</h2>
          <p>{error}</p>
          <button className="btn btn-primary" onClick={() => navigate('/dashboard')}>
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }
  
  // No profile found
  if (!profile) {
    return (
      <div className="page-container">
        <div className="error-state">
          <div className="error-icon">🔍</div>
          <h2>Profile Not Found</h2>
          <p>The profile you're looking for doesn't exist.</p>
          <button className="btn btn-primary" onClick={() => navigate('/dashboard')}>
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }
  
  // Get display name from properties or email
  const displayName = profile.properties?.name || profile.email.split('@')[0];
  const initials = displayName.charAt(0).toUpperCase();
  
  // Convert properties object to array for rendering
  const propertyEntries = profile.properties 
    ? Object.entries(profile.properties)
    : [];
  
  return (
    <div className="page-container">
      {/* Breadcrumb navigation */}
      <div className="breadcrumb">
        <Link to="/dashboard">Dashboard</Link>
        <span className="breadcrumb-separator">→</span>
        <span>Profile</span>
      </div>
      
      {/* Profile header card */}
      <div className="profile-header-card">
        <div className="profile-avatar-large">
          {initials}
        </div>
        <div className="profile-header-info">
          <h1>{displayName}</h1>
          <p className="profile-email-large">{profile.email}</p>
          {profile.properties?.company && (
            <span className="profile-company-badge">
              🏢 {profile.properties.company}
            </span>
          )}
        </div>
        <div className="profile-header-actions">
          <button className="btn btn-secondary" onClick={() => navigate('/dashboard')}>
            ← Back
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/campaign')}>
            Send Email
          </button>
        </div>
      </div>
      
      {/* Profile details section */}
      <div className="profile-content">
        {/* Main info card */}
        <div className="profile-section">
          <h2>📋 Profile Information</h2>
          <div className="profile-details-grid">
            {/* Email is always shown */}
            <div className="detail-item">
              <span className="detail-label">Email</span>
              <span className="detail-value">{profile.email}</span>
            </div>
            
            {/* Profile ID */}
            <div className="detail-item">
              <span className="detail-label">Profile ID</span>
              <span className="detail-value detail-id">#{profile.id}</span>
            </div>
          </div>
        </div>
        
        {/* Properties section */}
        {propertyEntries.length > 0 && (
          <div className="profile-section">
            <h2>🏷️ Properties</h2>
            <p className="section-description">
              Custom properties imported from your CSV file
            </p>
            <div className="properties-grid">
              {propertyEntries.map(([key, value]) => (
                <div key={key} className="property-card">
                  <span className="property-key">{formatPropertyKey(key)}</span>
                  <span className="property-value">{value || '—'}</span>
                </div>
              ))}
            </div>
          </div>
        )}
        
        {/* Empty properties state */}
        {propertyEntries.length === 0 && (
          <div className="profile-section">
            <h2>🏷️ Properties</h2>
            <div className="empty-properties">
              <p>No additional properties found for this profile.</p>
              <span className="empty-hint">
                Properties are imported from CSV columns other than email.
              </span>
            </div>
          </div>
        )}
        
        {/* Quick actions */}
        <div className="profile-section">
          <h2>⚡ Quick Actions</h2>
          <div className="quick-actions">
            <button 
              className="action-card"
              onClick={() => navigate('/campaign')}
            >
              <span className="action-icon">📧</span>
              <span className="action-text">Send Email</span>
              <span className="action-hint">Create a campaign for this profile</span>
            </button>
            <button 
              className="action-card"
              onClick={() => {
                navigator.clipboard.writeText(profile.email);
                alert('Email copied to clipboard!');
              }}
            >
              <span className="action-icon">📋</span>
              <span className="action-text">Copy Email</span>
              <span className="action-hint">Copy email to clipboard</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Format a property key for display.
 * Converts snake_case or camelCase to Title Case.
 */
function formatPropertyKey(key) {
  return key
    .replace(/_/g, ' ')           // Replace underscores with spaces
    .replace(/([A-Z])/g, ' $1')   // Add space before capital letters
    .replace(/^./, str => str.toUpperCase())  // Capitalize first letter
    .trim();
}

export default ProfilePage;

