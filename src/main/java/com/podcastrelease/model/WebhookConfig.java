package com.podcastrelease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_configs")
public class WebhookConfig {

    public enum WebhookType {
        SLACK, DISCORD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1024)
    private String webhookUrl;

    @Column(nullable = false)
    private String platform; // SLACK or DISCORD

    private boolean notifyOnPublished;
    private boolean notifyOnFailed;

    private LocalDateTime createdAt;

    public WebhookConfig() {
        this.createdAt = LocalDateTime.now();
        this.notifyOnPublished = true;
        this.notifyOnFailed = true;
        this.platform = "DISCORD";
    }

    public WebhookConfig(String name, String webhookUrl, String platform) {
        this();
        this.name = name;
        this.webhookUrl = webhookUrl;
        this.platform = platform;
    }

    public WebhookConfig(String name, WebhookType webhookType, String webhookUrl, boolean enabled) {
        this(name, webhookUrl, webhookType.name());
        this.notifyOnPublished = enabled;
        this.notifyOnFailed = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isNotifyOnPublished() {
        return notifyOnPublished;
    }

    public void setNotifyOnPublished(boolean notifyOnPublished) {
        this.notifyOnPublished = notifyOnPublished;
    }

    public boolean isNotifyOnFailed() {
        return notifyOnFailed;
    }

    public void setNotifyOnFailed(boolean notifyOnFailed) {
        this.notifyOnFailed = notifyOnFailed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
