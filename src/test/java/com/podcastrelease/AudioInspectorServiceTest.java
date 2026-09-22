package com.podcastrelease;

import com.podcastrelease.model.Episode;
import com.podcastrelease.service.AudioInspectorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AudioInspectorServiceTest {

    @Autowired
    private AudioInspectorService audioInspectorService;

    @Test
    void inspectAndPopulate_calculatesFallbackMetadataForRemoteUrl() {
        Episode episode = new Episode();
        episode.setAudioFileUrl("https://example.com/audio/sample_podcast.mp3");

        audioInspectorService.inspectAndPopulate(episode);

        assertNotNull(episode.getDurationSeconds());
        assertTrue(episode.getDurationSeconds() > 0);
        assertNotNull(episode.getFormattedDuration());
        assertNotNull(episode.getFileSizeBytes());
        assertTrue(episode.getFileSizeBytes() > 0);
    }

    @Test
    void inspectAndPopulate_handlesNullOrEmptyAudioUrlGracefully() {
        Episode episode = new Episode();
        episode.setAudioFileUrl(null);

        audioInspectorService.inspectAndPopulate(episode);

        assertNull(episode.getDurationSeconds());
        assertNull(episode.getFormattedDuration());
        assertNull(episode.getFileSizeBytes());
    }

    @Test
    void formatDuration_formatsSecondsToHms() {
        assertEquals("01:05", audioInspectorService.formatDuration(65));
        assertEquals("01:05:10", audioInspectorService.formatDuration(3910));
        assertEquals("00:00", audioInspectorService.formatDuration(0));
    }
}
