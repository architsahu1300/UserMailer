/**
 * DashboardPage.jsx - MAIN DASHBOARD
 * ===================================
 * This is the main page users see after logging in.
 * It shows:
 * 1. A welcome message
 * 2. Sample uploaded profiles
 * 3. A button/section to upload new CSV files
 * 
 * KEY CONCEPTS:
 * 
 * useEffect Hook:
 * - Runs code when component mounts (loads) or when dependencies change
 * - Perfect for fetching data when page loads
 * - Similar to @PostConstruct in Spring
 * 
 * Array.map():
 * - Transforms an array into a list of React elements
 * - Like Java streams: list.stream().map(...).collect()
 * 
 * File Input:
 * - HTML input type="file" for file selection
 * - onChange event gives us access to the selected file
 */

import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfiles, uploadCSV, getUploadStatus } from '../services/api';
import './DashboardPage.css';

function DashboardPage() {
  // State for profiles
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // State for file upload
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState(null);
  const [uploadError, setUploadError] = useState('');
  
  // State for showing upload instructions
  const [showInstructions, setShowInstructions] = useState(false);
  
  // useRef - like a pointer to a DOM element (the file input)
  const fileInputRef = useRef(null);
  
  // useNavigate hook for programmatic navigation
  const navigate = useNavigate();
  
  /**
   * useEffect runs when component mounts (first renders).
   * The empty array [] means "run once when component loads".
   * 
   * This is like @PostConstruct or ComponentDidMount.
   */
  useEffect(() => {
    loadProfiles();
  }, []);
  
  /**
   * Load profiles from the API.
   */
  const loadProfiles = async () => {
    try {
      setLoading(true);
      const data = await getProfiles();
      setProfiles(data.profiles);
    } catch (err) {
      console.error('Failed to load profiles:', err);
    } finally {
      setLoading(false);
    }
  };
  
  /**
   * Handle file selection and upload.
   */
  const handleFileSelect = async (event) => {
    const file = event.target.files[0];
    if (!file) return;
    
    // Validate file type
    if (!file.name.endsWith('.csv')) {
      setUploadError('Please select a CSV file');
      return;
    }
    
    // Start upload
    setUploading(true);
    setUploadError('');
    setUploadResult(null);
    
    try {
      const result = await uploadCSV(file);
      setUploadResult(result);
      
      // Refresh profiles after successful upload
      // (In reality, processing takes time, so profiles won't update immediately)
      setTimeout(() => loadProfiles(), 2000);
      
    } catch (err) {
      setUploadError(err.message || 'Upload failed');
    } finally {
      setUploading(false);
      // Clear the file input so user can upload same file again if needed
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };
  
  /**
   * Trigger the hidden file input when button is clicked.
   */
  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };
  
  return (
    <div className="page-container">
      {/* Page Header */}
      <div className="page-header">
        <h1>Dashboard</h1>
        <p>Manage your profiles and upload new data</p>
      </div>
      
      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-info">
            <span className="stat-value">{profiles.length}</span>
            <span className="stat-label">Total Profiles</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">📤</div>
          <div className="stat-info">
            <span className="stat-value">0</span>
            <span className="stat-label">Campaigns Sent</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">📊</div>
          <div className="stat-info">
            <span className="stat-value">-</span>
            <span className="stat-label">Open Rate</span>
          </div>
        </div>
      </div>
      
      {/* Upload Section */}
      <section className="dashboard-section">
        <div className="section-header">
          <h2>Upload Profiles</h2>
          <button 
            className="btn btn-secondary"
            onClick={() => setShowInstructions(!showInstructions)}
          >
            {showInstructions ? 'Hide Instructions' : 'Show Instructions'}
          </button>
        </div>
        
        {/* Upload Instructions (collapsible) */}
        {showInstructions && (
          <div className="instructions-card">
            <h3>📋 CSV Format Instructions</h3>
            <p>Your CSV file should have the following format:</p>
            <div className="code-block">
              <code>
                email,name,company,phone<br/>
                john@example.com,John Doe,Acme Inc,555-1234<br/>
                jane@example.com,Jane Smith,Tech Corp,555-5678
              </code>
            </div>
            <ul className="instructions-list">
              <li><strong>email</strong> column is required - this identifies each profile</li>
              <li>Other columns become profile properties (you can add any custom columns)</li>
              <li>First row should be the header with column names</li>
              <li>Maximum file size: 100MB</li>
            </ul>
          </div>
        )}
        
        {/* Upload Area */}
        <div className="upload-area">
          {/* Hidden file input */}
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileSelect}
            accept=".csv"
            style={{ display: 'none' }}
          />
          
          {/* Upload box */}
          <div 
            className={`upload-box ${uploading ? 'uploading' : ''}`}
            onClick={!uploading ? triggerFileInput : undefined}
          >
            {uploading ? (
              <>
                <div className="upload-spinner"></div>
                <span>Uploading...</span>
              </>
            ) : (
              <>
                <div className="upload-icon">📁</div>
                <span className="upload-text">Click to select a CSV file</span>
                <span className="upload-subtext">or drag and drop</span>
              </>
            )}
          </div>
          
          {/* Upload result message */}
          {uploadResult && (
            <div className="alert alert-success">
              ✓ {uploadResult.message}
              {uploadResult.jobId && (
                <span className="job-id">Job ID: {uploadResult.jobId}</span>
              )}
            </div>
          )}
          
          {/* Upload error message */}
          {uploadError && (
            <div className="alert alert-error">
              ✗ {uploadError}
            </div>
          )}
        </div>
      </section>
      
      {/* Profiles Section */}
      <section className="dashboard-section">
        <div className="section-header">
          <h2>Your Profiles</h2>
          <span className="section-badge">{profiles.length} profiles</span>
        </div>
        
        {loading ? (
          <div className="loading-state">
            <div className="loading-spinner"></div>
            <span>Loading profiles...</span>
          </div>
        ) : profiles.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📭</div>
            <h3>No profiles yet</h3>
            <p>Upload a CSV file to add profiles to your account.</p>
          </div>
        ) : (
          <div className="profiles-grid">
            {/* 
              .map() transforms each profile into a card component.
              The 'key' prop helps React track which items changed.
              onClick navigates to the profile detail page.
            */}
            {profiles.map((profile) => (
              <div 
                key={profile.id} 
                className="profile-card clickable"
                onClick={() => navigate(`/profile/${profile.id}`)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && navigate(`/profile/${profile.id}`)}
              >
                <div className="profile-avatar">
                  {profile.email[0].toUpperCase()}
                </div>
                <div className="profile-info">
                  <span className="profile-name">
                    {profile.properties?.name || profile.email.split('@')[0]}
                  </span>
                  <span className="profile-email">{profile.email}</span>
                  {profile.properties?.company && (
                    <span className="profile-company">{profile.properties.company}</span>
                  )}
                </div>
                <div className="profile-arrow">→</div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default DashboardPage;

