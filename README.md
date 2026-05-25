# 🚀 NexusHR — AI-Enabled Enterprise HR & Workforce Intelligence Platform

<div align="center">

![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-24+-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-Confidential-red?style=for-the-badge)

**Production-grade HR management platform covering the complete employee lifecycle**
**From onboarding to offboarding with real-time analytics & AI insights**

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [API Documentation](#-api-documentation)
- [Default Credentials](#-default-credentials)
- [Development Guide](#-development-guide)

---

## ✨ Features

| Module | Capabilities |
|--------|-------------|
| **Employee Lifecycle** | Onboarding, profile management, org-chart, role assignment, offboarding |
| **Attendance & Leave** | Biometric simulation, real-time SSE dashboard, configurable leave types, multi-level approval |
| **Payroll Engine** | CTC-based salary calculation, Indian tax deductions (PF/ESI/TDS), Spring Batch processing, PDF payslips |
| **Performance Management** | OKR/SMART goals, 360° feedback, weighted scoring, bell-curve distribution |
| **AI Workforce Intelligence** | Predictive attrition model (>80% accuracy), skill gap radar, engagement scoring, RAG chatbot |
| **Dashboards** | Role-based views, live metrics, department cost breakdown, PDF/Excel export |
| **Notifications** | Email + SMS for approvals, payslip dispatch, announcements |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   React 19 SPA (Vite)                   │
│          shadcn/ui + Tailwind CSS v4 + TanStack         │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS / REST API
┌──────────────────────▼──────────────────────────────────┐
│              Spring Boot 3.3 (Java 21 LTS)              │
│  ┌────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐  │
│  │  Auth  │ │ Employee │ │Attendance│ │   Payroll   │  │
│  │Service │ │ Service  │ │ Service  │ │   Engine    │  │
│  └────────┘ └──────────┘ └──────────┘ └─────────────┘  │
│  ┌────────────┐ ┌──────────┐ ┌──────────────────────┐  │
│  │Performance │ │    AI    │ │    Notification      │  │
│  │  Service   │ │ Service  │ │      Service         │  │
│  └────────────┘ └────┬─────┘ └──────────────────────┘  │
└───────────┬──────────┼──────────────────────────────────┘
            │          │
    ┌───────▼───┐  ┌───▼──────────┐
    │PostgreSQL │  │ FastAPI      │
    │17+PgVector│  │ AI Sidecar   │
    └───────────┘  │ (Python)     │
    ┌───────────┐  └──────────────┘
    │  Redis 7  │
    └───────────┘
```

---

## 🛠 Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend | Spring Boot | 3.3.x |
| Language | Java | 21 LTS |
| Frontend | React + TypeScript | 19 / 5.x |
| Build Tool | Vite | 5.x |
| UI Components | shadcn/ui + Tailwind CSS | Latest / v4 |
| Database | PostgreSQL + PgVector | 17 |
| Cache | Redis | 7+ |
| Auth | Spring Security 6 + JWT | 6.x |
| Batch | Spring Batch | 5.x |
| AI | Spring AI + OpenAI GPT-4o | 1.x |
| Containerisation | Docker | 24+ |
| Monitoring | Prometheus + Grafana | Latest |

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** (JDK 21 LTS)
- **Maven 3.9+**
- **Node.js 20+** and **npm 10+**
- **Docker** and **Docker Compose**

### 1. Clone the repository

```bash
git clone https://github.com/your-org/nexus-hr.git
cd nexus-hr
```

### 2. Start infrastructure services

```bash
docker compose up -d postgres redis minio mailhog
```

### 3. Build and run the backend

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -pl nexushr-app
```

The backend will start at **http://localhost:8080**

### 4. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start at **http://localhost:5173**

### 5. Access the application

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MailHog | http://localhost:8025 |
| MinIO Console | http://localhost:9001 |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |

---

## 🔐 Default Credentials

| User | Username | Password | Role |
|------|----------|----------|------|
| CEO / Admin | admin | NexusHR@2026 | ADMIN, HR_MANAGER |
| CPO | priya.sharma | NexusHR@2026 | HR_MANAGER |
| CTO | vikram.mehta | NexusHR@2026 | MANAGER |
| HR Manager | ananya.reddy | NexusHR@2026 | HR_MANAGER |
| Eng Manager | arjun.desai | NexusHR@2026 | MANAGER |
| Employee | sneha.gupta | NexusHR@2026 | EMPLOYEE |

---

## 📁 Project Structure

```
nexus-hr/
├── backend/                      # Maven multi-module Spring Boot
│   ├── nexushr-common/           # Shared DTOs, exceptions, audit
│   ├── nexushr-auth/             # JWT auth, Spring Security
│   ├── nexushr-employee/         # Employee CRUD, org-chart
│   ├── nexushr-attendance/       # Attendance, leave management
│   ├── nexushr-payroll/          # Payroll engine, Spring Batch
│   ├── nexushr-performance/      # Goals, reviews, feedback
│   ├── nexushr-ai/               # AI/ML integration
│   ├── nexushr-notification/     # Email, SMS notifications
│   └── nexushr-app/              # Main application assembly
├── frontend/                     # React 19 + Vite + TypeScript
├── ai-sidecar/                   # Python FastAPI ML service
├── docker/                       # Docker configurations
├── k8s/                          # Kubernetes + Helm charts
├── scripts/                      # Utility scripts
└── docs/                         # Documentation
```

---

## 📖 API Documentation

Full API docs are available via Swagger UI at: **http://localhost:8080/swagger-ui.html**

### Key Endpoints

| Module | Endpoint | Description |
|--------|----------|-------------|
| Auth | `POST /api/auth/login` | User login |
| Employees | `GET /api/employees` | List employees |
| Attendance | `GET /api/attendance/stream` | SSE live feed |
| Payroll | `POST /api/payroll/run` | Trigger payroll |
| Performance | `GET /api/performance/scorecard/{id}` | Performance scorecard |
| AI | `POST /api/ai/chatbot/query` | HR chatbot |

---

## 🧪 Development Guide

### Running Tests

```bash
# Unit tests
cd backend && mvn test

# Integration tests (requires Docker for Testcontainers)
cd backend && mvn verify -P integration-tests

# Frontend tests
cd frontend && npm test

# E2E tests
cd frontend && npx playwright test
```

### Code Quality

```bash
# Java linting
mvn checkstyle:check spotbugs:check

# Frontend linting
cd frontend && npm run lint
```

---

## 📄 License

**Confidential** — Zidio Development. All rights reserved.

---

<div align="center">
<b>NexusHR — Crafted with precision and modern engineering principles</b><br/>
Zidio Development • Java Full-Stack Domain • March 2026
</div>
