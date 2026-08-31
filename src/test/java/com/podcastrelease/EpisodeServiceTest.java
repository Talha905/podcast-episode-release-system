package com.podcastrelease;

import com.podcastrelease.model.AuditLog;
import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.repository.AuditLogRepository;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.service.EpisodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EpisodeServiceTest {

    @Autowired
    private EpisodeService episodeService;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
    }

    @Test
    void createEpisode_persistsWithDraftStatus() {
        Episode episode = new Episode();
        episode.setTitle("Episode 1: Pilot");
        episode.setDescription("Our first episode");
        episode.setPublishDate(LocalDate.now().plusDays(3));

        Episode saved = episodeService.create(episode, "producer");

        assertNotNull(saved.getId());
        assertEquals(EpisodeStatus.DRAFT, saved.getStatus());
        assertEquals("producer", saved.getCreatedBy().getUsername());
    }

    @Test
    void updateEpisodeMetadata_allowedInDraftOrValidatedStatus() {
        Episode episode = new Episode("Draft Ep", "Initial Description", "http://audio.mp3", LocalDate.now());
        Episode saved = episodeService.create(episode, "producer");

        saved.setTitle("Updated Title");
        Episode updated = episodeService.update(saved.getId(), saved, "producer");
        assertEquals("Updated Title", updated.getTitle());
    }

    @Test
    void updateEpisodeMetadata_failsWhenPublished() {
        Episode episode = new Episode("Published Ep", "Desc", "https://audio.com/ep.mp3", LocalDate.now());
        Episode saved = episodeService.create(episode, "producer");
        episodeService.updateStatus(saved.getId(), EpisodeStatus.VALIDATED, "producer");
        episodeService.updateStatus(saved.getId(), EpisodeStatus.PUBLISHED, "producer");

        saved.setTitle("Should Fail");
        assertThrows(IllegalStateException.class, () -> episodeService.update(saved.getId(), saved, "producer"));
    }

    @Test
    void statusTransition_validatesPrePublishAudioFile() {
        Episode episode = new Episode("No Audio Ep", "Desc", null, LocalDate.now());
        Episode saved = episodeService.create(episode, "producer");

        episodeService.updateStatus(saved.getId(), EpisodeStatus.VALIDATED, "producer");

        // Pre-publish should fail when audio file URL is missing
        assertThrows(IllegalArgumentException.class, () ->
            episodeService.updateStatus(saved.getId(), EpisodeStatus.PUBLISHED, "producer")
        );

        // Set valid audio file URL and transition should succeed
        saved.setAudioFileUrl("https://storage.com/audio/ep.mp3");
        episodeService.update(saved.getId(), saved, "producer");
        Episode published = episodeService.updateStatus(saved.getId(), EpisodeStatus.PUBLISHED, "producer");
        assertEquals(EpisodeStatus.PUBLISHED, published.getStatus());
    }

    @Test
    void statusTransition_adminOverrideAllowed() {
        Episode episode = new Episode("Draft Ep", "Desc", null, LocalDate.now());
        Episode saved = episodeService.create(episode, "producer");

        // Admin can directly set status to FAILED or any status
        Episode overridden = episodeService.updateStatus(saved.getId(), EpisodeStatus.FAILED, "admin");
        assertEquals(EpisodeStatus.FAILED, overridden.getStatus());
    }

    @Test
    void searchEpisodes_filtersByTitleAndStatus() {
        Episode ep1 = new Episode("Spring Boot Guide", "Desc", "http://a.mp3", LocalDate.now());
        Episode ep2 = new Episode("DevOps Automation", "Desc", "http://b.mp3", LocalDate.now());
        episodeService.create(ep1, "producer");
        episodeService.create(ep2, "producer");

        List<Episode> results = episodeService.search("Spring", null, null, null);
        assertEquals(1, results.size());
        assertEquals("Spring Boot Guide", results.get(0).getTitle());
    }

    @Test
    void dashboardSummary_returnsAccurateCounts() {
        Episode ep1 = new Episode("Ep 1", "Desc", "http://a.mp3", LocalDate.now());
        Episode ep2 = new Episode("Ep 2", "Desc", "http://b.mp3", LocalDate.now());
        episodeService.create(ep1, "producer");
        Episode saved2 = episodeService.create(ep2, "producer");
        episodeService.updateStatus(saved2.getId(), EpisodeStatus.VALIDATED, "producer");

        Map<String, Object> summary = episodeService.getDashboardSummary();
        assertEquals(1L, summary.get("DRAFT"));
        assertEquals(1L, summary.get("VALIDATED"));
        assertEquals(2L, summary.get("TOTAL"));
    }

    @Test
    void auditLog_recordsActions() {
        Episode ep = new Episode("Ep Audit", "Desc", "http://a.mp3", LocalDate.now());
        Episode saved = episodeService.create(ep, "producer");
        episodeService.updateStatus(saved.getId(), EpisodeStatus.VALIDATED, "producer");

        List<AuditLog> logs = episodeService.getAuditLogs(saved.getId());
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().anyMatch(l -> l.getAction().contains("STATUS_CHANGE")));
    }

    @Test
    void saveAudioFile_savesUploadedFileAndReturnsPath() {
        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "audioFile", "test_episode.mp3", "audio/mpeg", "dummy audio content".getBytes());

        String savedUrl = episodeService.saveAudioFile(mockFile);
        assertNotNull(savedUrl);
        assertTrue(savedUrl.startsWith("/audio/"));
        assertTrue(savedUrl.endsWith("_test_episode.mp3"));
    }
}