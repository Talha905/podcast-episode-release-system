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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
        ownerUser.setEnabled(true);
        userRepository.save(ownerUser);

        memberUser = userRepository.save(new User("teammember", "member@example.com", "pass123"));
        memberUser.setEnabled(true);
        userRepository.save(memberUser);

        outsiderUser = userRepository.save(new User("outsider", "outsider@example.com", "pass123"));
        outsiderUser.setEnabled(true);
        userRepository.save(outsiderUser);

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
        mockMvc.perform(post("/api/teams").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Beta Team\", \"description\": \"Beta desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Beta Team"))
                .andExpect(jsonPath("$.myRole").value("OWNER"));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void inviteMemberAndAccept_success() throws Exception {
        String inviteResponse = mockMvc.perform(post("/api/teams/" + sampleTeam.getId() + "/invites").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"outsider@example.com\", \"role\": \"EDITOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("outsider@example.com"))
                .andExpect(jsonPath("$.role").value("EDITOR"))
                .andReturn().getResponse().getContentAsString();

        String token = inviteResponse.split("\"token\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/invites/" + token + "/accept").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));

        mockMvc.perform(get("/api/teams/" + sampleTeam.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("EDITOR"));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void getTeamsView_returns200() throws Exception {
        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(view().name("teams"))
                .andExpect(model().attributeExists("myTeams", "activeTeam", "members"));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void inviteMemberWebAndAcceptWeb_success() throws Exception {
        mockMvc.perform(post("/teams/" + sampleTeam.getId() + "/invite").with(csrf())
                        .param("email", "outsider@example.com")
                        .param("role", "RELEASE_MANAGER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("createdInviteUrl", "successMessage"));

        var invite = teamInviteRepository.findByTeamId(sampleTeam.getId()).get(0);

        mockMvc.perform(get("/invites/" + invite.getToken() + "/accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Successfully joined team 'Alpha Team' as RELEASE_MANAGER!"));
    }
}
