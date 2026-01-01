# =============================================================================
# DOCKERFILE FOR PROFILEMAIL BACKEND (API + WORKER)
# =============================================================================
# This Dockerfile builds ONLY the Spring Boot backend.
# The frontend is built separately in frontend/Dockerfile.
#
# This Dockerfile uses a "multi-stage build":
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
WORKDIR /app

# Copy Maven wrapper files first (for caching)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Now copy your source code
COPY src src

# Build the application, skip tests
RUN ./mvnw package -DskipTests

# -----------------------------------------------------------------------------
# STAGE 2: RUNTIME
# -----------------------------------------------------------------------------
# Use a smaller JRE-only image (no compiler needed at runtime)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Document that the app uses port 8080
EXPOSE 8080

# Health check for Docker
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

