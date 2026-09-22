package com.podcastrelease.service;

import com.podcastrelease.model.Episode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;

@Service
public class AudioInspectorService {

    public void inspectAndPopulate(Episode episode) {
        if (episode == null || episode.getAudioFileUrl() == null || episode.getAudioFileUrl().trim().isEmpty()) {
            return;
        }
        long sizeBytes = 250_000L;
        int durationSecs = 240;

        if (episode.getAudioFileUrl().startsWith("/audio/")) {
            File file = new File("src/main/resources/static" + episode.getAudioFileUrl());
            if (!file.exists()) {
                file = new File("target/classes/static" + episode.getAudioFileUrl());
            }
            if (!file.exists()) {
                file = new File("uploads" + episode.getAudioFileUrl());
            }
            if (file.exists()) {
                sizeBytes = file.length();
                durationSecs = estimateOrCalculateDuration(file.toPath(), sizeBytes);
            }
        }

        episode.setFileSizeBytes(sizeBytes);
        episode.setDurationSeconds(durationSecs);
        episode.setFormattedDuration(formatSeconds(durationSecs));
    }

    public AudioMetadata inspect(MultipartFile file, Path savedFilePath) {
        long sizeBytes = file != null ? file.getSize() : 0L;
        if (sizeBytes == 0L && savedFilePath != null && savedFilePath.toFile().exists()) {
            sizeBytes = savedFilePath.toFile().length();
        }

        int durationSeconds = estimateOrCalculateDuration(savedFilePath, sizeBytes);
        String formattedDuration = formatSeconds(durationSeconds);

        return new AudioMetadata(sizeBytes, durationSeconds, formattedDuration);
    }

    private int estimateOrCalculateDuration(Path path, long sizeBytes) {
        if (path != null && path.toFile().exists()) {
            try {
                AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(path.toFile());
                Map<String, Object> properties = baseFileFormat.properties();
                if (properties.containsKey("duration")) {
                    long microseconds = (Long) properties.get("duration");
                    return (int) (microseconds / 1_000_000);
                }
            } catch (Exception ignored) {}
        }

        // Fallback: estimate 128 kbps audio stream duration based on file size
        if (sizeBytes > 0) {
            int bytesPerSecond = (128 * 1024) / 8; // ~16,000 bytes/sec
            return Math.max(1, (int) (sizeBytes / bytesPerSecond));
        }

        return 240; // Default 4 minutes fallback
    }

    public String formatSeconds(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public String formatDuration(int totalSeconds) {
        return formatSeconds(totalSeconds);
    }

    public record AudioMetadata(long sizeBytes, int durationSeconds, String formattedDuration) {}
}
