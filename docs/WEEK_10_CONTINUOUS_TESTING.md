# Week 10: Continuous Testing in Jenkins
**Project:** Jenkins-Based Podcast Episode Release System  
**Milestone:** Week-10 — Continuous Testing in Jenkins  
**Pipeline Job:** `Podcast_pipeline` (Jenkins running on `http://localhost:8080`)  
**Technology Stack:** Jenkins Declarative Pipeline, Selenium WebDriver, JUnit 5, Maven Surefire Plugin, Apache Tomcat 11  

---

## 1. Executive Summary & Objectives

In Week 10, the end-to-end Selenium WebDriver test suite created in Week 9 was integrated into the Jenkins CI/CD pipeline as an automated Quality Gate. The pipeline was configured to:
1. Automatically run the full unit, integration, and Selenium UI test suite during every build.
2. Parse and publish test reports into Jenkins using the `junit` pipeline step.
3. Automatically capture and archive failure screenshots and DOM dumps to the Jenkins build artifacts whenever any UI test fails.
4. Enforce a strict Quality Gate: any test failure immediately halts the pipeline, preventing defective code from reaching the `Package WAR` or `Deploy to Tomcat` stages.
5. Demonstrate a complete defect lifecycle: introduce a deliberate defect, capture failure evidence and stopped deployment, commit the defect correction, and verify a successful rerun.

---

## 2. Pipeline Configuration (`Jenkinsfile`)

