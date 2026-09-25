package com.podcastrelease.controller;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import com.podcastrelease.service.EpisodeService;
import com.podcastrelease.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class Phase6AuditNotificationReportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private PodcastShowRepository podcastShowRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private WebhookConfigRepository webhookConfigRepository;

    @Autowired
    private EpisodeService episodeService;

    @Autowired
    private NotificationService notificationService;

    private User ownerUser;
    private User nonMemberUser;
    private Team team1;
    private Team team2;
    private Episode episode1;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        webhookConfigRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = new User("p6owner", "owner@example.com", "password");
        ownerUser.setEnabled(true);
        ownerUser = userRepository.save(ownerUser);

        nonMemberUser = new User("p6stranger", "stranger@example.com", "password");
        nonMemberUser.setEnabled(true);
        nonMemberUser = userRepository.save(nonMemberUser);

        team1 = teamRepository.save(new Team("Alpha Team", "Alpha Team Description"));
        team2 = teamRepository.save(new Team("Beta Team", "Beta Team Description"));

        teamMembershipRepository.save(new TeamMembership(team1, ownerUser, TeamRole.OWNER, false));

        PodcastShow show = new PodcastShow("Alpha Show", "Alpha Show Description", "http://example.com/cover.jpg", "Tech");
        show.setTeam(team1);
        show = podcastShowRepository.save(show);

        episode1 = new Episode();
        episode1.setTitle("Phase 6 Tech Talk");
        episode1.setDescription("Deep dive into Audit Logs, Webhooks, and Reports");
        episode1.setAudioFileUrl("/audio/p6_talk.mp3");
        episode1.setPublishDate(LocalDate.now().plusDays(1));
        episode1.setStatus(EpisodeStatus.DRAFT);
        episode1.setPodcastShow(show);
        episode1.setTeam(team1);
        episode1 = episodeService.create(episode1, ownerUser.getUsername());
    }

    @Test
    @WithMockUser(username = "p6owner")
    void testCsvReportGenerationForTeamMember() throws Exception {
        mockMvc.perform(get("/api/teams/" + team1.getId() + "/reports/episodes.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("team_" + team1.getId() + "_episodes.csv")))
                .andExpect(content().string(containsString("Episode ID,Title,Status,Publish Date,Show Title")))
                .andExpect(content().string(containsString("Phase 6 Tech Talk")))
                .andExpect(content().string(containsString("Alpha Show")));
    }

    @Test
    @WithMockUser(username = "p6stranger")
    void testCsvReportGenerationReturns404ForNonMember() throws Exception {
        mockMvc.perform(get("/api/teams/" + team1.getId() + "/reports/episodes.csv"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "p6owner")
    void testTeamAuditLogsFetchForTeamMember() throws Exception {
        episodeService.updateStatus(episode1.getId(), EpisodeStatus.PENDING_EDIT, ownerUser.getUsername());

        mockMvc.perform(get("/api/teams/" + team1.getId() + "/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].teamId", is(team1.getId().intValue())))
                .andExpect(jsonPath("$[0].action", containsString("STATUS_CHANGE")));
    }

    @Test
    @WithMockUser(username = "p6stranger")
    void testTeamAuditLogsReturns404ForNonMember() throws Exception {
        mockMvc.perform(get("/api/teams/" + team1.getId() + "/audit-logs"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testWebhookNotificationDispatchOnStatusChange() {
        WebhookConfig slackConfig = new WebhookConfig("Team Slack", WebhookConfig.WebhookType.SLACK, "http://localhost:9999/dummy-slack", true);
        WebhookConfig discordConfig = new WebhookConfig("Team Discord", WebhookConfig.WebhookType.DISCORD, "http://localhost:9999/dummy-discord", true);
        webhookConfigRepository.save(slackConfig);
        webhookConfigRepository.save(discordConfig);

        assertDoesNotThrow(() -> {
            notificationService.sendWebhookNotification(episode1, EpisodeStatus.DRAFT, EpisodeStatus.NEEDS_REVISION, ownerUser);
            notificationService.sendWebhookNotification(episode1, EpisodeStatus.SUBMITTED_FOR_REVIEW, EpisodeStatus.PUBLISHED, ownerUser);
            notificationService.sendWebhookNotification(episode1, EpisodeStatus.PUBLISHING, EpisodeStatus.FAILED, ownerUser);
        });
    }
}
