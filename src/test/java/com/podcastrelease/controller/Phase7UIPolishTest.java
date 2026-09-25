package com.podcastrelease.controller;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import com.podcastrelease.service.EpisodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class Phase7UIPolishTest {

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
    private PlatformAccountRepository platformAccountRepository;

    @Autowired
    private EpisodeService episodeService;

    private User testUser;
    private User adminUser;
    private Episode episode1;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        platformAccountRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("uiUser", "ui@example.com", "password");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        adminUser = new User("adminUiUser", "adminui@example.com", "password", PlatformRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        Team team = teamRepository.save(new Team("UI Test Team", "Description"));
        teamMembershipRepository.save(new TeamMembership(team, testUser, TeamRole.OWNER, false));

        PodcastShow show = podcastShowRepository.save(new PodcastShow("UI Show", "Description", "http://example.com/cover.jpg", "Tech"));

        episode1 = new Episode();
        episode1.setTitle("UI Polish Ep");
        episode1.setDescription("Testing UI layout and links");
        episode1.setAudioFileUrl("/audio/ui_ep.mp3");
        episode1.setPublishDate(LocalDate.now().plusDays(2));
        episode1.setStatus(EpisodeStatus.DRAFT);
        episode1.setPodcastShow(show);
        episode1.setTeam(team);
        episode1 = episodeService.create(episode1, testUser.getUsername());
    }

    @Test
    @WithMockUser(username = "uiUser")
    void testDashboardRendering() throws Exception {
        mockMvc.perform(get("/episodes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Episode Release Catalog")))
                .andExpect(content().string(containsString("UI Polish Ep")));
    }

    @Test
    @WithMockUser(username = "uiUser")
    void testEpisodeDetailRendering() throws Exception {
        mockMvc.perform(get("/episodes/" + episode1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UI Polish Ep")))
                .andExpect(content().string(containsString("Multi-Stage Workflow Actions")))
                .andExpect(content().string(containsString("Audit Log Trail")));
    }

    @Test
    @WithMockUser(username = "uiUser")
    void testPlatformsRendering() throws Exception {
        mockMvc.perform(get("/platforms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Direct Platform Integrations (OAuth 2.0 1-Click Connect)")))
                .andExpect(content().string(containsString("/platforms/oauth2/connect/youtube")));
    }

    @Test
    @WithMockUser(username = "adminUiUser", roles = {"ADMIN"})
    void testAdminUserManagementRendering() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User Management")))
                .andExpect(content().string(containsString("uiUser")));
    }
}
