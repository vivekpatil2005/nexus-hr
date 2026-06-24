# ─── Stage 1: Build Frontend ───
FROM node:22-slim AS frontend-builder
WORKDIR /frontend-build
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ─── Stage 2: Build Backend ───
FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
WORKDIR /build

# Copy Maven parent POM first to cache dependencies
COPY backend/pom.xml .
COPY backend/nexushr-common/pom.xml nexushr-common/
COPY backend/nexushr-auth/pom.xml nexushr-auth/
COPY backend/nexushr-employee/pom.xml nexushr-employee/
COPY backend/nexushr-attendance/pom.xml nexushr-attendance/
COPY backend/nexushr-payroll/pom.xml nexushr-payroll/
COPY backend/nexushr-performance/pom.xml nexushr-performance/
COPY backend/nexushr-ai/pom.xml nexushr-ai/
COPY backend/nexushr-notification/pom.xml nexushr-notification/
COPY backend/nexushr-app/pom.xml nexushr-app/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy all source files
COPY backend/ /build/

# Copy the built frontend static assets into the Spring Boot resource directory
COPY --from=frontend-builder /frontend-build/dist /build/nexushr-app/src/main/resources/static/

# Package the application (skip tests for image builds)
RUN mvn clean package -pl nexushr-app -am -DskipTests

# ─── Stage 3: Production JRE ───
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create a non-root user and group
RUN groupadd -r nexushr && useradd -r -g nexushr nexushr

# Copy the built jar from the builder stage
COPY --from=backend-builder /build/nexushr-app/target/*.jar app.jar

# Adjust permissions
RUN chown -R nexushr:nexushr /app

# Run as non-root user
USER nexushr

# Document port
EXPOSE 8080

# Run JVM with optimal settings for virtual threads and cloud container deployment
ENTRYPOINT ["java", "-jar", "app.jar"]
