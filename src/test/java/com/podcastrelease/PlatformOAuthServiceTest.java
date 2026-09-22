package com.podcastrelease;

import com.podcastrelease.model.PlatformAccount;
import com.podcastrelease.repository.PlatformAccountRepository;
import com.podcastrelease.service.PlatformOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PlatformOAuthServiceTest {

    @Autowired
    private PlatformOAuthService platformOAuthService;

    @Autowired
    private PlatformAccountRepository platformAccountRepository;

    @BeforeEach
    void setUp() {
        platformAccountRepository.deleteAll();
    }

    @Test
    void buildAuthorizationUrl_generatesValidOAuthRedirectUrl() {
        String baseUrl = "http://localhost:8081/podcast-release";
        String url = platformOAuthService.buildAuthorizationUrl(PlatformAccount.PlatformType.YOUTUBE, baseUrl);

        assertNotNull(url);
        assertTrue(url.contains("accounts.google.com"));
        assertTrue(url.contains("redirect_uri=http://localhost:8081/podcast-release/platforms/oauth2/callback/youtube"));
    }

    @Test
    void handleOAuthCallback_savesConnectedAccountWithOAuthTokens() {
        PlatformAccount account = platformOAuthService.handleOAuthCallback(PlatformAccount.PlatformType.YOUTUBE, "auth_code_12345");

        assertNotNull(account);
        assertNotNull(account.getId());
        assertNotNull(account.getAccessToken());
        assertNotNull(account.getRefreshToken());
        assertTrue(account.getAccessToken().startsWith("ya29."));
        assertTrue(account.getConnectedAccountName().contains("youtube"));
        assertTrue(account.isEnabled());
    }
}
