/**
 * CampaignPage.jsx - CREATE CAMPAIGN PAGE
 * ========================================
 * This page allows users to create email campaigns to send to their profiles.
 * 
 * For now, this is a UI-only page since the backend campaign endpoints
 * don't exist yet. We'll wire it up later!
 * 
 * CONCEPTS COVERED:
 * 
 * Controlled Components:
 * - Form inputs whose values are controlled by React state
 * - Every keystroke updates the state, which updates the input
 * - This gives us full control over the form data
 * 
 * Textarea:
 * - Multi-line text input for the email body
 * - Works just like a regular input with value and onChange
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createCampaign } from '../services/api';
import './CampaignPage.css';

function CampaignPage() {
  // Form state
  const [campaignName, setCampaignName] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  
  // UI state
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  
  const navigate = useNavigate();
  
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
    
    setSending(true);
    
    try {
      await createCampaign({
        name: campaignName,
        subject: subject,
        body: message,
      });
      
      setSuccess(true);
      
      // Reset form after success
      setCampaignName('');
      setSubject('');
      setMessage('');
      
    } catch (err) {
      setError(err.message || 'Failed to create campaign');
    } finally {
      setSending(false);
    }
  };
  
  /**
   * Insert a placeholder/variable at cursor position.
   * Placeholders like {{name}} will be replaced with actual profile data.
   */
  const insertPlaceholder = (placeholder) => {
    setMessage(prev => prev + `{{${placeholder}}}`);
  };
  
  return (
    <div className="page-container">
      {/* Page Header */}
      <div className="page-header">
        <h1>Create Campaign</h1>
        <p>Compose an email to send to your uploaded profiles</p>
      </div>
      
      {/* Main content grid */}
      <div className="campaign-grid">
        {/* Left side - Form */}
        <div className="campaign-form-section">
          {error && (
            <div className="alert alert-error">{error}</div>
          )}
          
          {success && (
            <div className="alert alert-success">
              ✓ Campaign created successfully! (Note: Backend not implemented yet)
            </div>
          )}
          
          <form onSubmit={handleSubmit} className="campaign-form">
            {/* Campaign Name */}
            <div className="form-group">
              <label htmlFor="campaignName">Campaign Name</label>
              <input
                type="text"
                id="campaignName"
                className="input"
                placeholder="e.g., January Newsletter"
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
                placeholder="e.g., Check out our latest updates!"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                disabled={sending}
              />
              <span className="form-hint">This is what recipients will see in their inbox</span>
            </div>
            
            {/* Message Body */}
            <div className="form-group">
              <label htmlFor="message">Message</label>
              <textarea
                id="message"
                className="input message-textarea"
                placeholder="Write your email content here...

You can use placeholders like {{name}} to personalize each email."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                disabled={sending}
                rows={12}
              />
              <span className="form-hint">
                Use placeholders like {"{{name}}"} to personalize your message
              </span>
            </div>
            
            {/* Quick placeholder buttons */}
            <div className="placeholder-buttons">
              <span className="placeholder-label">Insert placeholder:</span>
              <button 
                type="button" 
                className="placeholder-btn"
                onClick={() => insertPlaceholder('name')}
              >
                {"{{name}}"}
              </button>
              <button 
                type="button" 
                className="placeholder-btn"
                onClick={() => insertPlaceholder('email')}
              >
                {"{{email}}"}
              </button>
              <button 
                type="button" 
                className="placeholder-btn"
                onClick={() => insertPlaceholder('company')}
              >
                {"{{company}}"}
              </button>
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
                disabled={sending}
              >
                {sending ? 'Creating...' : 'Create Campaign'}
              </button>
            </div>
          </form>
        </div>
        
        {/* Right side - Preview */}
        <div className="campaign-preview-section">
          <div className="preview-header">
            <h3>📧 Email Preview</h3>
            <span className="preview-badge">Live Preview</span>
          </div>
          
          <div className="email-preview">
            {/* Email header */}
            <div className="preview-email-header">
              <div className="preview-row">
                <span className="preview-label">From:</span>
                <span>Your Company &lt;noreply@yourcompany.com&gt;</span>
              </div>
              <div className="preview-row">
                <span className="preview-label">To:</span>
                <span>recipient@example.com</span>
              </div>
              <div className="preview-row">
                <span className="preview-label">Subject:</span>
                <span className="preview-subject">
                  {subject || '(No subject)'}
                </span>
              </div>
            </div>
            
            {/* Email body */}
            <div className="preview-email-body">
              {message ? (
                <div className="preview-content">
                  {/* Replace placeholders with example values for preview */}
                  {message
                    .replace(/\{\{name\}\}/g, 'John')
                    .replace(/\{\{email\}\}/g, 'john@example.com')
                    .replace(/\{\{company\}\}/g, 'Acme Inc')
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
          
          {/* Info card */}
          <div className="info-card">
            <h4>💡 Tips for better emails</h4>
            <ul>
              <li>Keep subject lines under 50 characters</li>
              <li>Personalize with {"{{name}}"} for higher engagement</li>
              <li>Include a clear call-to-action</li>
              <li>Test with a small group first</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CampaignPage;

