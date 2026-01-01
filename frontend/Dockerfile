# =============================================================================
# DOCKERFILE FOR PROFILEMAIL FRONTEND
# =============================================================================
# This Dockerfile builds the React frontend and serves it with Nginx.
#
# Multi-stage build:
# - Stage 1 (build): Builds React app into optimized static files
# - Stage 2 (runtime): Serves static files with Nginx
#
# Nginx is used because:
# - Extremely fast at serving static files
# - Handles gzip compression automatically
# - Can act as reverse proxy to backend API
# - Production-grade web server used by millions of sites
# =============================================================================

# -----------------------------------------------------------------------------
# STAGE 1: BUILD
# -----------------------------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first for better Docker layer caching
# If package.json doesn't change, npm install is cached
COPY package*.json ./

# Install dependencies
# npm ci is faster and more reliable than npm install for CI/CD
RUN npm ci --silent

# Copy source code
COPY . .

# Build the production bundle
# This creates optimized, minified files in /app/dist
RUN npm run build

# -----------------------------------------------------------------------------
# STAGE 2: RUNTIME (Nginx)
# -----------------------------------------------------------------------------
FROM nginx:alpine

# Copy our custom Nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy the built static files from Stage 1
# Nginx serves files from /usr/share/nginx/html by default
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port 80 (HTTP)
EXPOSE 80

# Nginx runs in the foreground (required for Docker)
CMD ["nginx", "-g", "daemon off;"]

