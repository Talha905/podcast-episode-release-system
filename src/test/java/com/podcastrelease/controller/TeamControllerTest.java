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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User ownerUser;
    private User memberUser;
    private User outsiderUser;
    private Team sampleTeam;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowMemberRepository.deleteAll();
        podcastShowRepository.deleteAll();
        teamInviteRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = userRepository.save(new User("teamowner", "owner@example.com", passwordEncoder.encode("pass123")));
        ownerUser.setEnabled(true);
        userRepository.save(ownerUser);

        memberUser = userRepository.save(new User("teammember", "member@example.com", passwordEncoder.encode("pass123")));
        memberUser.setEnabled(true);
        userRepository.save(memberUser);

        outsiderUser = userRepository.save(new User("outsider", "outsider@example.com", passwordEncoder.encode("pass123")));
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

    @Test
    void unauthenticatedUser_clickInviteLink_savesTokenInSessionAndRedirectsToLogin() throws Exception {
        // Owner creates invite
        com.podcastrelease.model.TeamInvite invite = new com.podcastrelease.model.TeamInvite(sampleTeam, "newuser@example.com", TeamRole.EDITOR, "token123", ownerUser);
        teamInviteRepository.save(invite);

        MockHttpSession session = new MockHttpSession();

        // 1. Unauthenticated user clicks invite link
        mockMvc.perform(get("/invites/token123/accept").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Token is saved in session
        org.junit.jupiter.api.Assertions.assertEquals("token123", session.getAttribute("pendingInviteToken"));

        // 3. User logs in with session containing token
        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "outsider")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams?teamId=" + sampleTeam.getId()));

        // 4. Verify outsider is now a member with EDITOR role
        var membership = teamMembershipRepository.findByTeamIdAndUserId(sampleTeam.getId(), outsiderUser.getId());
        org.junit.jupiter.api.Assertions.assertTrue(membership.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(TeamRole.EDITOR, membership.get().getRole());
    }

    @Test
    void invalidOrAlreadyAcceptedInviteLink_failsSafelyWithErrorMessage() throws Exception {
        // 1. Invalid token
        mockMvc.perform(get("/invites/invalid-token-xyz/accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("errorMessage", "Invalid invitation token."));

        // 2. Already accepted token
        com.podcastrelease.model.TeamInvite invite = new com.podcastrelease.model.TeamInvite(sampleTeam, "used@example.com", TeamRole.CREATOR, "usedtoken123", ownerUser);
        invite.setAccepted(true);
        teamInviteRepository.save(invite);

        mockMvc.perform(get("/invites/usedtoken123/accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("errorMessage", "Invitation has already been accepted."));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void inviteMember_createsInAppNotificationIfUserExists() throws Exception {
        mockMvc.perform(post("/teams/" + sampleTeam.getId() + "/invite").with(csrf())
                        .param("email", "outsider@example.com")
                        .param("role", "EDITOR"))
                .andExpect(status().is3xxRedirection());

        var notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(outsiderUser.getId());
        org.junit.jupiter.api.Assertions.assertFalse(notifs.isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals("TEAM_INVITE", notifs.get(0).getType());
    }

    @Test
    void declineInvite_invalidatesTokenAndRedirects() throws Exception {
        com.podcastrelease.model.TeamInvite invite = new com.podcastrelease.model.TeamInvite(sampleTeam, "outsider@example.com", TeamRole.EDITOR, "declinetoken123", ownerUser);
        teamInviteRepository.save(invite);

        mockMvc.perform(post("/invites/declinetoken123/decline").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("successMessage", "Invitation declined."));

        org.junit.jupiter.api.Assertions.assertTrue(teamInviteRepository.findByToken("declinetoken123").isEmpty());
    }

    @Test
    void normalLogin_withoutPendingInvite_redirectsToDashboard() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "teamowner")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void unauthenticatedAccessToProtectedPage_redirectsToLogin_thenPostLoginRedirectsBackToSavedPage() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // 1. Visit protected page unauthenticated as HTML browser navigation
        mockMvc.perform(get("/teams").header("Accept", "text/html").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        // 2. Perform login using same session
        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "teamowner")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("http://localhost/teams*"));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void profilePage_rendersSuccessfully() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("currentUser", "memberships"));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void changePassword_success() throws Exception {
        mockMvc.perform(post("/profile/password").with(csrf())
                        .param("oldPassword", "pass123")
                        .param("newPassword", "newpass123")
                        .param("confirmPassword", "newpass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMessage", "Password updated successfully!"));
    }

    @Test
    @WithMockUser(username = "teamowner")
    void changePassword_wrongOldPassword_returnsError() throws Exception {
        mockMvc.perform(post("/profile/password").with(csrf())
                        .param("oldPassword", "wrongpass")
                        .param("newPassword", "newpass123")
                        .param("confirmPassword", "newpass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("errorMessage", "Current password is incorrect."));
    }
}
