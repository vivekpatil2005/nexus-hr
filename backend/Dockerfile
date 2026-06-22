# ─── Stage 1: Build ───
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy Maven parent POM first to cache dependencies
COPY pom.xml .
COPY nexushr-common/pom.xml nexushr-common/
COPY nexushr-auth/pom.xml nexushr-auth/
COPY nexushr-employee/pom.xml nexushr-employee/
COPY nexushr-attendance/pom.xml nexushr-attendance/
COPY nexushr-payroll/pom.xml nexushr-payroll/
COPY nexushr-performance/pom.xml nexushr-performance/
COPY nexushr-ai/pom.xml nexushr-ai/
COPY nexushr-notification/pom.xml nexushr-notification/
COPY nexushr-app/pom.xml nexushr-app/

# Download all dependencies to local cache
RUN mvn dependency:go-offline -B

# Copy all source files
COPY nexushr-common/src nexushr-common/src
COPY nexushr-auth/src nexushr-auth/src
COPY nexushr-employee/src nexushr-employee/src
COPY nexushr-attendance/src nexushr-attendance/src
COPY nexushr-payroll/src nexushr-payroll/src
COPY nexushr-performance/src nexushr-performance/src
COPY nexushr-ai/src nexushr-ai/src
COPY nexushr-notification/src nexushr-notification/src
COPY nexushr-app/src nexushr-app/src

# Package the application (skip tests for image builds)
RUN mvn clean package -pl nexushr-app -am -DskipTests

# ─── Stage 2: Production JRE ───
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user and group
RUN addgroup -S nexushr && adduser -S nexushr -G nexushr

# Copy the built jar from the builder stage
COPY --from=builder /build/nexushr-app/target/nexushr-app-1.0.0-SNAPSHOT.jar app.jar

# Adjust permissions
RUN chown -R nexushr:nexushr /app

# Run as non-root user
USER nexushr

# Document port
EXPOSE 8080

# Run JVM with optimal settings for virtual threads and cloud container deployment
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
