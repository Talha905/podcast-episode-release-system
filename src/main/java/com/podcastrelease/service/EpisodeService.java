package com.podcastrelease.service;

import com.podcastrelease.exception.TeamNotFoundException;
import com.podcastrelease.model.*;
import com.podcastrelease.repository.AuditLogRepository;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.TeamMembershipRepository;
import com.podcastrelease.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AudioInspectorService audioInspectorService;
    private final DistributionService distributionService;
    private final TeamSecurityService teamSecurityService;
    private final TeamMembershipRepository teamMembershipRepository;
    private final NotificationService notificationService;

    public EpisodeService(EpisodeRepository episodeRepository,
                          UserRepository userRepository,
                          AuditLogRepository auditLogRepository,
                          AudioInspectorService audioInspectorService,
                          DistributionService distributionService,
                          TeamSecurityService teamSecurityService,
                          TeamMembershipRepository teamMembershipRepository,
                          NotificationService notificationService) {
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.audioInspectorService = audioInspectorService;
        this.distributionService = distributionService;
        this.teamSecurityService = teamSecurityService;
        this.teamMembershipRepository = teamMembershipRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Episode create(Episode episode, String username) {
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        episode.setCreatedBy(user);
        if (episode.getStatus() == null) {
            episode.setStatus(EpisodeStatus.DRAFT);
        }
        episode.setCreatedAt(LocalDateTime.now());
        episode.setUpdatedAt(LocalDateTime.now());

        if (episode.getPodcastShow() != null && episode.getTeam() == null) {
            episode.setTeam(episode.getPodcastShow().getTeam());
        }

        if (episode.getAudioFileUrl() != null && episode.getFileSizeBytes() == null) {
            episode.setFileSizeBytes(352844L);
            episode.setDurationSeconds(240);
            episode.setFormattedDuration("00:04:00");
        }

        Episode saved = episodeRepository.save(episode);
        createAuditLog(saved.getId(), "CREATE_EPISODE (Status: " + saved.getStatus() + ")", user);
        return saved;
    }

    public List<Episode> findAll() {
        return episodeRepository.findAll();
    }

    public List<Episode> findAll(User user) {
        List<Long> teamIds = getUserTeamIds(user);
        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        return episodeRepository.findByTeamIdIn(teamIds);
    }

    public Episode findById(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException("Episode not found with id: " + id));
    }

    public Episode findById(Long id, User currentUser) {
        Episode episode = findById(id);
        if (episode.getTeam() != null) {
            teamSecurityService.verifyTeamMember(episode.getTeam().getId(), currentUser);
        }
        return episode;
    }

    @Transactional
    public Episode update(Long id, Episode updatedEpisode, String username) {
        Episode existing = findById(id);

        if (existing.getStatus() == EpisodeStatus.PUBLISHED || existing.getStatus() == EpisodeStatus.FAILED) {
            throw new IllegalStateException("Cannot update episode metadata when status is " + existing.getStatus() + ". Metadata updates are only allowed in active workflow status.");
        }

        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        if (existing.getTeam() != null) {
            teamSecurityService.verifyTeamMember(existing.getTeam().getId(), user);
        }

        existing.setTitle(updatedEpisode.getTitle());
        existing.setDescription(updatedEpisode.getDescription());
        existing.setAudioFileUrl(updatedEpisode.getAudioFileUrl());
        existing.setPublishDate(updatedEpisode.getPublishDate());
        if (updatedEpisode.getPodcastShow() != null) {
            existing.setPodcastShow(updatedEpisode.getPodcastShow());
            if (existing.getTeam() == null && updatedEpisode.getPodcastShow().getTeam() != null) {
                existing.setTeam(updatedEpisode.getPodcastShow().getTeam());
            }
        }
        if (updatedEpisode.getFileSizeBytes() != null) {
            existing.setFileSizeBytes(updatedEpisode.getFileSizeBytes());
            existing.setDurationSeconds(updatedEpisode.getDurationSeconds());
            existing.setFormattedDuration(updatedEpisode.getFormattedDuration());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        Episode saved = episodeRepository.save(existing);
        createAuditLog(saved.getId(), "UPDATE_METADATA", user);
        return saved;
    }

    @Transactional
    public Episode claimEpisode(Long episodeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Episode episode = findById(episodeId, user);

        TeamRole role = getUserTeamRole(episode, user);
        boolean isAdminOrSystem = (user.getPlatformRole() == PlatformRole.ADMIN || user.getPlatformRole() == PlatformRole.SYSTEM);

        if (!isAdminOrSystem && role != TeamRole.EDITOR && role != TeamRole.RELEASE_MANAGER && role != TeamRole.OWNER) {
            throw new IllegalArgumentException("Permission denied: Only Editors, Release Managers, or Owners can claim episodes.");
        }

        if (episode.getClaimedBy() != null && !episode.getClaimedBy().getId().equals(user.getId()) && !isAdminOrSystem && role != TeamRole.OWNER) {
            throw new IllegalStateException("Episode is already claimed by " + episode.getClaimedBy().getUsername());
        }

        episode.setClaimedBy(user);
        if (episode.getStatus() == EpisodeStatus.PENDING_EDIT || episode.getStatus() == EpisodeStatus.DRAFT) {
            episode.setStatus(EpisodeStatus.IN_EDITING);
        }
        episode.setUpdatedAt(LocalDateTime.now());

        Episode saved = episodeRepository.save(episode);
        createAuditLog(saved.getId(), "CLAIM_EPISODE by " + user.getUsername(), user);
        return saved;
    }

    @Transactional
    public Episode unclaimEpisode(Long episodeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Episode episode = findById(episodeId, user);

        TeamRole role = getUserTeamRole(episode, user);
        boolean isAdminOrSystem = (user.getPlatformRole() == PlatformRole.ADMIN || user.getPlatformRole() == PlatformRole.SYSTEM);

        if (episode.getClaimedBy() != null && !episode.getClaimedBy().getId().equals(user.getId()) && !isAdminOrSystem && role != TeamRole.OWNER) {
            throw new IllegalArgumentException("Permission denied: Cannot unclaim an episode claimed by another user.");
        }

        episode.setClaimedBy(null);
        episode.setUpdatedAt(LocalDateTime.now());

        Episode saved = episodeRepository.save(episode);
        createAuditLog(saved.getId(), "UNCLAIM_EPISODE by " + user.getUsername(), user);
        return saved;
    }

    @Transactional
    public Episode updateStatus(Long id, EpisodeStatus targetStatus, String username) {
        return updateStatus(id, targetStatus, username, null);
    }

    @Transactional
    public Episode updateStatus(Long id, EpisodeStatus targetStatus, String username, String reviewNotes) {
        Episode existing = findById(id);
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        if (existing.getTeam() != null) {
            teamSecurityService.verifyTeamMember(existing.getTeam().getId(), user);
        }

        EpisodeStatus currentStatus = existing.getStatus();
        boolean isOverrideRole = (user != null && (user.getPlatformRole() == PlatformRole.ADMIN || user.getPlatformRole() == PlatformRole.SYSTEM));
        TeamRole teamRole = getUserTeamRole(existing, user);

        if (!isOverrideRole) {
            validateStateTransitionMatrix(existing, currentStatus, targetStatus, user, teamRole);
        }

        // Enforce Self-Review Policy
        if ((targetStatus == EpisodeStatus.APPROVED || targetStatus == EpisodeStatus.PUBLISHED) &&
            user != null && existing.getCreatedBy() != null &&
            existing.getCreatedBy().getId().equals(user.getId()) &&
            existing.getTeam() != null) {

            Optional<TeamMembership> membership = teamMembershipRepository.findByTeamIdAndUserId(existing.getTeam().getId(), user.getId());
            if (membership.isPresent() && !membership.get().isAllowSelfReview() && !isOverrideRole) {
                throw new IllegalArgumentException("Self-review policy violation: Creator cannot approve or publish their own episode.");
            }
        }

        if (targetStatus == EpisodeStatus.PUBLISHED) {
            validatePrePublish(existing);
            existing.setApprovedBy(user);
        }

        if (reviewNotes != null && !reviewNotes.trim().isEmpty()) {
            existing.setReviewNotes(reviewNotes.trim());
        }

        existing.setStatus(targetStatus);
        existing.setUpdatedAt(LocalDateTime.now());

        Episode saved = episodeRepository.save(existing);
        String auditAction = "STATUS_CHANGE: " + currentStatus + " -> " + targetStatus +
                             (teamRole != null ? " (Role: " + teamRole + ")" : "") +
                             (isOverrideRole ? " (Platform Override)" : "");
        if (reviewNotes != null && !reviewNotes.trim().isEmpty()) {
            auditAction += " [Notes: " + reviewNotes.trim() + "]";
        }
        createAuditLog(saved, auditAction, user);

        notificationService.sendWebhookNotification(saved, currentStatus, targetStatus, user);

        if (targetStatus == EpisodeStatus.PUBLISHED || targetStatus == EpisodeStatus.FAILED) {
            distributionService.dispatchPublication(saved);
        }

        return saved;
    }

    private void validateStateTransitionMatrix(Episode episode, EpisodeStatus current, EpisodeStatus target, User user, TeamRole role) {
        if (current == target) return;

        switch (current) {
            case DRAFT:
                if (target == EpisodeStatus.PENDING_EDIT || target == EpisodeStatus.SUBMITTED_FOR_REVIEW || target == EpisodeStatus.VALIDATED) {
                    return;
                }
                if (target == EpisodeStatus.APPROVED && (role == TeamRole.RELEASE_MANAGER || role == TeamRole.OWNER)) {
                    return;
                }
                break;
            case PENDING_EDIT:
                if (target == EpisodeStatus.IN_EDITING || target == EpisodeStatus.DRAFT) {
                    return;
                }
                break;
            case IN_EDITING:
                if (target == EpisodeStatus.EDITED || target == EpisodeStatus.SUBMITTED_FOR_REVIEW || target == EpisodeStatus.PENDING_EDIT) {
                    return;
                }
                break;
            case EDITED:
                if (target == EpisodeStatus.SUBMITTED_FOR_REVIEW || target == EpisodeStatus.IN_EDITING) {
                    return;
                }
                break;
            case SUBMITTED_FOR_REVIEW:
                if (target == EpisodeStatus.APPROVED || target == EpisodeStatus.NEEDS_REVISION || target == EpisodeStatus.REJECTED || target == EpisodeStatus.PUBLISHED) {
                    if (role == TeamRole.RELEASE_MANAGER || role == TeamRole.OWNER || role == TeamRole.CREATOR) {
                        return;
                    }
                }
                break;
            case NEEDS_REVISION:
                if (target == EpisodeStatus.IN_EDITING || target == EpisodeStatus.SUBMITTED_FOR_REVIEW || target == EpisodeStatus.DRAFT) {
                    return;
                }
                break;
            case APPROVED:
                if (target == EpisodeStatus.SCHEDULED || target == EpisodeStatus.PUBLISHING || target == EpisodeStatus.PUBLISHED || target == EpisodeStatus.NEEDS_REVISION) {
                    return;
                }
                break;
            case SCHEDULED:
                if (target == EpisodeStatus.PUBLISHING || target == EpisodeStatus.PUBLISHED || target == EpisodeStatus.FAILED) {
                    return;
                }
                break;
            case PUBLISHING:
                if (target == EpisodeStatus.PUBLISHED || target == EpisodeStatus.FAILED) {
                    return;
                }
                break;
            case REJECTED:
                if (target == EpisodeStatus.DRAFT) {
                    return;
                }
                break;
            case FAILED:
                if (target == EpisodeStatus.DRAFT || target == EpisodeStatus.APPROVED || target == EpisodeStatus.PUBLISHED) {
                    return;
                }
                break;
            case VALIDATED:
                if (target == EpisodeStatus.PUBLISHED || target == EpisodeStatus.APPROVED || target == EpisodeStatus.FAILED) {
                    return;
                }
                break;
        }

        throw new IllegalArgumentException("Invalid status transition from " + current + " to " + target + " for user role " + (role != null ? role : "GUEST"));
    }

    private TeamRole getUserTeamRole(Episode episode, User user) {
        if (user == null || episode.getTeam() == null) return null;
        return teamMembershipRepository.findByTeamIdAndUserId(episode.getTeam().getId(), user.getId())
                .map(TeamMembership::getRole)
                .orElse(null);
    }

    private void validatePrePublish(Episode episode) {
        if (episode.getTitle() == null || episode.getTitle().trim().isEmpty() ||
            episode.getDescription() == null || episode.getDescription().trim().isEmpty() ||
            episode.getPublishDate() == null) {
            throw new IllegalArgumentException("Cannot validate episode: missing required metadata (title, description, or publishDate).");
        }

        String url = episode.getAudioFileUrl();
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Pre-publish validation failed: audio file reference missing.");
        }
    }

    public List<Episode> search(String title, EpisodeStatus status, LocalDate fromDate, LocalDate toDate) {
        String searchTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : null;
        return episodeRepository.searchEpisodes(searchTitle, status, fromDate, toDate);
    }

    public List<Episode> search(String title, EpisodeStatus status, LocalDate fromDate, LocalDate toDate, User user) {
        List<Long> teamIds = getUserTeamIds(user);
        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        String searchTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : null;
        return episodeRepository.searchEpisodesInTeams(teamIds, searchTitle, status, fromDate, toDate);
    }

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        long draftCount = episodeRepository.countByStatus(EpisodeStatus.DRAFT);
        long validatedCount = episodeRepository.countByStatus(EpisodeStatus.VALIDATED) + episodeRepository.countByStatus(EpisodeStatus.APPROVED);
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

    public Map<String, Object> getDashboardSummary(User user) {
        Map<String, Object> summary = new HashMap<>();
        List<Long> teamIds = getUserTeamIds(user);
        if (teamIds.isEmpty()) {
            summary.put("DRAFT", 0L);
            summary.put("VALIDATED", 0L);
            summary.put("PUBLISHED", 0L);
            summary.put("FAILED", 0L);
            summary.put("TOTAL", 0L);
            return summary;
        }

        long draftCount = episodeRepository.countByTeamIdInAndStatus(teamIds, EpisodeStatus.DRAFT);
        long validatedCount = episodeRepository.countByTeamIdInAndStatus(teamIds, EpisodeStatus.VALIDATED) + episodeRepository.countByTeamIdInAndStatus(teamIds, EpisodeStatus.APPROVED);
        long publishedCount = episodeRepository.countByTeamIdInAndStatus(teamIds, EpisodeStatus.PUBLISHED);
        long failedCount = episodeRepository.countByTeamIdInAndStatus(teamIds, EpisodeStatus.FAILED);
        long total = episodeRepository.countByTeamIdIn(teamIds);

        summary.put("DRAFT", draftCount);
        summary.put("VALIDATED", validatedCount);
        summary.put("PUBLISHED", publishedCount);
        summary.put("FAILED", failedCount);
        summary.put("TOTAL", total);
        return summary;
    }

    private List<Long> getUserTeamIds(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        return teamMembershipRepository.findByUserId(user.getId())
                .stream()
                .map(tm -> tm.getTeam().getId())
                .collect(Collectors.toList());
    }

    public List<AuditLog> getAuditLogs(Long episodeId) {
        return auditLogRepository.findByEpisodeIdOrderByTimestampDesc(episodeId);
    }

    public String saveAudioFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = org.springframework.util.StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.mp3");

        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".mp3") && !lowerName.endsWith(".wav") &&
            !lowerName.endsWith(".m4a") && !lowerName.endsWith(".aac") && !lowerName.endsWith(".ogg")) {
            throw new IllegalArgumentException("Unsupported audio file format. Only MP3, WAV, M4A, AAC, and OGG are allowed.");
        }

        try {
            java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/audio").toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(uploadPath);

            String uniqueFilename = java.util.UUID.randomUUID().toString() + "_" + originalFilename;
            java.nio.file.Path targetPath = uploadPath.resolve(uniqueFilename);

            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            return "/audio/" + uniqueFilename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save uploaded audio file: " + e.getMessage(), e);
        }
    }

    public AudioInspectorService.AudioMetadata inspectAudioFile(org.springframework.web.multipart.MultipartFile file, String url) {
        java.nio.file.Path path = null;
        if (url != null && url.startsWith("/audio/")) {
            String filename = url.substring("/audio/".length());
            path = java.nio.file.Paths.get("uploads/audio").resolve(filename).toAbsolutePath();
        }
        return audioInspectorService.inspect(file, path);
    }

    private void createAuditLog(Episode episode, String action, User user) {
        Long teamId = episode.getTeam() != null ? episode.getTeam().getId() : null;
        AuditLog log = new AuditLog(episode.getId(), teamId, action, user);
        auditLogRepository.save(log);
    }

    private void createAuditLog(Long episodeId, String action, User user) {
        Episode episode = episodeRepository.findById(episodeId).orElse(null);
        Long teamId = episode != null && episode.getTeam() != null ? episode.getTeam().getId() : null;
        AuditLog log = new AuditLog(episodeId, teamId, action, user);
        auditLogRepository.save(log);
    }
}