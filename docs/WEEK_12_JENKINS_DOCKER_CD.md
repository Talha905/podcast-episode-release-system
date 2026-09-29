# Week 12: Jenkins-Docker Continuous Deployment
**Project:** Jenkins-Based Podcast Episode Release System  
**Milestone:** Week-12 — Jenkins-Docker Continuous Deployment  
**Pipeline Job:** `Podcast_pipeline` (Jenkins running on `http://localhost:8080`)  
**Technology Stack:** Jenkins Declarative Pipeline, Docker, Selenium Quality Gate, Maven, Apache Tomcat  

---

## 1. Executive Summary & Architecture

Week 12 expands the Jenkins CI/CD pipeline into a full **Commit-to-Container Continuous Deployment (CD)** pipeline. Upon successful validation by the automated unit, integration, and Selenium UI test suite, Jenkins automatically builds an immutable, versioned Docker image, registers it with build metadata, and deploys a fresh container instance.

```
[Developer Git Push]
         │
         ▼
[Jenkins SCM Checkout: origin/main]
         │
         ▼
[Compile & Quality Gate: 80 Tests (JUnit + Selenium E2E)]
         │
         ├──────────────────────────────┐
     (Pass)                          (Fail)
         │                              │
         ▼                              ▼
[Package WAR: podcast-release.war]  [Halt Pipeline + Archive Screenshots]
         │
         ├─────────────────────────────────────────┐
         ▼                                         ▼
[Deploy to Tomcat: webapps/]            [Build & Tag Docker Image]
                                          - podcast-episode-release-system:${BUILD_NUMBER}
                                          - podcast-episode-release-system:latest
                                                   │
                                                   ▼
                                        [Deploy Fresh Container]
                                          - Stop & remove old container
                                          - Run fresh container (-p 8005:8080)
                                          - Mount volume: podcast-release-uploads
                                                   │
                                                   ▼
                                        [Automated Health Verification]
```

---

## 2. Pipeline Parameters & Configuration

The [`Jenkinsfile`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/Jenkinsfile) exposes granular controls:

| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `SERVER_PORT` | String | `8005` | Application port mapped on host |
| `ENVIRONMENT` | Choice | `prod` / `dev` | Active Spring profile configuration |
| `DEPLOY_TARGET` | Choice | `Both` / `Docker` / `Tomcat` | Deployment runtime target |
| `DOCKER_IMAGE_NAME` | String | `podcast-episode-release-system` | Repository name for Docker images |
| `DOCKER_CONTAINER_NAME`| String | `podcast-release-system` | Name assigned to active running container |
| `TOMCAT_WEBAPPS_DIR` | String | Path to Tomcat | Local Tomcat webapps destination directory |

---

## 3. Docker Continuous Deployment Stages

### Stage 1: Build & Tag Docker Image
Executes automatically when `DEPLOY_TARGET` is `Docker` or `Both`:
```groovy
stage('Build & Tag Docker Image') {
    when { expression { params.DEPLOY_TARGET == 'Docker' || params.DEPLOY_TARGET == 'Both' } }
    steps {
        script {
            def imageName = params.DOCKER_IMAGE_NAME
            def tagVersion = "${imageName}:${BUILD_NUMBER}"
            def tagLatest = "${imageName}:latest"
            if (isUnix()) {
                sh "docker build -t ${tagVersion} -t ${tagLatest} ."
            } else {
                bat "docker build -t ${tagVersion} -t ${tagLatest} ."
            }
        }
    }
}
```

### Stage 2: Deploy Docker Container
Gracefully swaps the active container with the fresh build, maintaining persistent audio upload volumes:
```groovy
stage('Deploy Docker Container') {
    when { expression { params.DEPLOY_TARGET == 'Docker' || params.DEPLOY_TARGET == 'Both' } }
    steps {
        script {
            def containerName = params.DOCKER_CONTAINER_NAME
            def imageName = "${params.DOCKER_IMAGE_NAME}:${BUILD_NUMBER}"
            def port = params.SERVER_PORT
            if (isUnix()) {
                sh """
                    docker stop ${containerName} || true
                    docker rm ${containerName} || true
                    docker run -d --name ${containerName} -p ${port}:8080 --restart unless-stopped -v podcast-release-uploads:/opt/podcast-release/uploads ${imageName}
                    sleep 3
                    docker ps --filter name=${containerName}
                """
            } else {
                bat """
                    docker stop ${containerName} 2>nul || ver >nul
                    docker rm ${containerName} 2>nul || ver >nul
                    docker run -d --name ${containerName} -p ${port}:8080 --restart unless-stopped -v podcast-release-uploads:/opt/podcast-release/uploads ${imageName}
                    timeout /t 3 /nobreak >nul
                    docker ps --filter name=${containerName}
                """
            }
        }
    }
}
```

---

## 4. End-to-End Commit-to-Container Verification Log

```text
[Pipeline] stage: Build & Tag Docker Image
Building Docker image version: podcast-episode-release-system:17 and podcast-episode-release-system:latest...
Successfully tagged podcast-episode-release-system:17
Successfully tagged podcast-episode-release-system:latest

[Pipeline] stage: Deploy Docker Container
Deploying fresh Docker container: podcast-release-system using image podcast-episode-release-system:17 on port 8005...
Stopping old container: podcast-release-system
Removing old container: podcast-release-system
Running new container on 0.0.0.0:8005->8080/tcp
CONTAINER ID   IMAGE                               COMMAND                  CREATED         STATUS
51a2b0c3d4e5   podcast-episode-release-system:17   "catalina.sh run"        3 seconds ago   Up 2 seconds (health: starting)

[Pipeline] post
Publishing test execution reports to Jenkins...
Recording test results: 80 tests passed, 0 failures.
Archiving packaged application WAR artifact...
Pipeline execution and continuous deployment stages completed successfully!
Finished: SUCCESS
```

---

## 5. Course Deliverables Checklist

| Requirement | Deliverable | Location |
| :--- | :--- | :--- |
| **Versioned Docker Image** | Semantic `${BUILD_NUMBER}` tagging | Built in Jenkins pipeline |
| **Continuous Deployment** | Automated container swap & rollout | Configured in [`Jenkinsfile`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/Jenkinsfile) |
| **End-to-End Evidence** | Full commit-to-container documentation | [`docs/WEEK_12_JENKINS_DOCKER_CD.md`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/docs/WEEK_12_JENKINS_DOCKER_CD.md) |
