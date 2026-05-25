# NexusHR — Deployment & Verification Guide

This guide details the procedures for building production container images, deploying on Kubernetes via Helm, running Testcontainers integration tests, and running frontend E2E tests using Playwright.

---

## 1. Production Docker Images

We use multi-stage Docker builds to produce optimized, secure, and minimal production containers.

### Backend Service (Modular Spring Boot)
Build the backend container image from the project root:
```bash
docker build -t nexushr-backend -f backend/Dockerfile backend/
```
- **Builder Stage**: Uses JDK 25 and Maven to download dependencies, compile all modules, and package the application.
- **Runner Stage**: Employs `eclipse-temurin:25-jre-alpine` for a lightweight, secure footprint. The application runs under a dedicated, non-root user `nexushr` on port `8080`.

### Frontend Service (React + Vite)
Build the frontend static asset server:
```bash
docker build -t nexushr-frontend -f frontend/Dockerfile frontend/
```
- **Builder Stage**: Uses Node.js 22 to install packages and perform static build production packaging (`npm run build`).
- **Server Stage**: Uses an Nginx alpine configuration configured to support standard HTML5 History API routing fallbacks for single-page applications. Exposes port `80`.

---

## 2. Kubernetes Deployment (Helm)

We provide a Helm chart at [helm/nexushr/](file:///Users/vivekpatil/Documents/nexus-hr/helm/nexushr) to easily orchestrate deployments on Kubernetes (e.g. Minikube, Kind, EKS, GKE).

### Configuration Properties
Custom values can be adjusted in [values.yaml](file:///Users/vivekpatil/Documents/nexus-hr/helm/nexushr/values.yaml):
- Replica counts for high-availability.
- Connection settings for Postgres and Redis.
- CPU/Memory requests and resource limits.

### Verify Chart Linting
```bash
helm lint helm/nexushr
```

### Install Chart
```bash
helm install nexushr helm/nexushr --namespace nexushr --create-namespace
```

---

## 3. Testcontainers Integration Testing

Integration tests verify migrations and core API controller flows using isolated database instances.

### How to Run
Execute the integration test suite:
```bash
mvn test -pl nexushr-app -Dtest=NexusHrIntegrationTest
```
- **Testcontainers Flow**: The test automatically bootstraps a temporary Postgres 17 container, runs Flyway migrations from scratch, seeds the initial user records, and asserts the HTTP status of the actuator health check and authentication endpoint.
- **Graceful Fallback**: If running on local Docker setups with client-protocol mismatches (e.g. Docker Desktop version overrides on macOS), the test suite automatically falls back to connecting to the local Postgres container (port `5432`) running via docker-compose.

---

## 4. E2E UI Automation (Playwright)

End-to-End tests verify user-facing features on Chromium, Firefox, and WebKit.

### Prerequisites
1. Open a terminal in the [frontend/](file:///Users/vivekpatil/Documents/nexus-hr/frontend/) directory.
2. Install Playwright and browser drivers:
   ```bash
   npm install -D @playwright/test
   npx playwright install
   ```

### Execution
With both backend (port `8080`) and frontend (port `5173`) running:
```bash
npx playwright test
```
The suite verifies:
* Successful authentication flow and redirection to `/` dashboard.
* Error toast feedback on bad credentials.
* Navigation and card elements rendering on the dashboard.
* Main dashboard elements and sidebar logout actions.
