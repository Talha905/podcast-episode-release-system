package com.podcastrelease;

import com.podcastrelease.model.Episode;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.service.EpisodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EpisodeServiceTest {

    @Autowired
    private EpisodeService episodeService;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Test
    void createEpisode_persistsWithDraftStatus() {
        Episode episode = new Episode();
        episode.setTitle("Episode 1: Pilot");
        episode.setDescription("Our first episode");
        episode.setPublishDate(LocalDate.now().plusDays(3));

        Episode saved = episodeService.create(episode);

        assertNotNull(saved.getId());
        assertEquals("DRAFT", saved.getStatus().name());
        assertEquals(1, episodeRepository.findAll().size());
    }
}