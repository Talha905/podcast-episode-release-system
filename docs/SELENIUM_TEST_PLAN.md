# Week 9: Selenium Test Plan & Execution Guide
**Project:** Jenkins-Based Podcast Episode Release System  
**Milestone:** Week-9 — Selenium Test Design and Local Execution  
**Technology Stack:** Java 17+, Spring Boot 3.3.0, JUnit 5, Selenium WebDriver 4.19+, Maven Surefire  

---

## 1. Objective & Scope

The purpose of Week 9 is to design, implement, and locally execute an end-to-end automated UI test suite using Selenium WebDriver. The test suite validates the critical user journeys of the **Podcast Episode Release System**, ensures reliable state machine transitions, handles both positive and negative scenarios, and captures screenshots upon any unexpected failure.

### Key Deliverables Fulfilled:
1. **Test Plan**: Documenting 5 critical user journeys, test fixtures, and locators.
2. **Selenium Scripts**: Comprehensive WebDriver tests (`PodcastReleaseSystemSeleniumTest.java` and `BaseSeleniumTest.java`).
3. **Failure Screenshot Mechanism**: Automatic PNG screenshot and HTML DOM capture upon test failure (`ScreenshotOnFailureExtension.java`).
4. **Local Execution & Report**: Verified execution through Maven with Surefire reports generated in `target/surefire-reports/`.

---

## 2. Test Architecture

* **Embedded Server Deployment:** Tests execute against an embedded Spring Boot Tomcat container running on a dynamic ephemeral port (`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`).
* **Headless Browser Execution:** By default, the suite runs headless (`--headless=new`, `--disable-gpu`, `--no-sandbox`, `--disable-dev-shm-usage`) to ensure rapid execution without popping GUI windows and ensuring compatibility with headless CI environments (e.g. Jenkins).
* **Multi-Browser Driver Fallback:** The driver factory automatically attempts Chrome first, falling back to Microsoft Edge or Mozilla Firefox using Selenium 4's built-in Selenium Manager.
* **Resilient Explicit Synchronization:** Synchronizes on UI elements using `WebDriverWait` (expected conditions: `visibilityOfElementLocated`, `elementToBeClickable`, `urlMatches`) rather than brittle thread sleeps.

---

## 3. Critical User Journeys Matrix

| Journey ID | Journey Name | Scope & Steps | Test Data | Key Assertions |
| :--- | :--- | :--- | :--- | :--- |
| **UJ-01A** | Invalid Authentication | 1. Navigate to `/login`<br>2. Submit invalid username and password | User: `nonExistentUser`<br>Pass: `wrongPassword` | Page displays `.alert-danger` containing `"Invalid username or password"`. |
| **UJ-01B** | Valid Authentication & Dashboard Landing | 1. Navigate to `/login`<br>2. Enter credentials for admin<br>3. Submit login form | User: `admin`<br>Pass: `password123` | Redirects to `/dashboard`; sidebar is visible; brand and user name `"admin"` are rendered. |
| **UJ-02** | Podcast Show Creation & Catalog Verification | 1. Login as admin<br>2. Navigate to `/shows`<br>3. Fill Show Title, Author, Description, Category<br>4. Submit show creation form | Title: `Selenium Tech Talk {timestamp}`<br>Author: `Automated Host`<br>Category: `Technology` | URL redirects to `/shows`; show title is present in the managed shows table. |
| **UJ-03** | Episode Creation & State Machine Workflow | 1. Navigate to `/episodes/new`<br>2. Fill Episode Title, Description, and select Team<br>3. Submit episode form<br>4. Verify landing on `/episodes/{id}` in `DRAFT` status<br>5. Click "Submit for Review" | Title: `Selenium Ep {timestamp}`<br>Initial Status: `DRAFT`<br>Target Status: `SUBMITTED_FOR_REVIEW` | Status badge begins as `DRAFT`; after action, status badge transitions to `SUBMITTED_FOR_REVIEW`. |
| **UJ-04** | Team Workspace & Member Invitation | 1. Navigate to `/teams`<br>2. Open "Invite Member" modal<br>3. Enter email address and select role<br>4. Submit invitation | Email: `selenium_invitee_{timestamp}@example.com`<br>Role: `CREATOR` | Modal closes; page shows updated team details or confirmation notification. |
| **UJ-05** | User Profile & Security Settings View | 1. Navigate to `/profile`<br>2. Verify user info card<br>3. Verify Change Password form inputs | Active User: `admin`<br>Form fields: `oldPassword`, `newPassword`, `confirmPassword` | Page displays username, active team memberships, and all password update fields. |

---

## 4. Failure Screenshot Mechanism

The suite implements JUnit 5's `AfterTestExecutionCallback` in [`ScreenshotOnFailureExtension.java`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/src/test/java/com/podcastrelease/selenium/ScreenshotOnFailureExtension.java):
* **Trigger:** Triggers immediately when any test assertion fails or an unexpected exception is thrown, **before** the WebDriver session is torn down.
* **Artifacts Captured:**
  1. **High-Resolution PNG Screenshot:** Saved to `target/selenium-screenshots/{TestClass}_{testMethod}_{timestamp}.png`.
  2. **Full Page HTML DOM Dump:** Saved to `target/selenium-screenshots/{TestClass}_{testMethod}_{timestamp}_source.html`.
* **Console Notification:** The extension prints absolute file paths to standard error for immediate inspection during build logs.

---

## 5. Local Execution Instructions

### Running Only the Selenium Test Suite
Execute the following command in PowerShell / Command Prompt:
```powershell
mvn test -Dtest=PodcastReleaseSystemSeleniumTest
```
*(Or using full Maven wrapper / installed path: `C:\Users\thele\apache-maven-3.9.16\bin\mvn.cmd test -Dtest=PodcastReleaseSystemSeleniumTest`)*

### Running All Project Tests (Unit, Integration & Selenium)
```powershell
mvn test
```

### Inspecting Reports & Artifacts
* **Surefire Text Summary:** `target/surefire-reports/com.podcastrelease.selenium.PodcastReleaseSystemSeleniumTest.txt`
* **Surefire XML Report (Jenkins consumable):** `target/surefire-reports/TEST-com.podcastrelease.selenium.PodcastReleaseSystemSeleniumTest.xml`
* **Failure Screenshots & DOM Dumps:** `target/selenium-screenshots/`
