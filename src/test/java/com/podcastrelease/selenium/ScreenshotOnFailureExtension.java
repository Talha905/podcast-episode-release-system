package com.podcastrelease.selenium;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * JUnit 5 extension that automatically captures browser screenshots and page HTML
 * whenever a Selenium test assertion or step fails.
 * Implements AfterTestExecutionCallback to capture artifacts BEFORE @AfterEach quits the driver.
 */
public class ScreenshotOnFailureExtension implements AfterTestExecutionCallback, TestWatcher {

    private static final String SCREENSHOT_DIR = "target/selenium-screenshots";

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            Object testInstance = context.getRequiredTestInstance();
            if (testInstance instanceof BaseSeleniumTest baseTest) {
                WebDriver driver = baseTest.getDriver();
                if (driver != null) {
                    captureFailureArtifacts(context, driver, context.getExecutionException().get());
                }
            }
        }
    }

    private void captureFailureArtifacts(ExtensionContext context, WebDriver driver, Throwable cause) {
        try {
            Path outputDir = Paths.get(System.getProperty("user.dir", "."), SCREENSHOT_DIR);
            Files.createDirectories(outputDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String testClass = context.getRequiredTestClass().getSimpleName();
            String testMethod = context.getRequiredTestMethod().getName();
            String filePrefix = String.format("%s_%s_%s", testClass, testMethod, timestamp);

            // 1. Capture PNG Screenshot
            if (driver instanceof TakesScreenshot takesScreenshot) {
                byte[] pngBytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
                Path screenshotPath = outputDir.resolve(filePrefix + ".png");
                Files.write(screenshotPath, pngBytes);
                System.out.println("\n[SELENIUM FAILURE ARTIFACT] Screenshot saved: " + screenshotPath.toAbsolutePath());
            }

            // 2. Capture Page Source HTML
            String pageSource = driver.getPageSource();
            if (pageSource != null) {
                Path htmlPath = outputDir.resolve(filePrefix + "_source.html");
                Files.writeString(htmlPath, pageSource);
                System.out.println("[SELENIUM FAILURE ARTIFACT] Page DOM source saved: " + htmlPath.toAbsolutePath());
            }

            if (cause != null) {
                System.out.println("[SELENIUM FAILURE ARTIFACT] Failure cause: " + cause.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[SELENIUM FAILURE] Failed to capture failure screenshot: " + e.getMessage());
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        // Fallback if not already captured
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
    }
}
