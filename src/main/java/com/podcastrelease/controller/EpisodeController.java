package com.podcastrelease.controller;

import com.podcastrelease.model.AuditLog;
import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.service.EpisodeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EpisodeController {

    private final EpisodeService episodeService;

    public EpisodeController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @PostMapping("/episodes")
    public ResponseEntity<Episode> create(@Valid @RequestBody Episode episode, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Episode saved = episodeService.create(episode, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/episodes")
    public ResponseEntity<List<Episode>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) EpisodeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (title != null || status != null || from != null || to != null) {
            return ResponseEntity.ok(episodeService.search(title, status, from, to));
        }
        return ResponseEntity.ok(episodeService.findAll());
    }

    @GetMapping("/episodes/{id}")
    public ResponseEntity<Episode> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(episodeService.findById(id));
    }

    @PutMapping("/episodes/{id}")
    public ResponseEntity<Episode> update(
            @PathVariable Long id,
            @Valid @RequestBody Episode updatedEpisode,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : null;
        Episode saved = episodeService.update(id, updatedEpisode, username);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/episodes/{id}/status")
    public ResponseEntity<Episode> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String statusStr = body.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Field 'status' is required");
        }

        EpisodeStatus targetStatus = EpisodeStatus.valueOf(statusStr.toUpperCase());
        String username = authentication != null ? authentication.getName() : null;

        Episode updated = episodeService.updateStatus(id, targetStatus, username);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(episodeService.getDashboardSummary());
    }

    @GetMapping("/episodes/{id}/audit")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@PathVariable Long id) {
        return ResponseEntity.ok(episodeService.getAuditLogs(id));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}