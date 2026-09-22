package com.podcastrelease;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.service.ScheduledPublisherTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScheduledPublisherTaskTest {

    @Autowired
    private ScheduledPublisherTask scheduledPublisherTask;

    @Autowired
    private EpisodeRepository episodeRepository;

    @BeforeEach
    void setUp() {
        episodeRepository.deleteAll();
    }

    @Test
    void processScheduledReleases_publishesValidatedEpisodesWithPastOrTodayPublishDate() {
        Episode epReadyPast = new Episode("Ready Past Ep", "Desc", "/audio/ep1.mp3", LocalDate.now().minusDays(1));
        epReadyPast.setStatus(EpisodeStatus.VALIDATED);
        episodeRepository.save(epReadyPast);

        Episode epReadyToday = new Episode("Ready Today Ep", "Desc", "/audio/ep2.mp3", LocalDate.now());
        epReadyToday.setStatus(EpisodeStatus.VALIDATED);
        episodeRepository.save(epReadyToday);

        Episode epFuture = new Episode("Future Scheduled Ep", "Desc", "/audio/ep3.mp3", LocalDate.now().plusDays(5));
        epFuture.setStatus(EpisodeStatus.VALIDATED);
        episodeRepository.save(epFuture);

        Episode epDraft = new Episode("Draft Ep", "Desc", "/audio/ep4.mp3", LocalDate.now().minusDays(1));
        epDraft.setStatus(EpisodeStatus.DRAFT);
        episodeRepository.save(epDraft);

        scheduledPublisherTask.processScheduledReleases();

        Episode updatedPast = episodeRepository.findById(epReadyPast.getId()).orElseThrow();
        assertEquals(EpisodeStatus.PUBLISHED, updatedPast.getStatus());

        Episode updatedToday = episodeRepository.findById(epReadyToday.getId()).orElseThrow();
        assertEquals(EpisodeStatus.PUBLISHED, updatedToday.getStatus());

        Episode updatedFuture = episodeRepository.findById(epFuture.getId()).orElseThrow();
        assertEquals(EpisodeStatus.VALIDATED, updatedFuture.getStatus(), "Future scheduled episodes must remain VALIDATED");

        Episode updatedDraft = episodeRepository.findById(epDraft.getId()).orElseThrow();
        assertEquals(EpisodeStatus.DRAFT, updatedDraft.getStatus(), "Draft episodes must not be auto-published");
    }
}
