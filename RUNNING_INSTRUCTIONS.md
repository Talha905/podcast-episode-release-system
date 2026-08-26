# How to Run the Podcast Episode Release System

This document provides step-by-step instructions on how to build, test, run, and interact with the **Podcast Episode Release System (Week 6 MVP Core)**.

---

## 1. Prerequisites

Ensure you have the following installed on your machine:

- **Java Development Kit (JDK)**: Java 17 or higher (Java 21 supported)
- **Apache Maven**: Version 3.9 or higher
- **Git**: For version control

Verify your environment by running:
```bash
java -version
mvn -version
```

---

## 2. Building & Running Unit/Integration Tests

To compile the codebase and run all automated unit, integration, and security tests:

```bash
mvn clean test
```

Expected output:
```text
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 3. Starting the Application

To start the Spring Boot application locally:

```bash
mvn spring-boot:run
```

By default, the application runs on **port 8080** using the `dev` profile with an in-memory **H2 database**.

---

## 4. Accessing the Application

### Web UI
Open your browser and navigate to:
- **[http://localhost:8080](http://localhost:8080)** or **[http://localhost:8080/login](http://localhost:8080/login)**

### Pre-Seeded User Accounts

The application automatically seeds three demo user accounts on startup:

| Role | Username | Password | Permissions & Actions |
|---|---|---|---|
| **Producer** | `producer` | `password123` | Create episodes, update metadata (DRAFT/VALIDATED only), transition status (`DRAFT` → `VALIDATED` → `PUBLISHED`/`FAILED`) |
| **Host** | `host` | `password123` | View episodes, view dashboard summary metrics, override episode status |
| **Admin** | `admin` | `password123` | Full access: Create, edit, view audit logs, override status |

---

## 5. Developer & Admin Tools

### H2 Database Console
To view database tables (`episodes`, `users`, `audit_logs`), open:
- **URL**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **JDBC URL**: `jdbc:h2:mem:podcastdb`
- **User Name**: `sa`
- **Password**: *(leave blank)*

---

## 6. REST API Endpoints

All REST API endpoints support **HTTP Basic Authentication** as well as session login.

| Method | Endpoint | Description | Role Required |
|---|---|---|---|
| `POST` | `/api/auth/login` | Authenticate & get current user details | Public |
| `GET` | `/api/episodes` | List all episodes (Supports query params: `?title=...&status=...&from=...&to=...`) | `PRODUCER`, `HOST`, `ADMIN` |
| `POST` | `/api/episodes` | Create new episode | `PRODUCER`, `ADMIN` |
| `GET` | `/api/episodes/{id}` | View single episode details | `PRODUCER`, `HOST`, `ADMIN` |
| `PUT` | `/api/episodes/{id}` | Update episode metadata (Allowed in `DRAFT` or `VALIDATED` status) | `PRODUCER`, `ADMIN` |
| `PATCH` | `/api/episodes/{id}/status` | Transition status (Body: `{"status": "VALIDATED"}`) | `PRODUCER`, `HOST`, `ADMIN` |
| `GET` | `/api/dashboard/summary` | Get status counts summary | `PRODUCER`, `HOST`, `ADMIN` |
| `GET` | `/api/episodes/{id}/audit` | View audit trail for episode | `HOST`, `ADMIN` |

---

## 7. Environment Profiles

### Development Profile (`dev` - Default)
Uses an in-memory H2 database with auto-schema updates and pre-seeded sample data.
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Profile (`prod`)
Configured for MySQL 8 database connection. You can pass environment variables for your MySQL instance:
```bash
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="podcastdb"
$env:DB_USER="root"
$env:DB_PASSWORD="your_password"

mvn spring-boot:run -Dspring-boot.run.profiles=prod
```
