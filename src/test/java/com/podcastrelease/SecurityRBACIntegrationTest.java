package com.podcastrelease;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.podcastrelease.model.Episode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRBACIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedAccess_toApiEpisodes_returns401() throws Exception {
        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "host", roles = {"HOST"})
    void hostRole_createEpisode_returns403Forbidden() throws Exception {
        Episode episode = new Episode("Forbidden Ep", "Desc", "http://audio.mp3", LocalDate.now());

        mockMvc.perform(post("/api/episodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(episode)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "producer", roles = {"PRODUCER"})
    void producerRole_createEpisode_returns201Created() throws Exception {
        Episode episode = new Episode("Producer Ep", "Desc", "http://audio.mp3", LocalDate.now());

        mockMvc.perform(post("/api/episodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(episode)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminRole_createEpisode_returns201Created() throws Exception {
        Episode episode = new Episode("Admin Ep", "Desc", "http://audio.mp3", LocalDate.now());

        mockMvc.perform(post("/api/episodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(episode)))
                .andExpect(status().isCreated());
    }
}
