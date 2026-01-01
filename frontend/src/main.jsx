/**
 * main.jsx - THE ENTRY POINT
 * ===========================
 * This is where React starts. It takes control of the <div id="root"> 
 * in index.html and renders our entire app inside it.
 * 
 * Think of it like the "main" method in Java - everything starts here!
 */

import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

// This line finds the <div id="root"> and tells React to manage everything inside it
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)

