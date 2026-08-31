package com.podcastrelease.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.repository.EpisodeRepository;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        episodeRepository.deleteAll();
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
}