The [`Jenkinsfile`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/Jenkinsfile) was updated with the following continuous testing stages and post-actions:

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'SERVER_PORT', defaultValue: '8005', description: 'Target application server port')
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target deployment environment profile')
        string(name: 'TOMCAT_WEBAPPS_DIR', defaultValue: 'C:\\Users\\thele\\Downloads\\apache-tomcat-11.0.25\\apache-tomcat-11.0.25\\webapps', description: 'Path to target Tomcat webapps directory')
    }

    stages {
        stage('Checkout SCM') {
            steps {
                echo 'Checking out source code from Git repository...'
                checkout scm
            }
        }

        stage('Compile & Test') {
            steps {
                echo 'Executing unit, integration, and security test suite...'
                script {
                    if (isUnix()) {
                        sh 'mvn clean test'
                    } else {
                        bat 'where mvn >nul 2>&1 && mvn clean test || "C:\\Users\\thele\\apache-maven-3.9.16\\bin\\mvn.cmd" clean test'
                    }
                }
            }
        }

        stage('Package WAR') {
            steps {
                echo "Packaging WAR artifact for environment profile: ${params.ENVIRONMENT}..."
                script {
                    if (isUnix()) {
                        sh 'mvn package -DskipTests'
                    } else {
                        bat 'where mvn >nul 2>&1 && mvn package -DskipTests || "C:\\Users\\thele\\apache-maven-3.9.16\\bin\\mvn.cmd" package -DskipTests'
                    }
                }
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                script {
                    def tomcatDir = (params.TOMCAT_WEBAPPS_DIR && params.TOMCAT_WEBAPPS_DIR.trim()) ? params.TOMCAT_WEBAPPS_DIR.trim() : 'C:\\Users\\thele\\Downloads\\apache-tomcat-11.0.25\\apache-tomcat-11.0.25\\webapps'
                    echo "Deploying target/podcast-release.war to Tomcat directory: ${tomcatDir}"
                    if (isUnix()) {
                        sh """
                            mkdir -p "${tomcatDir}"
                            cp target/podcast-release.war "${tomcatDir}/"
                            echo "Successfully copied podcast-release.war to ${tomcatDir}"
                        """
                    } else {
                        bat """
                            IF NOT EXIST "${tomcatDir}" mkdir "${tomcatDir}"
                            copy /Y "target\\podcast-release.war" "${tomcatDir}\\podcast-release.war"
                            echo Successfully copied podcast-release.war to ${tomcatDir}
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Publishing test execution reports to Jenkins...'
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

            echo 'Archiving failure screenshots and DOM dumps if present...'
            archiveArtifacts artifacts: 'target/selenium-screenshots/**', allowEmptyArchive: true
        }
        success {
            echo 'Archiving packaged application WAR artifact...'
            archiveArtifacts artifacts: 'target/*.war', allowEmptyArchive: false
            echo 'Pipeline execution and Tomcat deployment stage completed successfully!'
        }
        failure {
            echo 'Pipeline build, test quality gate, or deployment failed. Deployment aborted.'
        }
    }
}
```

---

## 3. Deliberately Introduced Defect & Failure Evidence

### Defect Description
A regression was deliberately introduced into `src/main/resources/templates/login.html`:
```diff
- <title>Login - Podcast Episode Release System</title>
+ <title>Sign In - Podcast Portal (Defect)</title>
```

### Pipeline Failure & Quality Gate Trigger
When the automated suite ran, `PodcastReleaseSystemSeleniumTest.testJourney1_InvalidLoginShowsError` detected the deviation:
```text
[ERROR] com.podcastrelease.selenium.PodcastReleaseSystemSeleniumTest.testJourney1_InvalidLoginShowsError -- Time elapsed: 8.080 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <Login - Podcast Episode Release System> but was: <Sign In - Podcast Portal (Defect)>
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
	at com.podcastrelease.selenium.PodcastReleaseSystemSeleniumTest.testJourney1_InvalidLoginShowsError(PodcastReleaseSystemSeleniumTest.java:29)

[SELENIUM FAILURE ARTIFACT] Screenshot saved: target\selenium-screenshots\PodcastReleaseSystemSeleniumTest_testJourney1_InvalidLoginShowsError_20260928_230347.png
[SELENIUM FAILURE ARTIFACT] Page DOM source saved: target\selenium-screenshots\PodcastReleaseSystemSeleniumTest_testJourney1_InvalidLoginShowsError_20260928_230347_source.html
[SELENIUM FAILURE ARTIFACT] Failure cause: expected: <Login - Podcast Episode Release System> but was: <Sign In - Podcast Portal (Defect)>
```

### Quality Gate Outcome
* **Pipeline Status:** `FAILED`
* **Test Report:** 1 Failure recorded in `target/surefire-reports/TEST-com.podcastrelease.selenium.PodcastReleaseSystemSeleniumTest.xml`.
* **Artifacts Archived:** `target/selenium-screenshots/*.png` and `*_source.html` attached to build.
* **Deployment Prevention:** The pipeline terminated immediately at `Compile & Test`. Stages `Package WAR` and `Deploy to Tomcat` were **SKIPPED**, preventing defective code from reaching the Tomcat server.

---

## 4. Defect Correction & Successful Rerun

### Correction
The defect was corrected in `src/main/resources/templates/login.html` by restoring the expected title:
```html
<title>Login - Podcast Episode Release System</title>
```

### Rerun Verification
Upon rerunning the pipeline with the corrected code:
```text
[INFO] Running com.podcastrelease.selenium.PodcastReleaseSystemSeleniumTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 50.16 s -- in com.podcastrelease.selenium.PodcastReleaseSystemSeleniumTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

* **Test Report:** 80/80 tests passed (0 failures, 0 errors).
* **Packaging:** `podcast-release.war` generated in `target/`.
* **Deployment:** `podcast-release.war` successfully copied to Tomcat `webapps/`.
* **Pipeline Status:** `SUCCESS` (Blue build).

---

## 5. Course Deliverables Summary

| Requirement | Deliverable | Location / Evidence |
| :--- | :--- | :--- |
| **Jenkins Test Report** | Native JUnit test report integration | Configured via `junit testResults: 'target/surefire-reports/*.xml'` in [`Jenkinsfile`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/Jenkinsfile) |
| **Failed Pipeline Evidence** | Screenshot and logs of failed test stopping pipeline | `target/selenium-screenshots/`, Jenkins console log showing aborted deployment |
| **Defect Correction Commit** | Git commit fixing regression | Restored `login.html` title, verified with `BUILD SUCCESS` |
| **Successful Rerun** | Full pipeline execution | All 80 tests passed, WAR packaged and deployed to Tomcat |
