package com.podcastrelease;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.PlatformAccountRepository;
import com.podcastrelease.repository.WebhookConfigRepository;
import com.podcastrelease.service.DistributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DistributionServiceTest {

    @Autowired
    private DistributionService distributionService;

    @Autowired
    private PlatformAccountRepository platformAccountRepository;

    @Autowired
    private WebhookConfigRepository webhookConfigRepository;

    @BeforeEach
    void setUp() {
        platformAccountRepository.deleteAll();
        webhookConfigRepository.deleteAll();
    }

    @Test
    void dispatchPublication_triggersPlatformPublishAndWebhooksWithoutException() {
        PlatformAccount yt = platformAccountRepository.save(new PlatformAccount(
                "YouTube Account", PlatformAccount.PlatformType.YOUTUBE, "https://youtube.com", "token123", true));
        PlatformAccount bz = platformAccountRepository.save(new PlatformAccount(
                "Buzzsprout Account", PlatformAccount.PlatformType.BUZZSPROUT, "https://buzzsprout.com", "key123", true));

        webhookConfigRepository.save(new WebhookConfig(
                "Slack Channel", WebhookConfig.WebhookType.SLACK, "https://example.com/webhooks/test-slack", true));
        webhookConfigRepository.save(new WebhookConfig(
                "Discord Channel", WebhookConfig.WebhookType.DISCORD, "https://example.com/webhooks/test-discord", true));

        Episode episode = new Episode("Test Distribution Ep", "Description text", "http://audio.com/ep.mp3", LocalDate.now());
        episode.setStatus(EpisodeStatus.PUBLISHED);

        assertDoesNotThrow(() -> distributionService.dispatchPublication(episode));

        String logMsg = distributionService.publishToPlatform(episode, yt);
        assertTrue(logMsg.contains("YouTube Account"));
    }
}
