package com.podcastrelease.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EpisodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User producerUser;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        producerUser = userRepository.save(new User("producer", "producer@example.com", "pass"));
        producerUser.setEnabled(true);
        userRepository.save(producerUser);

        testTeam = teamRepository.save(new Team("Test Team", "Test Desc"));
        teamMembershipRepository.save(new TeamMembership(testTeam, producerUser, TeamRole.OWNER));
    }

    @Test
    @WithMockUser(username = "producer", roles = {"PRODUCER"})
    void createEpisode_returns201Created() throws Exception {
        Episode episode = new Episode("REST Ep Title", "REST Ep Desc", "http://audio.com/file.mp3", LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/episodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(episode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("REST Ep Title"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "producer", roles = {"PRODUCER"})
    void getEpisodes_returnsEpisodeList() throws Exception {
        Episode episode = new Episode("List Test Ep", "Desc", "http://audio.mp3", LocalDate.now());
        episode.setTeam(testTeam);
        episodeRepository.save(episode);

        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("List Test Ep"));
    }

    @Test
    @WithMockUser(username = "producer", roles = {"PRODUCER"})
    void updateStatus_patchTransition() throws Exception {
        Episode episode = new Episode("Patch Status Ep", "Desc", "https://storage.com/file.mp3", LocalDate.now());
        episode.setTeam(testTeam);
        Episode saved = episodeRepository.save(episode);

        mockMvc.perform(patch("/api/episodes/" + saved.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "VALIDATED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @WithMockUser(username = "producer", roles = {"PRODUCER"})
    void getDashboardView_returns200() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("episodes"));
    }

    @Test
    @WithMockUser(username = "producer", roles = {"PRODUCER"})
    void getEpisodeDetailView_rendersDetailHtmlWithoutErrors() throws Exception {
        Episode episode = new Episode("Detail View Test Ep", "Description text", "/audio/sample.mp3", LocalDate.now());
        episode.setTeam(testTeam);
        episode.setStatus(EpisodeStatus.VALIDATED);
        episode.setReviewNotes("Looks good!");
        Episode saved = episodeRepository.save(episode);

        mockMvc.perform(get("/episodes/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"))
                .andExpect(model().attributeExists("episode", "auditLogs", "statuses"));
    }
}
