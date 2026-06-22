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

---

## 5. Cloud Deployment (Render & Railway)

To deploy the entire platform in a live production environment, follow these instructions to provision database and cache instances, run the JVM container backend, and link your frontend.

### Option A: Railway (Recommended)

Railway is recommended because it manages database, Redis cache, and backend service networking under a single unified project workspace.

#### Step 1: Initialize Project
1. Log in to [Railway.app](https://railway.app).
2. Click **New Project** ➜ **Deploy from GitHub repo** and select your `nexus-hr` repository.

#### Step 2: Add Databases
1. In your project page, click **+ New** ➜ **Database** ➜ **Add PostgreSQL**.
2. Click **+ New** ➜ **Database** ➜ **Add Redis**.

#### Step 3: Configure Backend Service
1. Click the GitHub repo card service.
2. Under **Settings** ➜ **General**, set the **Root Directory** to `backend`.
3. Under **Variables**, add the following environment variables (Railway will automatically inject internal connection variables from the PostgreSQL and Redis services):
   ```env
   PORT=8080
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.DATABASE_HOST}}:${{Postgres.DATABASE_PORT}}/${{Postgres.DATABASE_NAME}}
   SPRING_DATASOURCE_USERNAME=${{Postgres.DATABASE_USER}}
   SPRING_DATASOURCE_PASSWORD=${{Postgres.DATABASE_PASSWORD}}
   SPRING_DATA_REDIS_HOST=${{Redis.REDIS_HOST}}
   SPRING_DATA_REDIS_PORT=${{Redis.REDIS_PORT}}
   SPRING_DATA_REDIS_PASSWORD=${{Redis.REDIS_PASSWORD}}
   NEXUSHR_JWT_SECRET=<your-super-long-secure-random-string-at-least-512-bits>
   NEXUSHR_CORS_ALLOWED_ORIGINS=https://<your-vercel-domain-url>
   ```
4. Under **Settings** ➜ **Networking**, click **Generate Domain** to get your public API URL (e.g. `https://nexus-hr-backend.up.railway.app`).

---

### Option B: Render Deployment

Render provides a scalable cloud environment for running Dockerized web services.

#### Step 1: Provision Databases
1. Log in to [Render.com](https://render.com).
2. Click **New** ➜ **PostgreSQL**:
   - Database Name: `nexushr`
   - User: `nexushr`
   - Copy the **Internal Database URL** once provisioned.
3. Click **New** ➜ **Redis**:
   - Copy the **Internal Redis URL**.

#### Step 2: Create Web Service
1. Click **New** ➜ **Web Service** and connect your `nexus-hr` repository.
2. Configure the service:
   - **Name**: `nexushr-backend`
   - **Environment**: `Docker`
   - **Root Directory**: `backend` (Points to the Java folder containing `Dockerfile`)
   - **Dockerfile Path**: `Dockerfile`
3. Click **Advanced** ➜ **Add Environment Variable**:
   * `SPRING_PROFILES_ACTIVE` = `prod`
   * `SPRING_DATASOURCE_URL` = `<Your Postgres Internal Database URL>` (Ensure protocol is `jdbc:postgresql://` instead of `postgres://`)
   * `SPRING_DATASOURCE_USERNAME` = `nexushr`
   * `SPRING_DATASOURCE_PASSWORD` = `<Your Postgres Password>`
   * `SPRING_DATA_REDIS_HOST` = `<Your Redis Host>` (extracted from your Redis internal URL)
   * `SPRING_DATA_REDIS_PORT` = `6379`
   * `NEXUSHR_JWT_SECRET` = `<Your Long Secret String>`
   * `NEXUSHR_CORS_ALLOWED_ORIGINS` = `https://<your-vercel-domain-url>`
4. Deploy the service to obtain your public endpoint (e.g. `https://nexushr-backend.onrender.com`).

---

## 6. Linking Vercel Frontend to Production Backend

To direct your live Vercel frontend to the production backend:

1. Open your **Vercel Project Dashboard**.
2. Go to **Settings** ➜ **Environment Variables**.
3. Create/Edit the `VITE_API_BASE_URL` environment variable:
   - **Key**: `VITE_API_BASE_URL`
   - **Value**: `https://<your-backend-domain-url>/api`
   - **Target**: Check *Production*, *Preview*, and *Development*.
4. Under the **Deployments** tab on Vercel, select the latest production build and click **Redeploy** (with "Use existing Build Cache" unchecked) to compile the Vite application with the live API base URL configuration.

