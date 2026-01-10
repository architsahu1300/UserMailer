/**
 * CampaignPage.jsx - CREATE CAMPAIGN PAGE
 * ========================================
 * This page allows users to create email campaigns with advanced filtering.
 * 
 * Features:
 * - Dynamic filter builder to target specific profiles
 * - Real-time preview of matching profiles
 * - Live email preview with placeholder replacement
 * - Campaign scheduling options
 */

import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  createCampaign, 
  getPropertyKeys, 
  getPropertyValues, 
  previewFilters 
} from '../services/api';
import './CampaignPage.css';

// Debounce hook for delayed API calls
function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value);
  
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);
  
  return debouncedValue;
}

function CampaignPage() {
  // Form state
  const [campaignName, setCampaignName] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  
  // Filter state
  const [filters, setFilters] = useState([]);
  const [filterLogic, setFilterLogic] = useState('AND');
  const [propertyKeys, setPropertyKeys] = useState([]);
  const [operators, setOperators] = useState([]);
  const [valuesSuggestions, setValuesSuggestions] = useState({});
  
  // Preview state
  const [previewData, setPreviewData] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  
  // UI state
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [sendOption, setSendOption] = useState('immediate');
  const [scheduledDate, setScheduledDate] = useState('');
  const [scheduledTime, setScheduledTime] = useState('');
  
  const navigate = useNavigate();
  
  // Debounce filters for preview
  const debouncedFilters = useDebounce(filters, 500);
  
  // Load property keys on mount
  useEffect(() => {
    loadPropertyKeys();
  }, []);
  
  // Load preview when filters change
  useEffect(() => {
    loadPreview();
  }, [debouncedFilters, filterLogic]);
  
  const loadPropertyKeys = async () => {
    try {
      const data = await getPropertyKeys();
      setPropertyKeys(data.properties || []);
      setOperators(data.operators || []);
    } catch (err) {
      console.error('Failed to load property keys:', err);
    }
  };
  
  const loadPropertyValues = async (key) => {
    if (valuesSuggestions[key]) return;
    
    try {
      const data = await getPropertyValues(key);
      setValuesSuggestions(prev => ({
        ...prev,
        [key]: data.values || []
      }));
    } catch (err) {
      console.error('Failed to load property values:', err);
    }
  };
  
  const loadPreview = async () => {
    setPreviewLoading(true);
    try {
      // Only include valid filters
      const validFilters = filters.filter(f => f.key && f.operator);
      const data = await previewFilters(validFilters, filterLogic, 5);
      setPreviewData(data);
    } catch (err) {
      console.error('Failed to load preview:', err);
    } finally {
      setPreviewLoading(false);
    }
  };
  
  // Filter management
  const addFilter = () => {
    setFilters([...filters, { key: '', operator: 'EQUALS', value: '' }]);
  };
  
  const updateFilter = (index, field, value) => {
    const newFilters = [...filters];
    newFilters[index] = { ...newFilters[index], [field]: value };
    setFilters(newFilters);
    
    // Load value suggestions when key changes
    if (field === 'key' && value) {
      loadPropertyValues(value);
    }
  };
  
  const removeFilter = (index) => {
    setFilters(filters.filter((_, i) => i !== index));
  };
  
  /**
   * Handle form submission.
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    // Validate inputs
    if (!campaignName.trim()) {
      setError('Please enter a campaign name');
      return;
    }
    if (!subject.trim()) {
      setError('Please enter an email subject');
      return;
    }
    if (!message.trim()) {
      setError('Please enter your message');
      return;
    }
    if (previewData?.totalCount === 0) {
      setError('No profiles match your filters. Please adjust your filters.');
      return;
    }
    
    setSending(true);
    
    try {
      const validFilters = filters.filter(f => f.key && f.operator);
      
      let scheduledTimeISO = null;
      if (sendOption === 'scheduled' && scheduledDate && scheduledTime) {
        scheduledTimeISO = new Date(`${scheduledDate}T${scheduledTime}`).toISOString();
      }
      
      await createCampaign({
        name: campaignName,
        subject: subject,
        body: message,
        filters: validFilters,
        filterLogic: filterLogic,
        sendImmediately: sendOption === 'immediate',
        scheduledTime: scheduledTimeISO,
      });
      
      setSuccess(true);
      
      // Reset form after success
      setCampaignName('');
      setSubject('');
      setMessage('');
      setFilters([]);
      setSendOption('immediate');
      
    } catch (err) {
      setError(err.message || 'Failed to create campaign');
    } finally {
      setSending(false);
    }
  };
  
  /**
   * Insert a placeholder/variable at cursor position.
   */
  const insertPlaceholder = (placeholder) => {
    setMessage(prev => prev + `{{${placeholder}}}`);
  };
  
  /**
   * Get placeholder buttons based on available properties
   */
  const getPlaceholderButtons = () => {
    const commonPlaceholders = ['email'];
    const customPlaceholders = propertyKeys.filter(k => k !== 'email').slice(0, 6);
    return [...commonPlaceholders, ...customPlaceholders];
  };
  
  /**
   * Check if operator requires a value input
   */
  const operatorNeedsValue = (op) => {
    return !['IS_EMPTY', 'IS_NOT_EMPTY'].includes(op);
  };
  
  return (
    <div className="page-container campaign-page">
      {/* Page Header */}
      <div className="page-header">
        <div className="header-content">
          <h1>Create Campaign</h1>
          <p>Target specific profiles and send personalized emails</p>
        </div>
        
        {/* Recipient count badge */}
        <div className="recipient-badge">
          <span className="recipient-icon">👥</span>
          <span className="recipient-count">
            {previewLoading ? '...' : (previewData?.totalCount ?? 0)}
          </span>
          <span className="recipient-label">recipients</span>
        </div>
      </div>
      
      {/* Main content */}
      <div className="campaign-layout">
        {/* Left side - Filters & Form */}
        <div className="campaign-main">
          {error && (
            <div className="alert alert-error">{error}</div>
          )}
          
          {success && (
            <div className="alert alert-success">
              ✓ Campaign created successfully! Your emails are being processed.
              <button 
                className="btn btn-secondary btn-sm"
                onClick={() => navigate('/dashboard')}
                style={{ marginLeft: '12px' }}
              >
                View Dashboard
              </button>
            </div>
          )}
          
          {/* Filter Builder Section */}
          <section className="filter-section">
            <div className="section-header">
              <div className="section-title">
                <span className="section-icon">🎯</span>
                <h2>Audience Filters</h2>
              </div>
              <p className="section-desc">
                Define who should receive this email. Leave empty to send to all profiles.
              </p>
            </div>
            
            <div className="filter-builder">
              {filters.length > 0 && (
                <>
                  {/* Filter logic toggle */}
                  <div className="filter-logic-toggle">
                    <span className="logic-label">Match profiles where</span>
                    <div className="logic-buttons">
                      <button
                        type="button"
                        className={`logic-btn ${filterLogic === 'AND' ? 'active' : ''}`}
                        onClick={() => setFilterLogic('AND')}
                      >
                        ALL conditions match
                      </button>
                      <button
                        type="button"
                        className={`logic-btn ${filterLogic === 'OR' ? 'active' : ''}`}
                        onClick={() => setFilterLogic('OR')}
                      >
                        ANY condition matches
                      </button>
                    </div>
                  </div>
                  
                  {/* Filter list */}
                  <div className="filters-list">
                    {filters.map((filter, index) => (
                      <div key={index} className="filter-row">
                        <span className="filter-connector">
                          {index === 0 ? 'Where' : filterLogic}
                        </span>
                        
                        <select
                          className="filter-select filter-key"
                          value={filter.key}
                          onChange={(e) => updateFilter(index, 'key', e.target.value)}
                        >
                          <option value="">Select property...</option>
                          {propertyKeys.map(key => (
                            <option key={key} value={key}>{key}</option>
                          ))}
                        </select>
                        
                        <select
                          className="filter-select filter-operator"
                          value={filter.operator}
                          onChange={(e) => updateFilter(index, 'operator', e.target.value)}
                        >
                          {operators.map(op => (
                            <option key={op.value} value={op.value}>{op.label}</option>
                          ))}
                        </select>
                        
                        {operatorNeedsValue(filter.operator) && (
                          <div className="filter-value-wrapper">
                            <input
                              type="text"
                              className="filter-input"
                              placeholder="Enter value..."
                              value={filter.value}
                              onChange={(e) => updateFilter(index, 'value', e.target.value)}
                              list={`values-${index}`}
                            />
                            {valuesSuggestions[filter.key] && (
                              <datalist id={`values-${index}`}>
                                {valuesSuggestions[filter.key].map(val => (
                                  <option key={val} value={val} />
                                ))}
                              </datalist>
                            )}
                          </div>
                        )}
                        
                        <button
                          type="button"
                          className="filter-remove-btn"
                          onClick={() => removeFilter(index)}
                          title="Remove filter"
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                </>
              )}
              
              <button
                type="button"
                className="add-filter-btn"
                onClick={addFilter}
              >
                <span className="add-icon">+</span>
                Add Filter
              </button>
              
              {filters.length === 0 && (
                <p className="no-filters-hint">
                  💡 No filters added. Campaign will be sent to all your profiles.
                </p>
              )}
            </div>
          </section>
          
          {/* Email Content Section */}
          <section className="content-section">
            <div className="section-header">
              <div className="section-title">
                <span className="section-icon">✉️</span>
                <h2>Email Content</h2>
              </div>
            </div>
            
            <form onSubmit={handleSubmit} className="campaign-form">
              {/* Campaign Name */}
              <div className="form-group">
                <label htmlFor="campaignName">Campaign Name</label>
                <input
                  type="text"
                  id="campaignName"
                  className="input"
                  placeholder="e.g., January Newsletter, Product Launch, Welcome Series"
                  value={campaignName}
                  onChange={(e) => setCampaignName(e.target.value)}
                  disabled={sending}
                />
                <span className="form-hint">Internal name to identify this campaign</span>
              </div>
              
              {/* Email Subject */}
              <div className="form-group">
                <label htmlFor="subject">Email Subject</label>
                <input
                  type="text"
                  id="subject"
                  className="input"
                  placeholder="e.g., {{name}}, check out our latest updates!"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  disabled={sending}
                />
                <span className="form-hint">Use placeholders like {"{{name}}"} for personalization</span>
              </div>
              
              {/* Message Body */}
              <div className="form-group">
                <label htmlFor="message">Message Body</label>
                <textarea
                  id="message"
                  className="input message-textarea"
                  placeholder="Write your email content here...

Dear {{name}},

Thank you for being a valued customer...

Best regards,
Your Team"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  disabled={sending}
                  rows={12}
                />
              </div>
              
              {/* Quick placeholder buttons */}
              <div className="placeholder-section">
                <span className="placeholder-label">Insert placeholder:</span>
                <div className="placeholder-buttons">
                  {getPlaceholderButtons().map(placeholder => (
                    <button
                      key={placeholder}
                      type="button"
                      className="placeholder-btn"
                      onClick={() => insertPlaceholder(placeholder)}
                    >
                      {`{{${placeholder}}}`}
                    </button>
                  ))}
                </div>
              </div>
              
              {/* Send Options */}
              <div className="send-options">
                <label className="send-option-label">When to send:</label>
                <div className="send-option-group">
                  <label className="radio-option">
                    <input
                      type="radio"
                      name="sendOption"
                      value="immediate"
                      checked={sendOption === 'immediate'}
                      onChange={(e) => setSendOption(e.target.value)}
                    />
                    <span className="radio-custom"></span>
                    <span className="radio-label">Send immediately</span>
                  </label>
                  
                  <label className="radio-option">
                    <input
                      type="radio"
                      name="sendOption"
                      value="draft"
                      checked={sendOption === 'draft'}
                      onChange={(e) => setSendOption(e.target.value)}
                    />
                    <span className="radio-custom"></span>
                    <span className="radio-label">Save as draft</span>
                  </label>
                  
                  <label className="radio-option">
                    <input
                      type="radio"
                      name="sendOption"
                      value="scheduled"
                      checked={sendOption === 'scheduled'}
                      onChange={(e) => setSendOption(e.target.value)}
                    />
                    <span className="radio-custom"></span>
                    <span className="radio-label">Schedule for later</span>
                  </label>
                </div>
                
                {sendOption === 'scheduled' && (
                  <div className="schedule-inputs">
                    <input
                      type="date"
                      className="input schedule-date"
                      value={scheduledDate}
                      onChange={(e) => setScheduledDate(e.target.value)}
                      min={new Date().toISOString().split('T')[0]}
                    />
                    <input
                      type="time"
                      className="input schedule-time"
                      value={scheduledTime}
                      onChange={(e) => setScheduledTime(e.target.value)}
                    />
                  </div>
                )}
              </div>
              
              {/* Submit button */}
              <div className="form-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => navigate('/dashboard')}
                  disabled={sending}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={sending || (previewData?.totalCount === 0)}
                >
                  {sending ? 'Creating...' : (
                    sendOption === 'immediate' 
                      ? `Send to ${previewData?.totalCount ?? 0} recipients` 
                      : sendOption === 'draft'
                        ? 'Save Draft'
                        : 'Schedule Campaign'
                  )}
                </button>
              </div>
            </form>
          </section>
        </div>
        
        {/* Right side - Preview */}
        <div className="campaign-sidebar">
          {/* Email Preview */}
          <div className="preview-card">
            <div className="preview-header">
              <h3>📧 Email Preview</h3>
              <span className="preview-badge">Live Preview</span>
            </div>
            
            <div className="email-preview">
              <div className="preview-email-header">
                <div className="preview-row">
                  <span className="preview-label">From:</span>
                  <span>Your Company &lt;noreply@yourcompany.com&gt;</span>
                </div>
                <div className="preview-row">
                  <span className="preview-label">To:</span>
                  <span>
                    {previewData?.sampleProfiles?.[0]?.email || 'recipient@example.com'}
                  </span>
                </div>
                <div className="preview-row">
                  <span className="preview-label">Subject:</span>
                  <span className="preview-subject">
                    {replaceWithSample(subject, previewData?.sampleProfiles?.[0]) || '(No subject)'}
                  </span>
                </div>
              </div>
              
              <div className="preview-email-body">
                {message ? (
                  <div className="preview-content">
                    {replaceWithSample(message, previewData?.sampleProfiles?.[0])
                      .split('\n')
                      .map((line, index) => (
                        <p key={index}>{line || '\u00A0'}</p>
                      ))
                    }
                  </div>
                ) : (
                  <div className="preview-placeholder">
                    Your email content will appear here...
                  </div>
                )}
              </div>
            </div>
          </div>
          
          {/* Matching Profiles Preview */}
          <div className="profiles-preview-card">
            <div className="profiles-header">
              <h3>👥 Matching Profiles</h3>
              <span className="profiles-count">
                {previewLoading ? '...' : `${previewData?.totalCount ?? 0} total`}
              </span>
            </div>
            
            {previewData?.sampleProfiles?.length > 0 ? (
              <div className="sample-profiles">
                {previewData.sampleProfiles.map((profile, idx) => (
                  <div key={profile.id || idx} className="sample-profile">
                    <div className="profile-avatar">
                      {profile.email?.[0]?.toUpperCase() || '?'}
                    </div>
                    <div className="profile-info">
                      <span className="profile-email">{profile.email}</span>
                      {profile.properties && Object.keys(profile.properties).length > 0 && (
                        <span className="profile-props">
                          {Object.entries(profile.properties)
                            .slice(0, 2)
                            .map(([k, v]) => `${k}: ${v}`)
                            .join(' · ')}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
                {(previewData?.totalCount > 5) && (
                  <div className="more-profiles">
                    + {previewData.totalCount - 5} more profiles
                  </div>
                )}
              </div>
            ) : (
              <div className="no-profiles">
                {previewLoading ? (
                  <span>Loading preview...</span>
                ) : filters.length > 0 ? (
                  <span>No profiles match your filters</span>
                ) : (
                  <span>Add filters or upload profiles</span>
                )}
              </div>
            )}
          </div>
          
          {/* Tips card */}
          <div className="info-card">
            <h4>💡 Tips for better campaigns</h4>
            <ul>
              <li>Use filters to target specific segments</li>
              <li>Personalize with {"{{property}}"} placeholders</li>
              <li>Keep subject lines under 50 characters</li>
              <li>Test with a small group first</li>
              <li>Schedule during business hours for better engagement</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Replace placeholders with sample profile data for preview
 */
function replaceWithSample(text, profile) {
  if (!text) return '';
  
  let result = text;
  
  // Replace email placeholder
  result = result.replace(/\{\{email\}\}/gi, profile?.email || 'john@example.com');
  
  // Replace property placeholders
  if (profile?.properties) {
    Object.entries(profile.properties).forEach(([key, value]) => {
      const regex = new RegExp(`\\{\\{${key}\\}\\}`, 'gi');
      result = result.replace(regex, value || '');
    });
  }
  
  // Replace any remaining placeholders with example values
  result = result.replace(/\{\{name\}\}/gi, profile?.properties?.name || 'John');
  result = result.replace(/\{\{company\}\}/gi, profile?.properties?.company || 'Acme Inc');
  result = result.replace(/\{\{(\w+)\}\}/gi, '[$1]');
  
  return result;
}

export default CampaignPage;
