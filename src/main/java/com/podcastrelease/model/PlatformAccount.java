package com.podcastrelease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_accounts")
public class PlatformAccount {

    public enum PlatformType {
        YOUTUBE, BUZZSPROUT, TRANSISTOR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String platformName; // e.g. YouTube, Buzzsprout, Transistor

    @Column(nullable = false)
    private String platformType; // YOUTUBE, BUZZSPROUT, TRANSISTOR

    private String accountIdentifier;
    private String apiKey;
    private String oauthToken;
    private boolean enabled;

    private LocalDateTime createdAt;

    public PlatformAccount() {
        this.createdAt = LocalDateTime.now();
        this.enabled = true;
    }

    public PlatformAccount(String platformName, String platformType, String accountIdentifier, String apiKey) {
        this();
        this.platformName = platformName;
        this.platformType = platformType;
        this.accountIdentifier = accountIdentifier;
        this.apiKey = apiKey;
    }

    public PlatformAccount(String platformName, PlatformType platformType, String accountIdentifier, String apiKey, boolean enabled) {
        this(platformName, platformType.name(), accountIdentifier, apiKey);
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(String platformType) {
        this.platformType = platformType;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }

    public void setAccountIdentifier(String accountIdentifier) {
        this.accountIdentifier = accountIdentifier;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
        this.oauthToken = oauthToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
