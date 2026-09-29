# Week 11: Docker Image and Container Lifecycle
**Project:** Jenkins-Based Podcast Episode Release System  
**Milestone:** Week-11 — Docker Image and Container Lifecycle  
**Technology Stack:** Docker, Docker Compose, Multi-stage Dockerfile, Eclipse Temurin JDK 17, Apache Tomcat 10.1  

---

## 1. Executive Summary & Scope

Week 11 focuses on containerizing the **Podcast Episode Release System** to achieve environment consistency across development, testing, CI/CD, and production. The key accomplishments include:
* Creating a secure, multi-stage production [`Dockerfile`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/Dockerfile).
* Declaring an orchestration configuration in [`docker-compose.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/docker-compose.yml).
* Implementing automated container health checks and non-root execution (`appuser:appgroup`).
* Documenting the complete container lifecycle operations (Build, Tag, Run, Inspect, Exec, Stop, Restart, Remove).

---

## 2. Multi-Stage Dockerfile Architecture

The build process is divided into two distinct stages:

```
[Source Code + pom.xml]
        │
        ▼
┌───────────────────────────────────────────────┐
│ Stage 1: Builder (maven:3.9.6-temurin-17)      │
│  - Downloads dependencies (cached layer)      │
│  - Compiles Java source and packages WAR     │
└───────────────────────────────────────────────┘
        │ (extract target/podcast-release.war)
        ▼
┌───────────────────────────────────────────────┐
│ Stage 2: Runtime (tomcat:10.1-jdk17-jammy)    │
│  - Strips default webapps                     │
│  - Copies WAR to /usr/local/tomcat/webapps/   │
│  - Creates non-root 'appuser'                 │
│  - Configures HEALTHCHECK                     │
│  - Exposes port 8080                          │
└───────────────────────────────────────────────┘
        │
        ▼
[Production Container: ~380MB, Minimal Attack Surface]
```

### Security & Production Enhancements:
1. **Non-Root Execution:** Container runs under `appuser` (UID/GID isolated) to mitigate container escape vulnerabilities.
2. **Attack Surface Reduction:** Default Tomcat manager, host-manager, and example webapps are purged during build.
3. **Healthcheck Probe:** Built-in `HEALTHCHECK` periodically verifies `http://localhost:8080/login` availability.
4. **Persistent Volumes:** Audio uploads (`/opt/podcast-release/uploads`) and logs are mapped to named volumes so data survives container lifecycle restarts.

---

## 3. Complete Container Lifecycle Operations

### Step 1: Building the Docker Image
To build the image and tag it with semantic versioning:
```powershell
docker build -t podcast-episode-release-system:1.0.0 -t podcast-episode-release-system:latest .
```

### Step 2: Verifying Image Details & Size
```powershell
docker images | findstr podcast-episode-release-system
```
*Expected Output:*
```text
REPOSITORY                       TAG       IMAGE ID       CREATED          SIZE
podcast-episode-release-system   1.0.0     a83f912c4b10   10 seconds ago   385MB
podcast-episode-release-system   latest    a83f912c4b10   10 seconds ago   385MB
```

### Step 3: Running the Container (Port Mapping & Volumes)
Run the container in detached mode, mapping host port `8005` to container port `8080`:
```powershell
docker run -d \
  --name podcast-release-system \
  -p 8005:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v podcast-release-uploads:/opt/podcast-release/uploads \
  --restart unless-stopped \
  podcast-episode-release-system:1.0.0
```

### Step 4: Inspecting Container Status and Health
```powershell
docker ps --filter name=podcast-release-system
```
*Check real-time health probe status:*
```powershell
docker inspect --format='{{json .State.Health}}' podcast-release-system
```

### Step 5: Streaming Logs
```powershell
docker logs -f podcast-release-system
```
*Confirm Spring Boot and Tomcat startup:*
```text
INFO [main] org.apache.catalina.startup.Catalina.start Server startup in [3824] milliseconds
INFO [main] c.p.PodcastReleaseApplication: Started PodcastReleaseApplication in 4.2 seconds
```

### Step 6: Executing Commands Inside the Container
```powershell
# Open interactive shell as appuser
docker exec -it podcast-release-system bash

# Check disk space in uploads directory
docker exec podcast-release-system df -h /opt/podcast-release/uploads
```

### Step 7: Container Stopping, Restarting, and Lifecycle Removal
```powershell
# Stop container gracefully
docker stop podcast-release-system

# Restart container
docker restart podcast-release-system

# Remove container
docker rm -f podcast-release-system
```

---

## 4. Docker Compose Quickstart

For automated multi-container lifecycle management:
```powershell
# Start container in detached mode
docker-compose up -d

# Check status
docker-compose ps

# View aggregate logs
docker-compose logs -f podcast-app

# Teardown containers (preserving persistent volumes)
docker-compose down
```

---

## 5. Course Deliverables Checklist

| Requirement | Deliverable | Location |
| :--- | :--- | :--- |
| **Dockerfile** | Multi-stage builder & runtime | [`Dockerfile`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/Dockerfile) |
| **Docker Compose** | Orchestration configuration | [`docker-compose.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/docker-compose.yml) |
| **Container Lifecycle** | Operations & commands manual | [`docs/WEEK_11_DOCKER_CONTAINER_LIFECYCLE.md`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/docs/WEEK_11_DOCKER_CONTAINER_LIFECYCLE.md) |
| **Security Hardening** | Non-root user & Healthcheck | Verified in Dockerfile layers |
