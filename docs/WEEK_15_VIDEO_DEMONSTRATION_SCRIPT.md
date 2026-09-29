# Week 15: Final End-to-End DevOps Video Demonstration Guide & Script

**Project:** Jenkins-Based Podcast Episode Release System  
**Milestone:** Week-15 — Final End-to-End Release, Documentation & Viva  
**Target Audience:** Evaluators, Professors, Viva Committee, DevOps Reviewers  
**Target Video Duration:** 8 to 12 minutes  

---

## Pre-Recording Checklist (Setup Before Recording)

Ensure the following tools and services are running before pressing **Record**:
- [x] **Docker Desktop** is open and running.
- [x] **Jenkins** is running at [http://localhost:8080](http://localhost:8080).
- [x] Container is running on port 8005 (`docker ps`).
- [x] Application browser tab ready: [http://localhost:8005/login](http://localhost:8005/login).
- [x] Terminal (PowerShell or VS Code terminal) open in `C:\Users\thele\OneDrive\Desktop\podcast-episode-release-system`.
- [x] GitHub repository open in browser: `https://github.com/Talha905/podcast-episode-release-system`.
- [x] Recording software ready (OBS Studio, Windows Game Bar `Win + G`, or Zoom recording).

---

## Detailed Step-by-Step Video Script & Action Plan

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 VIDEO SCENE FLOW TIMELINE                                        │
├───────────────┬───────────────────────────┬──────────────────────────────────────────────────────┤
│ Scene 1       │ 00:00 - 01:30 (1.5 mins)  │ Title, Objectives & End-to-End DevOps Toolchain      │
│ Scene 2       │ 01:30 - 03:00 (1.5 mins)  │ Architecture, Codebase Structure & Git Governance    │
│ Scene 3       │ 03:00 - 04:30 (1.5 mins)  │ Automated Testing Suite & Selenium Quality Gate      │
│ Scene 4       │ 04:30 - 07:00 (2.5 mins)  │ Jenkins CI/CD Pipeline & Commit-to-Container Rollout │
│ Scene 5       │ 07:00 - 09:00 (2.0 mins)  │ Live Container Demo & Role-Based Podcast Workflow    │
│ Scene 6       │ 09:00 - 10:30 (1.5 mins)  │ Ansible Configuration Management, Idempotency & Probe│
│ Scene 7       │ 10:30 - 11:30 (1.0 min)   │ Viva Defense Summary, Best Practices & Conclusion    │
└───────────────┴───────────────────────────┴──────────────────────────────────────────────────────┘
```

---

### Scene 1: Introduction & DevOps Architecture (00:00 - 01:30)

#### What to Show on Screen:
1. Show your Title Slide or README in VS Code / GitHub: **"Jenkins-Based Podcast Episode Release System — Final DevOps Demonstration (Week 15)"**.
2. Show the GitHub repository (`https://github.com/Talha905/podcast-episode-release-system`).

#### What to Say (Speaking Script):
> *"Hello everyone and welcome to the final end-to-end DevOps demonstration of the **Jenkins-Based Podcast Episode Release System** for Week 15.*
> 
> *The goal of this system is to solve the operational bottlenecks of media publishing by replacing manual, error-prone podcast release steps with a fully automated, auditable, and resilient DevOps lifecycle.*
> 
> *Over the past 15 weeks, we have engineered a complete DevOps toolchain comprising:*
> 1. *A Spring Boot enterprise application with role-based governance.*
> 2. *Git branching policies and semantic commit discipline on GitHub.*
> 3. *Comprehensive test automation with 80 JUnit and Selenium E2E tests acting as an automated Quality Gate.*
> 4. *Jenkins Declarative CI/CD pipeline automating compilation, packaging, and versioned Docker deployments.*
> 5. *Docker and Docker Compose multi-stage containerization with non-root security.*
> 6. *Ansible configuration management ensuring automated provisioning, idempotency, health checks, and rollback recovery.*
> 
> *Let us now inspect the project structure and technical implementation."*

---

### Scene 2: Codebase Architecture & Git Governance (01:30 - 03:00)

#### What to Show on Screen:
1. In VS Code, expand the project tree:
   - `src/main/java/com/podcastrelease/` (Controllers, Models, Services, Security).
   - `src/test/java/com/podcastrelease/` (Unit, Integration, and Selenium tests).
   - `Dockerfile` and `docker-compose.yml`.
   - `Jenkinsfile`.
   - `ansible/` directory (Roles, Tasks, Templates, Playbooks).
   - `docs/` directory showing documentation for Weeks 1 to 14.
2. In the terminal, run:
   ```powershell
   git status
   git log --oneline -n 5
   ```

#### What to Say (Speaking Script):
> *"Here in our codebase, we follow a clean, layered architecture built on Java 17 and Spring Boot. We have separated concerns across controllers, business services, and repositories supporting an H2 embedded database for fast, isolated testing and development.*
> 
> *Notice our configuration-as-code files directly in the root:*
> - *Our multi-stage `Dockerfile` creating secure, non-root Tomcat runtime containers.*
> - *Our `Jenkinsfile` specifying the declarative pipeline as code.*
> - *Our `ansible` directory containing playbooks for idempotent target node provisioning.*
> - *And our `docs` folder containing complete technical specifications and reports for every single milestone.*
> 
> *Our Git repository is completely clean, synchronized with GitHub `main`, and follows semantic commit conventions."*

---

### Scene 3: Automated Testing & Selenium Quality Gate (03:00 - 04:30)

#### What to Show on Screen:
1. Open [`src/test/java/com/podcastrelease/selenium/PodcastReleaseSystemSeleniumTest.java`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/src/test/java/com/podcastrelease/selenium/PodcastReleaseSystemSeleniumTest.java) and [`ScreenshotOnFailureExtension.java`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/src/test/java/com/podcastrelease/selenium/ScreenshotOnFailureExtension.java).
2. Point out the 5 critical user journeys tested (Login, Episode Creation, Admin Approval, Auditor Verification, Validation/Access Control).
3. In PowerShell terminal, run:
   ```powershell
   mvn test
   ```
4. Show the build output ending in **`BUILD SUCCESS`** with **80 tests run, 0 failures, 0 errors**.

#### What to Say (Speaking Script):
> *"Next is our automated testing and Quality Gate. In Week 9 and Week 10, we established that no unverified code ever reaches production.*
> 
> *We have 80 automated tests spanning unit logic, Spring MockMvc security boundaries, and Selenium WebDriver end-to-end tests.*
> 
> *Notice our Selenium test suite runs headless Chrome covering full user flows from Creator episode submission to Admin approval. We also have a JUnit 5 failure extension that automatically captures full-page screenshots if any UI journey fails.*
> 
> *As you can see on the terminal, all 80 tests pass cleanly in under 20 seconds. If even one test were to fail in our Jenkins pipeline, the quality gate triggers, the build fails immediately, screenshots are archived for triage, and deployment is aborted."*

---

### Scene 4: Jenkins CI/CD Pipeline & Automated Docker CD (04:30 - 07:00)

#### What to Show on Screen:
1. Switch browser to Jenkins: **`http://localhost:8080/job/Podcast_pipeline/`**.
2. Show the **Stage View** of previous builds with green checkmarks across:
   - `Checkout SCM`
   - `Compile & Test`
   - `Package WAR`
   - `Build & Tag Docker Image`
   - `Deploy Docker Container`
3. Click on the latest build (e.g. Build #17 or #18) -> Click **Test Result**: show **80 tests passed, 0 failures**.
4. Click **Build with Parameters**:
   - Show parameters: `DEPLOY_TARGET = Docker`, `SERVER_PORT = 8005`, `ENVIRONMENT = dev`.
   - Click **Build** to show the pipeline running live.
   - Click on the running build -> **Console Output** to show the live log stream.

#### What to Say (Speaking Script):
> *"Now let us examine our Jenkins Continuous Deployment pipeline. This is the heart of our automation.*
> 
> *In Jenkins, our pipeline job `Podcast_pipeline` pulls the code from GitHub, compiles it, and runs the entire 80-test quality gate.*
> 
> *Once the tests pass, Jenkins packages the WAR file and triggers our Docker Continuous Deployment stage added in Week 12.*
> 
> *Notice how Jenkins builds an immutable Docker image and tags it with the exact build number as well as `latest`. It then executes an automated container rollout: gracefully stopping the older container, removing it, and starting a fresh container on port 8005 with persistent upload volumes.*
> 
> *The entire commit-to-container deployment runs without any human intervention and finishes with a SUCCESS status."*

---

### Scene 5: Live Container Demonstration & Application Walkthrough (07:00 - 09:00)

#### What to Show on Screen:
1. In PowerShell, show container status:
   ```powershell
   docker ps
   curl.exe -I http://localhost:8005/login
   ```
2. In browser, navigate to: **`http://localhost:8005/login`**.
3. **Login as Creator (`creator` / `creator123`)**:
   - Show Creator Dashboard.
   - Click **Create Episode**: Fill in Title (*"DevOps Finale Special"*), Season 1, Episode 15, Summary (*"Automated release demonstration"*), Audio file upload.
   - Click **Save / Submit**. Show episode status is **DRAFT / PENDING_REVIEW**.
   - Log out.
4. **Login as Admin (`admin` / `admin123`)**:
   - Show Admin Review Queue.
   - Find the newly submitted episode.
   - Click **Approve & Schedule / Release**.
   - Show status change to **RELEASED / PUBLISHED**.
   - Log out.
5. **Login as Auditor (`auditor` / `auditor123`)**:
   - Show the read-only Audit Trail and verify all timestamps, role transitions, and release events are immutably logged.

#### What to Say (Speaking Script):
> *"Let's verify the live running container. Running `docker ps` shows container `podcast-release-system` healthy and mapped to host port 8005.*
> 
> *Now let's open `http://localhost:8005/login` and demonstrate the core three-role business workflow:*
> 1. *First, we log in as the **Creator**. The Creator can draft podcast episodes, configure metadata, and upload media files. Once submitted, the episode enters `PENDING_REVIEW`.*
> 2. *Next, we log in as the **Admin**. The Admin dashboard has exclusive authorization to review pending episodes, verify compliance, and trigger the release. We approve and publish the episode.*
> 3. *Finally, we log in as the **Auditor**. The Auditor has a dedicated view to inspect release histories, audit logs, and status transitions without modifying production data.*
> 
> *The container handles file uploads into persistent Docker volumes, ensuring no media is lost across restarts."*

---

### Scene 6: Ansible Configuration Management, Idempotency & Reliability (09:00 - 10:30)

#### What to Show on Screen:
1. Open [`ansible/playbooks/site.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/site.yml) and [`ansible/roles/podcast_release/tasks/main.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/roles/podcast_release/tasks/main.yml) in VS Code.
   - Highlight package installations (OpenJDK 17, Docker, UFW), dedicated user `podcast`, directory permissions, and systemd service template.
2. Open [`ansible/playbooks/provision_and_validate.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/provision_and_validate.yml) and [`ansible/playbooks/rollback.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/rollback.yml).
3. In PowerShell terminal, run the automated health check probe:
   ```powershell
   python scripts/health_check.py --url http://localhost:8005/login
   ```
   Show output: **HTTP 200 OK, latency under 50ms, health check PASSED**.

#### What to Say (Speaking Script):
> *"In Weeks 13 and 14, we elevated infrastructure management using Ansible for automated server provisioning and disaster recovery.*
> 
> *Instead of manually configuring target servers, our Ansible role `podcast_release` codifies all dependencies: installing Java 17 and Docker, enforcing firewall rules, creating secure system users, and provisioning systemd services.*
> 
> *To guarantee infrastructure reliability:*
> 1. *We proved **Idempotency**: re-running `provision_and_validate.yml` yields `changed=0`, guaranteeing no unintended side-effects or drift.*
> 2. *We built an automated health check probe in `scripts/health_check.py` which verifies HTTP 200 response codes, response latency, and page integrity.*
> 3. *And in `rollback.yml`, we established automated disaster recovery: if an unviable release is detected, the playbook automatically restores the previous stable backup, restarts the service, and verifies recovery.*
> 
> *Running our automated health check probe right now confirms the system is fully operational and healthy."*

---

### Scene 7: Viva Defense Summary & Conclusion (10:30 - 11:30)

#### What to Show on Screen:
1. Switch back to the GitHub repository / project documentation folder in VS Code showing:
   - `docs/WEEK_10_CONTINUOUS_TESTING.md`
   - `docs/WEEK_11_DOCKER_CONTAINER_LIFECYCLE.md`
   - `docs/WEEK_12_JENKINS_DOCKER_CD.md`
   - `docs/WEEK_13_CONFIGURATION_MANAGEMENT.md`
   - `docs/WEEK_14_AUTOMATED_PROVISIONING_RELIABILITY.md`
2. Show the final slide / closing screen.

#### What to Say (Speaking Script):
> *"To conclude, the Jenkins-Based Podcast Episode Release System demonstrates the full power of modern DevOps:*
> - *Agile development backed by automated testing.*
> - *Zero-touch Continuous Integration and Continuous Deployment.*
> - *Isolated, portable containerization with Docker.*
> - *And repeatable, idempotent infrastructure as code with Ansible.*
> 
> *Every milestone from Week 1 to Week 15 is rigorously tested, fully functional, and thoroughly documented in our repository.*
> 
> *Thank you very much for your time and evaluation. I am now ready for any viva questions."*

---

## Quick Reference: Commands to Execute During Video

| Stage | Command to Run | Expected Output |
| :--- | :--- | :--- |
| **Git Status** | `git status` | `On branch main`, working tree clean |
| **Git Log** | `git log --oneline -n 5` | Recent commits with semantic tags |
| **Run Tests** | `mvn test` | `BUILD SUCCESS`, 80 tests passed |
| **Docker Status** | `docker ps` | `podcast-release-system` up on port `8005` |
| **Container Probe**| `curl.exe -I http://localhost:8005/login` | `HTTP/1.1 200` |
| **Health Probe** | `python scripts/health_check.py --url http://localhost:8005/login` | Health check passed, latency < 50ms |
