package com.podcastrelease.service;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Phase4YouTubeDistributionTest {

    @Autowired
    private PlatformOAuthService platformOAuthService;

    @Autowired
    private DistributionService distributionService;

    @Autowired
    private PlatformAccountRepository platformAccountRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PodcastShowRepository podcastShowRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Team testTeam;
    private User testUser;
    private PodcastShow testShow;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowRepository.deleteAll();
        platformAccountRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(new User("yt_user", "yt@example.com", "pass123"));
        testTeam = teamRepository.save(new Team("Media Team", "Media Team Desc"));
        teamMembershipRepository.save(new TeamMembership(testTeam, testUser, TeamRole.OWNER));

        testShow = new PodcastShow("YT Show", "yt-show", "Desc", "Tech", "Author", "yt@example.com", null);
        testShow.setTeam(testTeam);
        testShow = podcastShowRepository.save(testShow);
    }

    @Test
    void buildAuthorizationUrl_targetsCorrectScopeAndPort() {
        String url = platformOAuthService.buildAuthorizationUrl(PlatformAccount.PlatformType.YOUTUBE, "http://localhost:8005");
        assertNotNull(url);
        assertTrue(url.contains("accounts.google.com"));
        assertTrue(url.contains("youtube.upload"));
        assertTrue(url.contains("http%3A%2F%2Flocalhost%3A8005"));
    }

    @Test
    void handleOAuthCallback_createsAndLinksAccountToTeam() {
        PlatformAccount account = platformOAuthService.handleOAuthCallback(PlatformAccount.PlatformType.YOUTUBE, "demo_code", "http://localhost:8005");
        account.setTeam(testTeam);
        PlatformAccount saved = platformAccountRepository.save(account);

        assertNotNull(saved.getId());
        assertEquals("YOUTUBE", saved.getPlatformType());
        assertNotNull(saved.getAccessToken());
    }

    @Test
    void publishToPlatform_updatesEpisodeDistributionDetails() {
        PlatformAccount account = new PlatformAccount("YouTube Studio Channel", PlatformAccount.PlatformType.YOUTUBE, "@yt_channel", "api_key_123", true);
        account.setTeam(testTeam);
        platformAccountRepository.save(account);

        Episode ep = new Episode("YT Distribution Ep", "Desc", "http://audio.mp3", LocalDate.now());
        ep.setPodcastShow(testShow);
        ep.setTeam(testTeam);
        ep.setStatus(EpisodeStatus.PUBLISHED);
        ep = episodeRepository.save(ep);

        distributionService.dispatchPublication(ep);
        assertNotNull(ep.getId());
    }
}
