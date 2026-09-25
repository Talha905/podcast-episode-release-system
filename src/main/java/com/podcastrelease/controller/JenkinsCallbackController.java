package com.podcastrelease.controller;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.PlatformRole;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.UserRepository;
import com.podcastrelease.service.EpisodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JenkinsCallbackController {

    private final EpisodeService episodeService;
    private final EpisodeRepository episodeRepository;
    private final UserRepository userRepository;

    public JenkinsCallbackController(EpisodeService episodeService,
                                     EpisodeRepository episodeRepository,
                                     UserRepository userRepository) {
        this.episodeService = episodeService;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/jenkins/callback")
    @Transactional
    public ResponseEntity<Map<String, Object>> genericCallback(@RequestBody Map<String, Object> body, Authentication authentication) {
        String statusStr = (String) body.get("status");
        String buildNumber = body.get("buildNumber") != null ? body.get("buildNumber").toString() : "UNKNOWN";
        String buildUrl = (String) body.get("buildUrl");
        String notes = (String) body.get("notes");
        Long episodeId = body.get("episodeId") != null ? Long.valueOf(body.get("episodeId").toString()) : null;

        String authUsername = authentication != null ? authentication.getName() : "jenkins_bot";
        User sysUser = userRepository.findByUsername(authUsername)
                .orElseGet(() -> userRepository.findByUsername("jenkins_bot").orElse(null));

        if (episodeId != null) {
            return processEpisodeCallback(episodeId, statusStr, buildNumber, buildUrl, notes, sysUser);
        }

        // If no specific episodeId is given, target episodes currently in PUBLISHING or SCHEDULED status
        List<Episode> targetEpisodes = episodeRepository.findByStatus(EpisodeStatus.PUBLISHING);
        if (targetEpisodes.isEmpty()) {
            targetEpisodes = episodeRepository.findByStatus(EpisodeStatus.SCHEDULED);
        }

        int processedCount = 0;
        for (Episode ep : targetEpisodes) {
            processEpisodeCallback(ep.getId(), statusStr, buildNumber, buildUrl, notes, sysUser);
            processedCount++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Jenkins callback processed successfully");
        response.put("processedEpisodesCount", processedCount);
        response.put("buildNumber", buildNumber);
        response.put("status", statusStr);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/episodes/{episodeId}/jenkins-callback")
    @Transactional
    public ResponseEntity<Map<String, Object>> episodeSpecificCallback(@PathVariable Long episodeId,
                                                                        @RequestBody Map<String, Object> body,
                                                                        Authentication authentication) {
        String statusStr = (String) body.get("status");
        String buildNumber = body.get("buildNumber") != null ? body.get("buildNumber").toString() : "UNKNOWN";
        String buildUrl = (String) body.get("buildUrl");
        String notes = (String) body.get("notes");

        String authUsername = authentication != null ? authentication.getName() : "jenkins_bot";
        User sysUser = userRepository.findByUsername(authUsername)
                .orElseGet(() -> userRepository.findByUsername("jenkins_bot").orElse(null));

        return processEpisodeCallback(episodeId, statusStr, buildNumber, buildUrl, notes, sysUser);
    }

    private ResponseEntity<Map<String, Object>> processEpisodeCallback(Long episodeId,
                                                                       String statusStr,
                                                                       String buildNumber,
                                                                       String buildUrl,
                                                                       String notes,
                                                                       User sysUser) {
        Episode episode = episodeService.findById(episodeId);
        String username = sysUser != null ? sysUser.getUsername() : "jenkins_bot";

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(statusStr);
        EpisodeStatus targetStatus = isSuccess ? EpisodeStatus.PUBLISHED : EpisodeStatus.FAILED;

        String reviewNotes = "Jenkins Pipeline Build #" + buildNumber + " [" + (statusStr != null ? statusStr.toUpperCase() : "CALLBACK") + "]";
        if (buildUrl != null && !buildUrl.isBlank()) {
            reviewNotes += " - Log: " + buildUrl;
        }
        if (notes != null && !notes.isBlank()) {
            reviewNotes += " (" + notes + ")";
        }

        Episode updated = episodeService.updateStatus(episode.getId(), targetStatus, username, reviewNotes);

        Map<String, Object> response = new HashMap<>();
        response.put("episodeId", updated.getId());
        response.put("episodeTitle", updated.getTitle());
        response.put("status", updated.getStatus());
        response.put("buildNumber", buildNumber);
        response.put("message", "Jenkins callback applied successfully to episode " + episodeId);

        return ResponseEntity.ok(response);
    }
}
