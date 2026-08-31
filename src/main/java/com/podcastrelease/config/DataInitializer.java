package com.podcastrelease.config;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.User;
import com.podcastrelease.model.UserRole;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EpisodeRepository episodeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, EpisodeRepository episodeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.episodeRepository = episodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureSampleAudioFilesExist();

        if (userRepository.count() == 0) {
            User producer = userRepository.save(new User("producer", passwordEncoder.encode("password123"), UserRole.PRODUCER));
            User host = userRepository.save(new User("host", passwordEncoder.encode("password123"), UserRole.HOST));
            User admin = userRepository.save(new User("admin", passwordEncoder.encode("password123"), UserRole.ADMIN));

            if (episodeRepository.count() == 0) {
                Episode ep1 = new Episode("Episode 1: Tech Trends 2026", "Discussion on emerging AI tech trends.", "/audio/ep1.mp3", LocalDate.now().plusDays(2));
                ep1.setCreatedBy(producer);
                ep1.setStatus(EpisodeStatus.DRAFT);
                episodeRepository.save(ep1);

                Episode ep2 = new Episode("Episode 2: Deep Dive into DevOps", "An in-depth look into Jenkins CI/CD automation.", "/audio/ep2.mp3", LocalDate.now().plusDays(5));
                ep2.setCreatedBy(producer);
                ep2.setStatus(EpisodeStatus.VALIDATED);
                episodeRepository.save(ep2);

                Episode ep3 = new Episode("Episode 3: Cloud Infrastructure Best Practices", "Exploring containerization and Kubernetes orchestration.", "/audio/ep3.mp3", LocalDate.now().minusDays(3));
                ep3.setCreatedBy(producer);
                ep3.setStatus(EpisodeStatus.PUBLISHED);
                episodeRepository.save(ep3);
            }
        }
    }

    private void ensureSampleAudioFilesExist() {
        File audioDir = new File("src/main/resources/static/audio");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        File targetDir = new File("target/classes/static/audio");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        String[] sampleFiles = new String[]{"ep1.mp3", "ep2.mp3", "ep3.mp3"};
        for (int i = 0; i < sampleFiles.length; i++) {
            String fileName = sampleFiles[i];
            File file1 = new File(audioDir, fileName);
            File file2 = new File(targetDir, fileName);
            double frequency = 300.0 + (i * 150.0);
            if (!file1.exists()) {
                generateToneAudioFile(file1, frequency);
            }
            if (!file2.exists()) {
                generateToneAudioFile(file2, frequency);
            }
        }
    }

    private void generateToneAudioFile(File file, double frequency) {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            int durationSeconds = 3;
            byte[] pcm = new byte[44100 * 2 * durationSeconds];
            for (int i = 0; i < pcm.length / 2; i++) {
                double angle = 2.0 * Math.PI * frequency * i / 44100.0;
                short sample = (short) (Math.sin(angle) * 8000);
                pcm[i * 2] = (byte) (sample & 0xff);
                pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(pcm);
            AudioInputStream ais = new AudioInputStream(bais, format, pcm.length / 2);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        } catch (Exception ignored) {}
    }
}
