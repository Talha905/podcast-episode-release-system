package com.podcastrelease.controller;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class Phase8CrossTeamIsolationTest {

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
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private User userC;

    private Episode episodeA;
    private Episode episodeB;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        // User A & Team A
        userA = userRepository.save(new User("userA", "usera@example.com", passwordEncoder.encode("password")));
        userA.setEnabled(true);
        userRepository.save(userA);

        Team teamA = teamRepository.save(new Team("Team Alpha", "Description A"));
        teamMembershipRepository.save(new TeamMembership(teamA, userA, TeamRole.OWNER));

        PodcastShow showA = new PodcastShow("Show Alpha", "Feed A", "Author A", "http://cover.jpg");
        showA.setTeam(teamA);
        showA = podcastShowRepository.save(showA);

        episodeA = new Episode("Episode Alpha Title", "Description Alpha", "http://audio.com/alpha.mp3", LocalDate.now());
        episodeA.setPodcastShow(showA);
        episodeA.setTeam(teamA);
        episodeA.setCreatedBy(userA);
        episodeA.setStatus(EpisodeStatus.DRAFT);
        episodeA = episodeRepository.save(episodeA);

        // User B & Team B
        userB = userRepository.save(new User("userB", "userb@example.com", passwordEncoder.encode("password")));
        userB.setEnabled(true);
        userRepository.save(userB);

        Team teamB = teamRepository.save(new Team("Team Beta", "Description B"));
        teamMembershipRepository.save(new TeamMembership(teamB, userB, TeamRole.OWNER));

        PodcastShow showB = new PodcastShow("Show Beta", "Feed B", "Author B", "http://cover.jpg");
        showB.setTeam(teamB);
        showB = podcastShowRepository.save(showB);

        episodeB = new Episode("Episode Beta Title", "Description Beta", "http://audio.com/beta.mp3", LocalDate.now());
        episodeB.setPodcastShow(showB);
        episodeB.setTeam(teamB);
        episodeB.setCreatedBy(userB);
        episodeB.setStatus(EpisodeStatus.DRAFT);
        episodeB = episodeRepository.save(episodeB);

        // User C with zero team memberships
        userC = userRepository.save(new User("userC", "userc@example.com", passwordEncoder.encode("password")));
        userC.setEnabled(true);
        userRepository.save(userC);
    }

    @Test
    @WithMockUser(username = "userA")
    void userA_canOnlySeeTeamAEpisodes_andReturns404ForTeamBEpisodes() throws Exception {
        // 1. Main Episode List Endpoint
        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Episode Alpha Title"));

        // 2. Dashboard View
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("episodes", hasSize(1)))
                .andExpect(model().attributeExists("summary"));

        // 3. Dashboard Summary Endpoint
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TOTAL").value(1))
                .andExpect(jsonPath("$.DRAFT").value(1));

        // 4. Direct GET by ID - Own Episode (HTTP 200)
        mockMvc.perform(get("/api/episodes/" + episodeA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episode Alpha Title"));

        mockMvc.perform(get("/episodes/" + episodeA.getId()))
                .andExpect(status().isOk());

        // 5. Direct GET by ID - Other Team's Episode (HTTP 404 NOT FOUND)
        mockMvc.perform(get("/api/episodes/" + episodeB.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/episodes/" + episodeB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "userB")
    void userB_canOnlySeeTeamBEpisodes_andReturns404ForTeamAEpisodes() throws Exception {
        // 1. Main Episode List Endpoint
        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Episode Beta Title"));

        // 2. Dashboard View
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("episodes", hasSize(1)));

        // 3. Direct GET by ID - Own Episode (HTTP 200)
        mockMvc.perform(get("/api/episodes/" + episodeB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episode Beta Title"));

        // 4. Direct GET by ID - Other Team's Episode (HTTP 404 NOT FOUND)
        mockMvc.perform(get("/api/episodes/" + episodeA.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/episodes/" + episodeA.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "userC")
    void userC_withZeroTeamMemberships_seesEmptyDashboardAndList() throws Exception {
        // 1. Main Episode List Endpoint (Empty list)
        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 2. Dashboard View (0 episodes)
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("episodes", hasSize(0)));

        // 3. Dashboard Summary Endpoint (All 0s)
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TOTAL").value(0))
                .andExpect(jsonPath("$.DRAFT").value(0));

        // 4. Direct GET by ID for Episode A & B (HTTP 404 NOT FOUND)
        mockMvc.perform(get("/api/episodes/" + episodeA.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/episodes/" + episodeB.getId()))
                .andExpect(status().isNotFound());
    }
}
