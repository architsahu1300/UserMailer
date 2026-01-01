import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite is the build tool that runs our React app
// It compiles our code and serves it during development
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // Frontend runs on port 3000
    proxy: {
      // This forwards API calls to your Spring Boot backend
      // So when frontend calls /api/..., it goes to localhost:8080/api/...
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})

