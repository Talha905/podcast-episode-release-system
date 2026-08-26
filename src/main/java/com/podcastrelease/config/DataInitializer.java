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
        if (userRepository.count() == 0) {
            User producer = userRepository.save(new User("producer", passwordEncoder.encode("password123"), UserRole.PRODUCER));
            User host = userRepository.save(new User("host", passwordEncoder.encode("password123"), UserRole.HOST));
            User admin = userRepository.save(new User("admin", passwordEncoder.encode("password123"), UserRole.ADMIN));

            if (episodeRepository.count() == 0) {
                Episode ep1 = new Episode("Episode 1: Tech Trends 2026", "Discussion on emerging AI tech trends.", "https://storage.podcast.com/audio/ep1.mp3", LocalDate.now().plusDays(2));
                ep1.setCreatedBy(producer);
                ep1.setStatus(EpisodeStatus.DRAFT);
                episodeRepository.save(ep1);

                Episode ep2 = new Episode("Episode 2: Deep Dive into DevOps", "An in-depth look into Jenkins CI/CD automation.", "https://storage.podcast.com/audio/ep2.mp3", LocalDate.now().plusDays(5));
                ep2.setCreatedBy(producer);
                ep2.setStatus(EpisodeStatus.VALIDATED);
                episodeRepository.save(ep2);

                Episode ep3 = new Episode("Episode 3: Cloud Infrastructure Best Practices", "Exploring containerization and Kubernetes orchestration.", "https://storage.podcast.com/audio/ep3.mp3", LocalDate.now().minusDays(3));
                ep3.setCreatedBy(producer);
                ep3.setStatus(EpisodeStatus.PUBLISHED);
                episodeRepository.save(ep3);
            }
        }
    }
}
