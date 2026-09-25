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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase3StateMachineTest {

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
    private EpisodeService episodeService;

    private User creator;
    private User editor;
    private User releaseManager;
    private Team team;
    private PodcastShow show;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        creator = userRepository.save(new User("sm_creator", "creator@example.com", "pass123"));
        editor = userRepository.save(new User("sm_editor", "editor@example.com", "pass123"));
        releaseManager = userRepository.save(new User("sm_rm", "rm@example.com", "pass123"));

        team = teamRepository.save(new Team("Pipeline Team", "Pipeline Team Desc"));

        teamMembershipRepository.save(new TeamMembership(team, creator, TeamRole.CREATOR, false));
        teamMembershipRepository.save(new TeamMembership(team, editor, TeamRole.EDITOR, true));
        teamMembershipRepository.save(new TeamMembership(team, releaseManager, TeamRole.RELEASE_MANAGER, true));

        show = new PodcastShow("Pipeline Show", "pipeline-show", "Desc", "Tech", "Author", "show@example.com", null);
        show.setTeam(team);
        show = podcastShowRepository.save(show);
    }

    @Test
    @WithMockUser(username = "sm_creator")
    void fullValidLifecycle_fromDraftToPublished() {
        Episode ep = new Episode("Full Lifecycle Ep", "Desc", "https://audio.com/file.mp3", LocalDate.now());
        ep.setPodcastShow(show);
        ep.setTeam(team);
        Episode saved = episodeService.create(ep, "sm_creator");
        assertEquals(EpisodeStatus.DRAFT, saved.getStatus());

        // DRAFT -> PENDING_EDIT
        Episode pendingEdit = episodeService.updateStatus(saved.getId(), EpisodeStatus.PENDING_EDIT, "sm_creator");
        assertEquals(EpisodeStatus.PENDING_EDIT, pendingEdit.getStatus());

        // PENDING_EDIT -> IN_EDITING (Editor claims)
        Episode claimed = episodeService.claimEpisode(saved.getId(), "sm_editor");
        assertEquals(EpisodeStatus.IN_EDITING, claimed.getStatus());
        assertEquals("sm_editor", claimed.getClaimedBy().getUsername());

        // IN_EDITING -> EDITED
        Episode edited = episodeService.updateStatus(saved.getId(), EpisodeStatus.EDITED, "sm_editor");
        assertEquals(EpisodeStatus.EDITED, edited.getStatus());

        // EDITED -> SUBMITTED_FOR_REVIEW
        Episode submitted = episodeService.updateStatus(saved.getId(), EpisodeStatus.SUBMITTED_FOR_REVIEW, "sm_editor");
        assertEquals(EpisodeStatus.SUBMITTED_FOR_REVIEW, submitted.getStatus());

        // SUBMITTED_FOR_REVIEW -> APPROVED (Release Manager approves)
        Episode approved = episodeService.updateStatus(saved.getId(), EpisodeStatus.APPROVED, "sm_rm");
        assertEquals(EpisodeStatus.APPROVED, approved.getStatus());

        // APPROVED -> SCHEDULED
        Episode scheduled = episodeService.updateStatus(saved.getId(), EpisodeStatus.SCHEDULED, "sm_rm");
        assertEquals(EpisodeStatus.SCHEDULED, scheduled.getStatus());

        // SCHEDULED -> PUBLISHED
        Episode published = episodeService.updateStatus(saved.getId(), EpisodeStatus.PUBLISHED, "sm_rm");
        assertEquals(EpisodeStatus.PUBLISHED, published.getStatus());

        // Verify Audit Logs contain role details
        List<AuditLog> logs = episodeService.getAuditLogs(saved.getId());
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().anyMatch(l -> l.getAction().contains("STATUS_CHANGE")));
    }

    @Test
    @WithMockUser(username = "sm_creator")
    void invalidStatusTransition_throwsException() {
        Episode ep = new Episode("Invalid Ep", "Desc", "https://audio.com/file.mp3", LocalDate.now());
        ep.setPodcastShow(show);
        ep.setTeam(team);
        Episode saved = episodeService.create(ep, "sm_creator");

        // DRAFT directly to PUBLISHING is invalid
        assertThrows(IllegalArgumentException.class, () ->
            episodeService.updateStatus(saved.getId(), EpisodeStatus.PUBLISHING, "sm_creator")
        );
    }
}
