package main.java.com.podcastrelease.service;

import com.podcastrelease.model.Episode;
import com.podcastrelease.repository.EpisodeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EpisodeService {

    private final EpisodeRepository episodeRepository;

    public EpisodeService(EpisodeRepository episodeRepository) {
        this.episodeRepository = episodeRepository;
    }

    public Episode create(Episode episode) {
        episode.setCreatedAt(LocalDateTime.now());
        episode.setUpdatedAt(LocalDateTime.now());
        return episodeRepository.save(episode);
    }

    public List<Episode> findAll() {
        return episodeRepository.findAll();
    }

    public Episode findById(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + id));
    }
}