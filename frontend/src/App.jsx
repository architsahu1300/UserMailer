/**
 * App.jsx - THE MAIN APP COMPONENT
 * =================================
 * This is the root component of our React app. It sets up:
 * 1. Routing - deciding which page to show based on the URL
 * 2. Authentication state - tracking if user is logged in
 * 3. Protected routes - redirecting to login if not authenticated
 * 
 * COMPONENTS in React:
 * - A component is a reusable piece of UI (like a Java class for UI)
 * - Components can have "state" (data that can change)
 * - When state changes, React automatically re-renders the component
 * 
 * HOOKS in React:
 * - useState: Creates a state variable (like a class field that triggers re-render when changed)
 * - useEffect: Runs code when component loads or when dependencies change
 */

import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link, useNavigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import CampaignPage from './pages/CampaignPage';
import ProfilePage from './pages/ProfilePage';
import { isAuthenticated, logout } from './services/api';
import './App.css';

/**
 * Navigation component - shown on all pages when logged in.
 */
function Navigation() {
  const navigate = useNavigate();
  
  const handleLogout = () => {
    logout();
    navigate('/login');
  };
  
  return (
    <nav className="main-nav">
      <div className="nav-brand">
        <span className="brand-icon">📧</span>
        <span className="brand-text">UserMailer</span>
      </div>
      <div className="nav-links">
        <Link to="/dashboard" className="nav-link">Dashboard</Link>
        <Link to="/campaign" className="nav-link">Create Campaign</Link>
        <button onClick={handleLogout} className="nav-link logout-btn">
          Logout
        </button>
      </div>
    </nav>
  );
}

/**
 * ProtectedRoute - A wrapper that checks if user is logged in.
 * If not logged in, redirects to login page.
 * 
 * Think of it like a security filter in Spring Boot!
 */
function ProtectedRoute({ children }) {
  if (!isAuthenticated()) {
    // Navigate is like response.sendRedirect() in Java
    return <Navigate to="/login" replace />;
  }
  
  return (
    <>
      <Navigation />
      {children}
    </>
  );
}

/**
 * Main App component.
 * Sets up all the routes (URLs) for our app.
 */
function App() {
  return (
    // BrowserRouter enables client-side routing (changing pages without full reload)
    <BrowserRouter>
      <Routes>
        {/* Public route - Login/Register page */}
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected routes - require authentication */}
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/campaign" 
          element={
            <ProtectedRoute>
              <CampaignPage />
            </ProtectedRoute>
          } 
        />
        
        {/* Profile details page - :id is a URL parameter */}
        <Route 
          path="/profile/:id" 
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          } 
        />
        
        {/* Default route - redirect to dashboard or login */}
        <Route 
          path="/" 
          element={
            isAuthenticated() 
              ? <Navigate to="/dashboard" replace />
              : <Navigate to="/login" replace />
          } 
        />
        
        {/* Catch-all route for unknown URLs */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

