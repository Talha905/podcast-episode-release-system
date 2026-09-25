package com.podcastrelease.controller;

import com.podcastrelease.model.Team;
import com.podcastrelease.model.TeamRole;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private TeamInviteRepository teamInviteRepository;

    @Autowired
    private PodcastShowRepository podcastShowRepository;

    @Autowired
    private PodcastShowMemberRepository podcastShowMemberRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User ownerUser;
    private User memberUser;
    private User outsiderUser;
    private Team sampleTeam;

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

        ownerUser = userRepository.save(new User("teamowner", "owner@example.com", "pass123"));
        memberUser = userRepository.save(new User("teammember", "member@example.com", "pass123"));
        outsiderUser = userRepository.save(new User("outsider", "outsider@example.com", "pass123"));

        sampleTeam = teamRepository.save(new Team("Alpha Team", "Alpha team description"));

        teamMembershipRepository.save(new com.podcastrelease.model.TeamMembership(sampleTeam, ownerUser, TeamRole.OWNER));
        teamMembershipRepository.save(new com.podcastrelease.model.TeamMembership(sampleTeam, memberUser, TeamRole.CREATOR));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void getTeamById_accessibleForMember() throws Exception {
        mockMvc.perform(get("/api/teams/" + sampleTeam.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alpha Team"))
                .andExpect(jsonPath("$.myRole").value("OWNER"));
    }

    @Test
    @WithMockUser(username = "outsider")
    void getTeamById_returns404ForNonMember() throws Exception {
        // Must strictly return 404 NOT FOUND for unauthorized isolation
        mockMvc.perform(get("/api/teams/" + sampleTeam.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "outsider")
    void getTeamMembers_returns404ForNonMember() throws Exception {
        mockMvc.perform(get("/api/teams/" + sampleTeam.getId() + "/members"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "teamowner")
    void createTeam_success() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Beta Team\", \"description\": \"Beta desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Beta Team"))
                .andExpect(jsonPath("$.myRole").value("OWNER"));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void inviteMemberAndAccept_success() throws Exception {
        // 1. Create invite as owner
        String inviteResponse = mockMvc.perform(post("/api/teams/" + sampleTeam.getId() + "/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"outsider@example.com\", \"role\": \"EDITOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("outsider@example.com"))
                .andExpect(jsonPath("$.role").value("EDITOR"))
                .andReturn().getResponse().getContentAsString();

        String token = inviteResponse.split("\"token\":\"")[1].split("\"")[0];

        // 2. Outsider accepts invite
        mockMvc.perform(post("/api/invites/" + token + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));

        // 3. Outsider can now view team details
        mockMvc.perform(get("/api/teams/" + sampleTeam.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("EDITOR"));
    }
}
