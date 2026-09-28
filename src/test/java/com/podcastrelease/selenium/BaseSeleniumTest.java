package com.podcastrelease.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

/**
 * Base class for Selenium WebDriver tests.
 * Spins up Spring Boot on a random ephemeral port and initializes a headless browser
 * with automatic fallback (Edge -> Chrome -> Firefox).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(ScreenshotOnFailureExtension.class)
public abstract class BaseSeleniumTest {

    @LocalServerPort
    protected int port;

    @org.springframework.beans.factory.annotation.Autowired
    protected com.podcastrelease.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    protected org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    protected com.podcastrelease.repository.TeamRepository teamRepository;

    @org.springframework.beans.factory.annotation.Autowired
    protected com.podcastrelease.repository.TeamMembershipRepository teamMembershipRepository;

    protected WebDriver driver;
    protected WebDriverWait wait;

    public WebDriver getDriver() {
        return driver;
    }

    @BeforeEach
    public void setUpBrowser() {
        ensureAdminAndTeamExist();
        driver = createHeadlessDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    protected void ensureAdminAndTeamExist() {
        com.podcastrelease.model.Team defaultTeam = teamRepository.findByName("Legacy Podcast Team").orElseGet(() ->
                teamRepository.save(new com.podcastrelease.model.Team("Legacy Podcast Team", "Default podcast production team"))
        );

        com.podcastrelease.model.User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new com.podcastrelease.model.User("admin", "admin@podcastrelease.com", passwordEncoder.encode("password123"), com.podcastrelease.model.PlatformRole.ADMIN);
            admin = userRepository.save(admin);
        } else {
            admin.setPasswordHash(passwordEncoder.encode("password123"));
            admin = userRepository.save(admin);
        }

        final com.podcastrelease.model.User finalAdmin = admin;
        boolean hasMembership = teamMembershipRepository.findByUserId(admin.getId()).stream()
                .anyMatch(m -> m.getTeam().getId().equals(defaultTeam.getId()));
        if (!hasMembership) {
            teamMembershipRepository.save(new com.podcastrelease.model.TeamMembership(defaultTeam, finalAdmin, com.podcastrelease.model.TeamRole.OWNER));
        }
    }

    @AfterEach
    public void tearDownBrowser() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
    }

    private WebDriver createHeadlessDriver() {
        // Priority 1: Chrome
        try {
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--remote-allow-origins=*", "--window-size=1920,1080");
            return new ChromeDriver(chromeOptions);
        } catch (Throwable t1) {
            System.out.println("[SELENIUM] ChromeDriver initialization failed (" + t1.getMessage() + "), falling back to EdgeDriver...");
        }

        // Priority 2: Edge
        try {
            EdgeOptions edgeOptions = new EdgeOptions();
            edgeOptions.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--remote-allow-origins=*", "--window-size=1920,1080");
            return new EdgeDriver(edgeOptions);
        } catch (Throwable t2) {
            System.out.println("[SELENIUM] EdgeDriver initialization failed (" + t2.getMessage() + "), falling back to FirefoxDriver...");
        }

        // Priority 3: Firefox
        try {
            FirefoxOptions firefoxOptions = new FirefoxOptions();
            firefoxOptions.addArguments("-headless");
            return new FirefoxDriver(firefoxOptions);
        } catch (Throwable t3) {
            throw new IllegalStateException("Failed to initialize any Selenium WebDriver (Chrome, Edge, or Firefox). Please ensure a supported browser is installed.", t3);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    public void navigateTo(String path) {
        driver.get(baseUrl() + path);
    }

    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void login(String username, String password) {
        navigateTo("/login");
        waitForVisible(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(username);

        waitForVisible(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys(password);

        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    public void loginAsAdmin() {
        login("admin", "password123");
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    public void jsClick(WebElement element) {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'}); arguments[0].click();", element);
    }

    public void scrollTo(WebElement element) {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }
}
