# =============================================================================
# DOCKERFILE FOR PROFILEMAIL APPLICATION
# =============================================================================
# This Dockerfile uses a "multi-stage build" which means:
# - Stage 1 (build): Compiles your Java code into a JAR file
# - Stage 2 (runtime): Creates a small, clean image with just the JAR
#
# Why multi-stage?
# - Build stage needs Maven, JDK, source code (~500MB)
# - Runtime only needs JRE and JAR file (~200MB)
# - Final image is smaller, faster to deploy, more secure
# =============================================================================

# -----------------------------------------------------------------------------
# STAGE 1: BUILD
# -----------------------------------------------------------------------------
# We use Eclipse Temurin (free, open-source JDK) with Maven for building
FROM eclipse-temurin:17-jdk-alpine AS build

# Set working directory inside the container
# All subsequent commands run from /app
WORKDIR /app

# Copy Maven wrapper files first (for caching)
# Docker caches each layer - if these files don't change, this layer is reused
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached if pom.xml doesn't change)
# This is a trick: we download deps before copying source code
# So if only your code changes (not dependencies), this step is cached
RUN ./mvnw dependency:go-offline -B

# Now copy your source code
COPY src src

# Build the application, skip tests (we'll run tests separately)
# -DskipTests: Don't run tests during build (faster)
# package: Create the JAR file in target/ folder
RUN ./mvnw package -DskipTests

# -----------------------------------------------------------------------------
# STAGE 2: RUNTIME
# -----------------------------------------------------------------------------
# Use a smaller JRE-only image (no compiler needed at runtime)
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Copy ONLY the built JAR from the build stage
# --from=build: Copy from the 'build' stage above
# target/*.jar: The compiled JAR file
# app.jar: Rename it to app.jar for simplicity
COPY --from=build /app/target/*.jar app.jar

# Document that the app uses port 8080
# (This doesn't actually open the port, just documents it)
EXPOSE 8080

# Health check: Docker will ping this endpoint to check if app is healthy
# --interval=30s: Check every 30 seconds
# --timeout=3s: Wait max 3 seconds for response
# --start-period=60s: Give app 60 seconds to start before checking
# --retries=3: Mark unhealthy after 3 failed checks
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# The command to run when container starts
# java -jar app.jar: Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

