/**
 * LoginPage.jsx - LOGIN & REGISTER PAGE
 * ======================================
 * This page handles both login and registration.
 * 
 * KEY CONCEPTS:
 * 
 * 1. useState - Creates "state variables" that React tracks
 *    Example: const [email, setEmail] = useState('')
 *    - email: the current value
 *    - setEmail: function to update the value
 *    - useState(''): initial value is empty string
 * 
 * 2. Forms in React - Unlike HTML forms that reload the page,
 *    React forms prevent default behavior and handle submission
 *    with JavaScript.
 * 
 * 3. Conditional Rendering - Using {condition && <Component>}
 *    to show/hide elements based on state.
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register } from '../services/api';
import './LoginPage.css';

function LoginPage() {
  // State variables - React re-renders when these change
  const [isLoginMode, setIsLoginMode] = useState(true);  // Toggle between login/register
  const [email, setEmail] = useState('');                 // Email input value
  const [password, setPassword] = useState('');           // Password input value
  const [error, setError] = useState('');                 // Error message to display
  const [loading, setLoading] = useState(false);          // Loading state for button
  
  // useNavigate hook - like getting a "redirect" object
  const navigate = useNavigate();
  
  /**
   * Handle form submission.
   * This is called when user clicks Login/Register button.
   */
  const handleSubmit = async (e) => {
    // Prevent the default form submission (which would reload the page)
    e.preventDefault();
    
    // Clear any previous errors
    setError('');
    
    // Basic validation
    if (!email || !password) {
      setError('Please fill in all fields');
      return;
    }
    
    if (password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    
    // Start loading
    setLoading(true);
    
    try {
      if (isLoginMode) {
        // Try to login
        await login(email, password);
      } else {
        // Try to register
        await register(email, password);
      }
      
      // Success! Navigate to dashboard
      navigate('/dashboard');
      
    } catch (err) {
      // Show error message
      setError(err.message || 'Something went wrong');
    } finally {
      // Stop loading (runs whether success or error)
      setLoading(false);
    }
  };
  
  /**
   * Toggle between Login and Register modes.
   */
  const toggleMode = () => {
    setIsLoginMode(!isLoginMode);
    setError(''); // Clear errors when switching
  };
  
  return (
    <div className="login-container">
      {/* Decorative background elements */}
      <div className="login-bg-shapes">
        <div className="shape shape-1"></div>
        <div className="shape shape-2"></div>
        <div className="shape shape-3"></div>
      </div>
      
      {/* Main login card */}
      <div className="login-card">
        {/* Header */}
        <div className="login-header">
          <div className="login-logo">📧</div>
          <h1>UserMailer</h1>
          <p>
            {isLoginMode 
              ? 'Welcome back! Please sign in to continue.' 
              : 'Create an account to get started.'}
          </p>
        </div>
        
        {/* Error message (only shows if error is not empty) */}
        {error && (
          <div className="alert alert-error">
            {error}
          </div>
        )}
        
        {/* Login/Register Form */}
        <form onSubmit={handleSubmit}>
          {/* Email field */}
          <div className="form-group">
            <label htmlFor="email">Email Address</label>
            <input
              type="email"
              id="email"
              className="input"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
            />
          </div>
          
          {/* Password field */}
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              className="input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
          </div>
          
          {/* Submit button */}
          <button 
            type="submit" 
            className="btn btn-primary login-btn"
            disabled={loading}
          >
            {loading ? 'Please wait...' : (isLoginMode ? 'Sign In' : 'Create Account')}
          </button>
        </form>
        
        {/* Toggle between login/register */}
        <div className="login-footer">
          <p>
            {isLoginMode ? "Don't have an account?" : "Already have an account?"}
            <button 
              type="button" 
              className="toggle-btn"
              onClick={toggleMode}
            >
              {isLoginMode ? 'Sign up' : 'Sign in'}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;


