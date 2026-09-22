package com.podcastrelease.service;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.PlatformAccount;
import com.podcastrelease.model.WebhookConfig;
import com.podcastrelease.repository.PlatformAccountRepository;
import com.podcastrelease.repository.WebhookConfigRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class DistributionService {

    private final PlatformAccountRepository platformAccountRepository;
    private final WebhookConfigRepository webhookConfigRepository;
    private final HttpClient httpClient;

    public DistributionService(PlatformAccountRepository platformAccountRepository,
                               WebhookConfigRepository webhookConfigRepository) {
        this.platformAccountRepository = platformAccountRepository;
        this.webhookConfigRepository = webhookConfigRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void dispatchPublication(Episode episode) {
        if (episode == null) return;

        // 1. Publish to connected platforms (YouTube, Buzzsprout, etc.)
        List<PlatformAccount> activeAccounts = platformAccountRepository.findByEnabledTrue();
        for (PlatformAccount account : activeAccounts) {
            publishToPlatform(episode, account);
        }

        // 2. Dispatch Slack / Discord Webhooks
        List<WebhookConfig> webhooks = webhookConfigRepository.findAll();
        for (WebhookConfig webhook : webhooks) {
            if (episode.getStatus() == EpisodeStatus.PUBLISHED && webhook.isNotifyOnPublished()) {
                sendWebhookNotification(webhook, episode, "🎉 EPISODE PUBLISHED");
            } else if (episode.getStatus() == EpisodeStatus.FAILED && webhook.isNotifyOnFailed()) {
                sendWebhookNotification(webhook, episode, "🚨 EPISODE RELEASE FAILED");
            }
        }
    }

    public String publishToPlatform(Episode episode, PlatformAccount account) {
        // Simulates platform publishing call (REST API / OAuth)
        String logMessage = String.format("Published '%s' to %s (%s)",
                episode.getTitle(), account.getPlatformName(), account.getPlatformType());
        return logMessage;
    }

    private void sendWebhookNotification(WebhookConfig config, Episode episode, String header) {
        if (config.getWebhookUrl() == null || config.getWebhookUrl().isBlank()) return;

        try {
            String jsonPayload;
            if ("DISCORD".equalsIgnoreCase(config.getPlatform())) {
                jsonPayload = String.format("""
                        {
                          "content": "%s: **%s**",
                          "embeds": [{
                            "title": "%s",
                            "description": "%s",
                            "color": %d
                          }]
                        }
                        """,
                        header,
                        escapeJson(episode.getTitle()),
                        escapeJson(episode.getTitle()),
                        escapeJson(episode.getDescription()),
                        episode.getStatus() == EpisodeStatus.PUBLISHED ? 3066993 : 15158332);
            } else { // SLACK fallback
                jsonPayload = String.format("""
                        {
                          "text": "%s: *%s*\\n> %s"
                        }
                        """,
                        header,
                        escapeJson(episode.getTitle()),
                        escapeJson(episode.getDescription()));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", " ");
    }
}
