/**
 * api.js - API SERVICE
 * =====================
 * This file contains all the functions that communicate with your Spring Boot backend.
 * 
 * In React, we use "fetch" to make HTTP requests (similar to HttpURLConnection in Java,
 * but much simpler). Each function here corresponds to one of your backend endpoints.
 * 
 * The "async/await" syntax is like CompletableFuture in Java - it lets us write
 * asynchronous code that looks synchronous and easy to read.
 */

// Base URL for API calls - empty because Vite proxy handles it (see vite.config.js)
const API_BASE = '';

/**
 * Helper function to get the stored authentication token.
 * We store the JWT token in localStorage (browser's persistent storage).
 */
const getToken = () => localStorage.getItem('token');

/**
 * Helper function to make authenticated API requests.
 * Automatically adds the JWT token to the Authorization header.
 * Also handles 401 errors by logging out the user and redirecting to login.
 */
const authFetch = async (url, options = {}) => {
  const token = getToken();
  const headers = {
    ...options.headers,
  };
  
  // Add Authorization header if we have a token
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  // Add Content-Type for JSON requests (but not for FormData/file uploads)
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  
  const response = await fetch(API_BASE + url, {
    ...options,
    headers,
  });
  
  // Handle authentication errors - auto logout on 401
  if (response.status === 401) {
    // Clear the invalid token
    localStorage.removeItem('token');
    
    // Redirect to login page
    // Using window.location instead of React Router to ensure a full page reload
    // This clears any stale state in the app
    window.location.href = '/login';
    
    // Throw an error to stop further processing
    throw new Error('Session expired. Please login again.');
  }
  
  return response;
};

// =============================================================================
// AUTHENTICATION APIs
// =============================================================================

/**
 * Register a new user.
 * Calls: POST /auth/register
 */
export const register = async (email, password) => {
  const response = await fetch(API_BASE + '/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.message || 'Registration failed');
  }
  
  // The backend returns { token: { token: "..." } }
  const token = data.token?.token || data.token;
  if (token) {
    localStorage.setItem('token', token);
  }
  
  return data;
};

/**
 * Login an existing user.
 * Calls: POST /auth/login
 */
export const login = async (email, password) => {
  const response = await fetch(API_BASE + '/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.message || 'Login failed');
  }
  
  // Store the JWT token in browser's localStorage
  if (data.token) {
    localStorage.setItem('token', data.token);
  }
  
  return data;
};

/**
 * Logout - simply remove the token from localStorage.
 * (No backend call needed since JWT is stateless)
 */
export const logout = () => {
  localStorage.removeItem('token');
};

/**
 * Check if user is currently logged in.
 */
export const isAuthenticated = () => {
  return !!getToken();
};

// =============================================================================
// UPLOAD APIs
// =============================================================================

/**
 * Upload a CSV file.
 * Calls: POST /api/import/csv
 * 
 * Note: For file uploads, we use FormData instead of JSON.
 */
export const uploadCSV = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await authFetch('/api/import/csv', {
    method: 'POST',
    body: formData,
  });
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.error || 'Upload failed');
  }
  
  return data;
};

/**
 * Get the status of a CSV upload job.
 * Calls: GET /api/import/status/{dbRecordId}
 */
export const getUploadStatus = async (dbRecordId) => {
  const response = await authFetch(`/api/import/status/${dbRecordId}`);
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.error || 'Failed to get status');
  }
  
  return data;
};

// =============================================================================
// PROFILE APIs
// =============================================================================

/**
 * Get user's uploaded profiles.
 * Calls: GET /api/profiles?page=X&size=Y
 */
export const getProfiles = async (page = 0, size = 10) => {
  const response = await authFetch(`/api/profiles?page=${page}&size=${size}`);
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.error || 'Failed to get profiles');
  }
  
  return data;
};

/**
 * Get a single profile by ID.
 * Calls: GET /api/profiles/{id}
 */
export const getProfile = async (id) => {
  const response = await authFetch(`/api/profiles/${id}`);
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.error || 'Failed to get profile');
  }
  
  return data;
};

// =============================================================================
// CAMPAIGN APIs (Backend to be implemented later)
// =============================================================================

/**
 * Create a new campaign.
 * NOTE: Backend endpoint doesn't exist yet - we'll build it later.
 */
export const createCampaign = async (campaignData) => {
  // TODO: Implement when backend is ready
  // const response = await authFetch('/api/campaigns', {
  //   method: 'POST',
  //   body: JSON.stringify(campaignData),
  // });
  
  // For now, just log and return success
  console.log('Campaign data to be sent:', campaignData);
  return { success: true, message: 'Campaign created (mock)' };
};

/**
 * Get user's campaigns.
 * NOTE: Backend endpoint doesn't exist yet.
 */
export const getCampaigns = async () => {
  // TODO: Implement when backend is ready
  return {
    campaigns: [],
  };
};

