package com.podcastrelease.controller;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase5JenkinsIntegrationTest {

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

    private Team team;
    private PodcastShow show;
    private Episode episode;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        User bot = userRepository.save(new User("jenkins_bot", "jenkins@example.com", "pass123", PlatformRole.SYSTEM));
        User producer = userRepository.save(new User("jk_producer", "prod@example.com", "pass123"));

        team = teamRepository.save(new Team("Jenkins Team", "Jenkins Pipeline Team"));
        teamMembershipRepository.save(new TeamMembership(team, producer, TeamRole.CREATOR));

        show = new PodcastShow("Jenkins Show", "jenkins-show", "Desc", "Tech", "Author", "show@example.com", null);
        show.setTeam(team);
        show = podcastShowRepository.save(show);

        episode = new Episode("Jenkins Pipeline Ep", "Desc", "http://audio.mp3", LocalDate.now());
        episode.setPodcastShow(show);
        episode.setTeam(team);
        episode.setCreatedBy(producer);
        episode.setStatus(EpisodeStatus.SCHEDULED);
        episode = episodeRepository.save(episode);
    }

    @Test
    void jenkinsCallback_success_updatesEpisodeToPublished() throws Exception {
        mockMvc.perform(post("/api/episodes/" + episode.getId() + "/jenkins-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUCCESS\", \"buildNumber\": \"105\", \"buildUrl\": \"http://jenkins:8080/job/105\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.buildNumber").value("105"));

        Episode updated = episodeRepository.findById(episode.getId()).orElseThrow();
        assertEquals(EpisodeStatus.PUBLISHED, updated.getStatus());

        List<AuditLog> logs = auditLogRepository.findByEpisodeIdOrderByTimestampDesc(episode.getId());
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().anyMatch(l -> l.getAction().contains("STATUS_CHANGE")));
    }

    @Test
    void jenkinsCallback_failure_updatesEpisodeToFailedWithReviewNotes() throws Exception {
        mockMvc.perform(post("/api/episodes/" + episode.getId() + "/jenkins-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FAILURE\", \"buildNumber\": \"106\", \"notes\": \"Validation gate error\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        Episode updated = episodeRepository.findById(episode.getId()).orElseThrow();
        assertEquals(EpisodeStatus.FAILED, updated.getStatus());
        assertTrue(updated.getReviewNotes().contains("Validation gate error"));
    }
}
