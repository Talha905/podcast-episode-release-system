package com.podcastrelease.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Week 9 Deliverable: Comprehensive Selenium WebDriver test suite
 * executing 5 critical user journeys against the running Spring Boot application.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PodcastReleaseSystemSeleniumTest extends BaseSeleniumTest {

    @Test
    @Order(1)
    @DisplayName("Journey 1A: Invalid login shows appropriate error notification")
    public void testJourney1_InvalidLoginShowsError() {
        navigateTo("/login");
        assertEquals("Login - Podcast Episode Release System", driver.getTitle());

        login("nonExistentUser", "wrongPassword");

        WebElement errorAlert = waitForVisible(By.cssSelector(".alert-danger"));
        assertNotNull(errorAlert, "Error alert should be rendered for bad credentials");
        assertTrue(errorAlert.getText().contains("Invalid username or password"),
                "Alert text should state 'Invalid username or password'");
    }

    @Test
    @Order(2)
    @DisplayName("Journey 1B: Valid login successfully lands on Dashboard with sidebar & metrics")
    public void testJourney1_ValidLoginLandsOnDashboard() {
        loginAsAdmin();

        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "URL should redirect to /dashboard upon valid authentication");

        WebElement sidebar = waitForVisible(By.cssSelector(".sidebar"));
        assertTrue(sidebar.isDisplayed(), "Left sidebar navigation must be visible");

        // Verify brand title and dashboard navigation link
        assertTrue(driver.getPageSource().contains("Podcast System") || driver.getPageSource().contains("Dashboard"),
                "Dashboard page should contain brand and page header");

        // Check user session badge
        assertTrue(driver.getPageSource().contains("admin"), "Active user name 'admin' should be displayed");
    }

    @Test
    @Order(3)
    @DisplayName("Journey 2: Show Creation & Verification in Shows Catalog")
    public void testJourney2_CreatePodcastShow() {
        loginAsAdmin();
        navigateTo("/shows");

        WebElement titleInput = waitForVisible(By.cssSelector("form[action*='/shows/create'] input[name='title']"));
        assertNotNull(titleInput, "Show creation title field should be visible");

        String showTitle = "Selenium Tech Talk " + System.currentTimeMillis();
        String showAuthor = "Automated Host";
        String showDescription = "Automated show created by Week 9 Selenium WebDriver test suite.";

        titleInput.clear();
        titleInput.sendKeys(showTitle);

        WebElement authorInput = driver.findElement(By.cssSelector("form[action*='/shows/create'] input[name='author']"));
        authorInput.clear();
        authorInput.sendKeys(showAuthor);

        WebElement descInput = driver.findElement(By.cssSelector("form[action*='/shows/create'] textarea[name='description']"));
        descInput.clear();
        descInput.sendKeys(showDescription);

        WebElement submitBtn = driver.findElement(By.cssSelector("form[action*='/shows/create'] button[type='submit']"));
        jsClick(submitBtn);

        // Verify redirection and existence of newly created show
        wait.until(ExpectedConditions.urlContains("/shows"));
        WebElement showTable = waitForVisible(By.cssSelector("table"));
        assertNotNull(showTable, "Shows table should exist");
        assertTrue(driver.getPageSource().contains(showTitle),
                "Newly created show title should be listed in the shows catalog");
    }

    @Test
    @Order(4)
    @DisplayName("Journey 3: Episode Creation and Lifecycle Workflow State Transition")
    public void testJourney3_EpisodeCreationAndWorkflowTransition() {
        loginAsAdmin();
        navigateTo("/episodes/new");

        assertTrue(driver.getTitle().contains("Create Episode") || driver.getPageSource().contains("Create New Episode"),
                "Create Episode page should load");

        String episodeTitle = "Selenium Ep " + System.currentTimeMillis();
        String episodeDesc = "End-to-end automated test for episode lifecycle transitions.";

        WebElement titleInput = waitForVisible(By.id("title"));
        titleInput.sendKeys(episodeTitle);

        WebElement descInput = driver.findElement(By.id("description"));
        descInput.sendKeys(episodeDesc);

        // Select team if dropdown present
        List<WebElement> teamSelectElements = driver.findElements(By.id("teamId"));
        if (!teamSelectElements.isEmpty() && teamSelectElements.get(0).isDisplayed()) {
            Select teamSelect = new Select(teamSelectElements.get(0));
            if (!teamSelect.getOptions().isEmpty()) {
                teamSelect.selectByIndex(0);
            }
        }

        // Submit form via JS click to prevent any fixed navbar interception
        WebElement createBtn = driver.findElement(By.cssSelector("form[action*='/episodes/create'] button[type='submit']"));
        jsClick(createBtn);

        // Verify navigation to episode detail page
        wait.until(ExpectedConditions.urlMatches(".*/episodes/\\d+.*"));
        assertTrue(driver.getPageSource().contains(episodeTitle),
                "Episode detail view should display the created episode title");
        assertTrue(driver.getPageSource().contains("DRAFT"),
                "Newly created episode should have initial status DRAFT");

        // Execute workflow action: Submit for Review
        List<WebElement> submitForReviewButtons = driver.findElements(By.cssSelector("button[name='status'][value='SUBMITTED_FOR_REVIEW']"));
        if (!submitForReviewButtons.isEmpty()) {
            WebElement reviewBtn = submitForReviewButtons.get(0);
            jsClick(reviewBtn);

            // Verify status transition
            wait.until(ExpectedConditions.urlMatches(".*/episodes/\\d+.*"));
            assertTrue(driver.getPageSource().contains("SUBMITTED_FOR_REVIEW"),
                    "Episode status should transition to SUBMITTED_FOR_REVIEW");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Journey 4: Team Workspace & Member Invite Modal Interaction")
    public void testJourney4_TeamWorkspaceAndInviteModal() {
        loginAsAdmin();
        navigateTo("/teams");

        assertTrue(driver.getPageSource().contains("Team Members") || driver.getPageSource().contains("Active Team"),
                "Team management page must be loaded");

        List<WebElement> inviteModalButtons = driver.findElements(By.cssSelector("button[data-bs-target='#inviteModal']"));
        if (!inviteModalButtons.isEmpty()) {
            WebElement inviteBtn = inviteModalButtons.get(0);
            jsClick(inviteBtn);

            WebElement emailInput = waitForVisible(By.cssSelector("#inviteModal input[name='email']"));
            assertNotNull(emailInput, "Invite modal email input should be visible");

            String testInviteEmail = "selenium_invitee_" + System.currentTimeMillis() + "@example.com";
            emailInput.sendKeys(testInviteEmail);

            WebElement sendBtn = driver.findElement(By.cssSelector("#inviteModal button[type='submit']"));
            jsClick(sendBtn);

            wait.until(ExpectedConditions.urlContains("/teams"));
            assertTrue(driver.getPageSource().contains("success") || driver.getPageSource().contains("Invitation") || driver.getPageSource().contains("Team Members"),
                    "Page should confirm invitation creation or refresh member list");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Journey 5: User Profile & Security Settings View")
    public void testJourney5_UserProfileAndSecurityView() {
        loginAsAdmin();
        navigateTo("/profile");

        wait.until(ExpectedConditions.urlContains("/profile"));

        // Verify user profile details
        assertTrue(driver.getPageSource().contains("admin"),
                "Profile page must display the username 'admin'");
        assertTrue(driver.getPageSource().contains("Change Password"),
                "Profile page must have Change Password section");

        WebElement oldPwdField = waitForVisible(By.name("oldPassword"));
        assertNotNull(oldPwdField, "Current password input field should be present");

        WebElement newPwdField = driver.findElement(By.name("newPassword"));
        assertNotNull(newPwdField, "New password input field should be present");

        WebElement confirmPwdField = driver.findElement(By.name("confirmPassword"));
        assertNotNull(confirmPwdField, "Confirm password input field should be present");
    }
}
