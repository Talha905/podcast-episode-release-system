package com.podcastrelease.controller;

import com.podcastrelease.model.Episode;
import com.podcastrelease.service.EpisodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/episodes")
public class EpisodeController {

    private final EpisodeService episodeService;

    public EpisodeController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @PostMapping
    public ResponseEntity<Episode> create(@Valid @RequestBody Episode episode) {
        Episode saved = episodeService.create(episode);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Episode>> list() {
        return ResponseEntity.ok(episodeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Episode> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(episodeService.findById(id));
    }
}