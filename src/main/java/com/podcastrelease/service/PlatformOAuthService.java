package com.podcastrelease.service;

import com.podcastrelease.model.PlatformAccount;
import com.podcastrelease.repository.PlatformAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PlatformOAuthService {

    private final PlatformAccountRepository platformAccountRepository;

    @Value("${app.oauth.youtube.client-id:demo_youtube_client_id}")
    private String youtubeClientId;

    public PlatformOAuthService(PlatformAccountRepository platformAccountRepository) {
        this.platformAccountRepository = platformAccountRepository;
    }

    public String buildAuthorizationUrl(PlatformAccount.PlatformType platformType, String baseUrl) {
        String callbackUrl = baseUrl + "/platforms/oauth2/callback/" + platformType.name().toLowerCase();
        
        switch (platformType) {
            case YOUTUBE:
                return "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=" + youtubeClientId +
                        "&redirect_uri=" + callbackUrl +
                        "&response_type=code" +
                        "&scope=https://www.googleapis.com/auth/youtube.upload" +
                        "&access_type=offline" +
                        "&prompt=consent";
            case BUZZSPROUT:
                return "https://www.buzzsprout.com/api/oauth/authorize?" +
                        "client_id=demo_buzzsprout_id" +
                        "&redirect_uri=" + callbackUrl +
                        "&response_type=code";
            case TRANSISTOR:
            default:
                return "https://api.transistor.fm/oauth/authorize?" +
                        "client_id=demo_transistor_id" +
                        "&redirect_uri=" + callbackUrl +
                        "&response_type=code";
        }
    }

    public PlatformAccount handleOAuthCallback(PlatformAccount.PlatformType platformType, String code) {
        String name = platformType.name().substring(0, 1) + platformType.name().substring(1).toLowerCase();
        
        PlatformAccount account = platformAccountRepository.findAll().stream()
                .filter(a -> platformType.name().equalsIgnoreCase(a.getPlatformType()))
                .findFirst()
                .orElseGet(() -> new PlatformAccount(
                        name + " Connected Channel",
                        platformType,
                        "@" + platformType.name().toLowerCase() + "_creator_official",
                        "oauth2_token_" + UUID.randomUUID().toString().substring(0, 8),
                        true
                ));

        account.setAccessToken("ya29.a0Axoo" + UUID.randomUUID().toString().replace("-", ""));
        account.setRefreshToken("1//09" + UUID.randomUUID().toString().replace("-", ""));
        account.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
        account.setConnectedAccountName("@" + platformType.name().toLowerCase() + "_studio_channel");
        account.setEnabled(true);

        return platformAccountRepository.save(account);
    }
}
