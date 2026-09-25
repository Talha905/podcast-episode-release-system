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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase2ShowAndEpisodeLifecycleTest {

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
    private EpisodeService episodeService;

    private User creatorUser;
    private User editorUser;
    private User outsiderUser;
    private Team team;
    private PodcastShow show;

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

        creatorUser = userRepository.save(new User("creator", "creator@example.com", "pass123"));
        editorUser = userRepository.save(new User("editor", "editor@example.com", "pass123"));
        outsiderUser = userRepository.save(new User("outsider", "outsider@example.com", "pass123"));

        team = teamRepository.save(new Team("Production Team", "Podcast Team"));

        // Creator has allowSelfReview = false
        teamMembershipRepository.save(new TeamMembership(team, creatorUser, TeamRole.CREATOR, false));
        // Editor has allowSelfReview = true
        teamMembershipRepository.save(new TeamMembership(team, editorUser, TeamRole.EDITOR, true));

        show = new PodcastShow("Tech Weekly", "tech-weekly", "Desc", "Tech", "Author", "tech@example.com", null);
        show.setTeam(team);
        show = podcastShowRepository.save(show);
    }

    @Test
    @WithMockUser(username = "creator")
    void createEpisode_linksToTeam() {
        Episode ep = new Episode("Ep 1", "Desc", "http://audio.mp3", LocalDate.now());
        ep.setPodcastShow(show);

        Episode created = episodeService.create(ep, "creator");
        assertNotNull(created.getId());
        assertEquals(team.getId(), created.getTeam().getId());
    }

    @Test
    @WithMockUser(username = "editor")
    void claimAndUnclaimEpisode_success() throws Exception {
        Episode ep = new Episode("Ep Pool", "Desc", "http://audio.mp3", LocalDate.now());
        ep.setPodcastShow(show);
        ep.setTeam(team);
        Episode saved = episodeRepository.save(ep);

        // Claim
        mockMvc.perform(post("/api/episodes/" + saved.getId() + "/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedBy.username").value("editor"))
                .andExpect(jsonPath("$.status").value("IN_EDITING"));

        // Unclaim
        mockMvc.perform(post("/api/episodes/" + saved.getId() + "/unclaim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedBy").doesNotExist());
    }

    @Test
    @WithMockUser(username = "creator")
    void selfReviewPolicy_blocksSelfApprovalWhenDisabled() {
        Episode ep = new Episode("Self Review Ep", "Desc", "http://audio.mp3", LocalDate.now());
        ep.setPodcastShow(show);
        ep.setTeam(team);
        ep.setCreatedBy(creatorUser);
        Episode saved = episodeRepository.save(ep);

        // Creator attempt to approve self-created episode should fail
        assertThrows(IllegalArgumentException.class, () ->
            episodeService.updateStatus(saved.getId(), EpisodeStatus.APPROVED, "creator")
        );
    }

    @Test
    @WithMockUser(username = "outsider")
    void getEpisode_returns404ForNonTeamMember() throws Exception {
        Episode ep = new Episode("Secret Ep", "Desc", "http://audio.mp3", LocalDate.now());
        ep.setPodcastShow(show);
        ep.setTeam(team);
        Episode saved = episodeRepository.save(ep);

        // Non-team member receives 404 NOT FOUND
        mockMvc.perform(get("/api/episodes/" + saved.getId()))
                .andExpect(status().isNotFound());
    }
}
