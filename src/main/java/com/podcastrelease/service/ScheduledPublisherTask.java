package com.podcastrelease.service;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.repository.EpisodeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ScheduledPublisherTask {

    private final EpisodeRepository episodeRepository;
    private final EpisodeService episodeService;

    public ScheduledPublisherTask(EpisodeRepository episodeRepository, EpisodeService episodeService) {
        this.episodeRepository = episodeRepository;
        this.episodeService = episodeService;
    }

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void processScheduledReleases() {
        List<Episode> validatedEpisodes = episodeRepository.findByStatus(EpisodeStatus.VALIDATED);
        LocalDate today = LocalDate.now();

        for (Episode ep : validatedEpisodes) {
            if (ep.getPublishDate() != null && !ep.getPublishDate().isAfter(today)) {
                try {
                    episodeService.updateStatus(ep.getId(), EpisodeStatus.PUBLISHED, "ScheduledAutoPublisher");
                } catch (Exception ignored) {}
            }
        }
    }
}
