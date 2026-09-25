package com.podcastrelease.service;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.User;
import com.podcastrelease.model.WebhookConfig;
import com.podcastrelease.repository.WebhookConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final WebhookConfigRepository webhookConfigRepository;
    private final RestTemplate restTemplate;

    public NotificationService(WebhookConfigRepository webhookConfigRepository) {
        this.webhookConfigRepository = webhookConfigRepository;
        this.restTemplate = new RestTemplate();
    }

    public void sendWebhookNotification(Episode episode, EpisodeStatus oldStatus, EpisodeStatus newStatus, User actor) {
        List<WebhookConfig> configs = webhookConfigRepository.findAll();
        if (configs.isEmpty()) {
            log.info("No webhook configurations found. Skipping notification for episode '{}'.", episode.getTitle());
            return;
        }

        for (WebhookConfig config : configs) {
            boolean shouldNotify = false;
            if (newStatus == EpisodeStatus.PUBLISHED && config.isNotifyOnPublished()) {
                shouldNotify = true;
            } else if (newStatus == EpisodeStatus.FAILED && config.isNotifyOnFailed()) {
                shouldNotify = true;
            } else if (newStatus == EpisodeStatus.NEEDS_REVISION) {
                shouldNotify = true;
            }

            if (!shouldNotify) {
                continue;
            }

            try {
                String message = String.format("📢 Podcast Release System Update:\n" +
                                "• Episode: '%s' (ID: %d)\n" +
                                "• Status: %s -> %s\n" +
                                "• Performed By: %s\n" +
                                "• Show: %s",
                        episode.getTitle(),
                        episode.getId(),
                        oldStatus != null ? oldStatus : "N/A",
                        newStatus,
                        actor != null ? actor.getUsername() : "System",
                        episode.getPodcastShow() != null ? episode.getPodcastShow().getTitle() : "Unassigned");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, String> payload = new HashMap<>();
                if ("SLACK".equalsIgnoreCase(config.getPlatform())) {
                    payload.put("text", message);
                } else {
                    payload.put("content", message);
                }

                HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
                if (config.getWebhookUrl() != null && config.getWebhookUrl().startsWith("http")) {
                    restTemplate.postForEntity(config.getWebhookUrl(), request, String.class);
                    log.info("Webhook notification successfully sent to {} ({})", config.getName(), config.getPlatform());
                } else {
                    log.info("[SIMULATED WEBHOOK DISPATCH] To: {} ({}) | Content: {}", config.getName(), config.getPlatform(), message);
                }
            } catch (Exception e) {
                log.warn("Failed to dispatch webhook notification to {}: {}", config.getWebhookUrl(), e.getMessage());
            }
        }
    }
}
