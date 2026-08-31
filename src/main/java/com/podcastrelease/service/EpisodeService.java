package com.podcastrelease.service;

import com.podcastrelease.model.AuditLog;
import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.User;
import com.podcastrelease.model.UserRole;
import com.podcastrelease.repository.AuditLogRepository;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public EpisodeService(EpisodeRepository episodeRepository,
                          UserRepository userRepository,
                          AuditLogRepository auditLogRepository) {
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Episode create(Episode episode, String username) {
        User user = null;
        if (username != null) {
            user = userRepository.findByUsername(username).orElse(null);
        }

        episode.setCreatedBy(user);
        if (episode.getStatus() == null) {
            episode.setStatus(EpisodeStatus.DRAFT);
        }
        episode.setCreatedAt(LocalDateTime.now());
        episode.setUpdatedAt(LocalDateTime.now());

        Episode saved = episodeRepository.save(episode);
        createAuditLog(saved.getId(), "CREATE_EPISODE (Status: " + saved.getStatus() + ")", user);
        return saved;
    }

    public List<Episode> findAll() {
        return episodeRepository.findAll();
    }

    public Episode findById(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found with id: " + id));
    }

    @Transactional
    public Episode update(Long id, Episode updatedEpisode, String username) {
        Episode existing = findById(id);

        if (existing.getStatus() == EpisodeStatus.PUBLISHED || existing.getStatus() == EpisodeStatus.FAILED) {
            throw new IllegalStateException("Cannot update episode metadata when status is " + existing.getStatus() + ". Metadata updates are only allowed in DRAFT or VALIDATED status.");
        }

        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        existing.setTitle(updatedEpisode.getTitle());
        existing.setDescription(updatedEpisode.getDescription());
        existing.setAudioFileUrl(updatedEpisode.getAudioFileUrl());
        existing.setPublishDate(updatedEpisode.getPublishDate());
        existing.setUpdatedAt(LocalDateTime.now());

        Episode saved = episodeRepository.save(existing);
        createAuditLog(saved.getId(), "UPDATE_METADATA", user);
        return saved;
    }

    @Transactional
    public Episode updateStatus(Long id, EpisodeStatus targetStatus, String username) {
        Episode existing = findById(id);
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        EpisodeStatus currentStatus = existing.getStatus();

        boolean isOverrideRole = (user != null && (user.getRole() == UserRole.HOST || user.getRole() == UserRole.ADMIN));

        if (!isOverrideRole) {
            validateWorkflowTransition(existing, currentStatus, targetStatus);
        }

        if (targetStatus == EpisodeStatus.PUBLISHED) {
            validatePrePublish(existing);
        }

        existing.setStatus(targetStatus);
        existing.setUpdatedAt(LocalDateTime.now());

        Episode saved = episodeRepository.save(existing);
        createAuditLog(saved.getId(), "STATUS_CHANGE: " + currentStatus + " -> " + targetStatus + (isOverrideRole ? " (Role Override)" : ""), user);
        return saved;
    }

    private void validateWorkflowTransition(Episode episode, EpisodeStatus current, EpisodeStatus target) {
        if (current == target) {
            return;
        }

        if (current == EpisodeStatus.DRAFT && target == EpisodeStatus.VALIDATED) {
            validateMetadataPresent(episode);
            return;
        }

        if (current == EpisodeStatus.VALIDATED && (target == EpisodeStatus.PUBLISHED || target == EpisodeStatus.FAILED)) {
            return;
        }

        throw new IllegalArgumentException("Invalid status transition from " + current + " to " + target + ". Producer standard workflow is DRAFT -> VALIDATED -> PUBLISHED/FAILED.");
    }

    private void validateMetadataPresent(Episode episode) {
        if (episode.getTitle() == null || episode.getTitle().trim().isEmpty() ||
            episode.getDescription() == null || episode.getDescription().trim().isEmpty() ||
            episode.getPublishDate() == null) {
            throw new IllegalArgumentException("Cannot validate episode: missing required metadata (title, description, or publishDate).");
        }
    }

    private void validatePrePublish(Episode episode) {
        validateMetadataPresent(episode);
        String url = episode.getAudioFileUrl();
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Pre-publish validation failed: audio file reference missing.");
        }

        String lowerUrl = url.trim().toLowerCase();
        boolean isValidUrlOrExtension = lowerUrl.startsWith("http://") ||
                                        lowerUrl.startsWith("https://") ||
                                        lowerUrl.startsWith("s3://") ||
                                        lowerUrl.startsWith("file://") ||
                                        lowerUrl.startsWith("/") ||
                                        lowerUrl.endsWith(".mp3") ||
                                        lowerUrl.endsWith(".wav") ||
                                        lowerUrl.endsWith(".m4a") ||
                                        lowerUrl.endsWith(".aac") ||
                                        lowerUrl.endsWith(".ogg");

        if (!isValidUrlOrExtension) {
            throw new IllegalArgumentException("Pre-publish validation failed: invalid audio file format or URL schema.");
        }
    }

    public List<Episode> search(String title, EpisodeStatus status, LocalDate fromDate, LocalDate toDate) {
        String searchTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : null;
        return episodeRepository.searchEpisodes(searchTitle, status, fromDate, toDate);
    }

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        long draftCount = episodeRepository.countByStatus(EpisodeStatus.DRAFT);
        long validatedCount = episodeRepository.countByStatus(EpisodeStatus.VALIDATED);
        long publishedCount = episodeRepository.countByStatus(EpisodeStatus.PUBLISHED);
        long failedCount = episodeRepository.countByStatus(EpisodeStatus.FAILED);
        long total = episodeRepository.count();

        summary.put("DRAFT", draftCount);
        summary.put("VALIDATED", validatedCount);
        summary.put("PUBLISHED", publishedCount);
        summary.put("FAILED", failedCount);
        summary.put("TOTAL", total);
        return summary;
    }

    public List<AuditLog> getAuditLogs(Long episodeId) {
        return auditLogRepository.findByEpisodeIdOrderByTimestampDesc(episodeId);
    }

    private void createAuditLog(Long episodeId, String action, User user) {
        AuditLog log = new AuditLog(episodeId, action, user);
        auditLogRepository.save(log);
    }
}