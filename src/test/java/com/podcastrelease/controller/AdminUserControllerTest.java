package com.podcastrelease.controller;

import com.podcastrelease.model.PlatformRole;
import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PodcastShowRepository podcastShowRepository;

    @Autowired
    private PodcastShowMemberRepository podcastShowMemberRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private TeamInviteRepository teamInviteRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private TeamRepository teamRepository;

    private User sampleUser;
    private PodcastShow sampleShow;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowMemberRepository.deleteAll();
        podcastShowRepository.deleteAll();
        teamInviteRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        sampleUser = userRepository.save(new User("sampleprod", "prod@example.com", "pass123"));
        sampleShow = podcastShowRepository.save(new PodcastShow("Test Show", "test-show", "Desc", "Tech", "Author", "email@show.com", null));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void listUsers_forbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void listUsers_accessibleForAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users", "shows"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUserRole_changesRoleSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/users/" + sampleUser.getId() + "/role")
                        .with(csrf())
                        .param("platformRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User updated = userRepository.findById(sampleUser.getId()).orElseThrow();
        assertEquals(PlatformRole.ADMIN, updated.getPlatformRole());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void toggleUserStatus_disablesUser() throws Exception {
        assertTrue(sampleUser.isEnabled());

        mockMvc.perform(post("/admin/users/" + sampleUser.getId() + "/toggle-status")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User updated = userRepository.findById(sampleUser.getId()).orElseThrow();
        assertFalse(updated.isEnabled());
    }
}
