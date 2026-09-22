package com.podcastrelease.controller;

import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.model.User;
import com.podcastrelease.model.UserRole;
import com.podcastrelease.repository.AuditLogRepository;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.PodcastShowMemberRepository;
import com.podcastrelease.repository.PodcastShowRepository;
import com.podcastrelease.repository.UserRepository;
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

    private User sampleUser;
    private PodcastShow sampleShow;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowMemberRepository.deleteAll();
        userRepository.deleteAll();
        podcastShowRepository.deleteAll();

        sampleUser = userRepository.save(new User("sampleprod", "prod@example.com", "pass123", UserRole.PRODUCER));
        sampleShow = podcastShowRepository.save(new PodcastShow("Test Show", "test-show", "Desc", "Tech", "Author", "email@show.com", null));
    }

    @Test
    @WithMockUser(username = "producer", roles = {"PRODUCER"})
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
                        .param("role", "HOST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User updated = userRepository.findById(sampleUser.getId()).orElseThrow();
        assertEquals(UserRole.HOST, updated.getRole());
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

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void assignUserToShow_createsShowMemberRecord() throws Exception {
        mockMvc.perform(post("/admin/users/" + sampleUser.getId() + "/assign-show")
                        .with(csrf())
                        .param("showId", sampleShow.getId().toString())
                        .param("roleInShow", "PRODUCER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertTrue(podcastShowMemberRepository.existsByPodcastShowIdAndUserId(sampleShow.getId(), sampleUser.getId()));
    }
}
