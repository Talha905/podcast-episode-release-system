package com.podcastrelease.config;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.*;
import com.podcastrelease.service.AudioInspectorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EpisodeRepository episodeRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final PodcastShowRepository podcastShowRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final WebhookConfigRepository webhookConfigRepository;
    private final AudioInspectorService audioInspectorService;

    public DataInitializer(UserRepository userRepository,
                           EpisodeRepository episodeRepository,
                           AuditLogRepository auditLogRepository,
                           PasswordEncoder passwordEncoder,
                           PodcastShowRepository podcastShowRepository,
                           PlatformAccountRepository platformAccountRepository,
                           WebhookConfigRepository webhookConfigRepository,
                           AudioInspectorService audioInspectorService) {
        this.userRepository = userRepository;
        this.episodeRepository = episodeRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.podcastShowRepository = podcastShowRepository;
        this.platformAccountRepository = platformAccountRepository;
        this.webhookConfigRepository = webhookConfigRepository;
        this.audioInspectorService = audioInspectorService;
    }

    @Override
    public void run(String... args) {
        ensureSampleAudioFilesExist();

        // Seed Podcast Shows
        PodcastShow show1 = null;
        PodcastShow show2 = null;
        if (podcastShowRepository.count() == 0) {
            show1 = podcastShowRepository.save(new PodcastShow(
                    "Tech & AI Insights",
                    "tech-ai-insights",
                    "Deep dives into AI, software engineering, and high-throughput system design.",
                    "Technology",
                    "Talha Patrawala",
                    "producer@podcastrelease.com",
                    "https://podcastrelease.com/artwork/tech.png"
            ));

            show2 = podcastShowRepository.save(new PodcastShow(
                    "DevOps Uncut",
                    "devops-uncut",
                    "Raw and unfiltered discussions on CI/CD pipelines, Kubernetes, cloud native security, and infrastructure automation.",
                    "Technology",
                    "DevOps Engineering Team",
                    "devops@podcastrelease.com",
                    "https://podcastrelease.com/artwork/devops.png"
            ));
        } else {
            show1 = podcastShowRepository.findBySlug("tech-ai-insights").orElse(null);
            show2 = podcastShowRepository.findBySlug("devops-uncut").orElse(null);
        }

        // Seed Platform Accounts
        if (platformAccountRepository.count() == 0) {
            platformAccountRepository.save(new PlatformAccount(
                    "YouTube Podcasts Channel",
                    PlatformAccount.PlatformType.YOUTUBE,
                    "https://upload.youtube.com/my_podcast_channel",
                    "sample_yt_oauth_token_sec_key",
                    true
            ));

            platformAccountRepository.save(new PlatformAccount(
                    "Buzzsprout Hosting Account",
                    PlatformAccount.PlatformType.BUZZSPROUT,
                    "https://api.buzzsprout.com/v1/episodes",
                    "bz_api_key_88392019481029",
                    true
            ));
        }

        // Seed Webhook Configs
        if (webhookConfigRepository.count() == 0) {
            webhookConfigRepository.save(new WebhookConfig(
                    "Slack Release Notifications",
                    WebhookConfig.WebhookType.SLACK,
                    "https://example.com/webhooks/slack-release-alerts",
                    true
            ));

            webhookConfigRepository.save(new WebhookConfig(
                    "Discord Alert Channel",
                    WebhookConfig.WebhookType.DISCORD,
                    "https://example.com/webhooks/discord-release-alerts",
                    true
            ));
        }

        if (userRepository.count() == 0) {
            User producer = userRepository.save(new User("producer", passwordEncoder.encode("password123"), UserRole.PRODUCER));
            User host = userRepository.save(new User("host", passwordEncoder.encode("password123"), UserRole.HOST));
            User admin = userRepository.save(new User("admin", passwordEncoder.encode("password123"), UserRole.ADMIN));

            if (episodeRepository.count() == 0) {
                // Episode 101 - Published
                Episode ep1 = new Episode(
                        "Ep 101: The Future of Generative AI in Production",
                        "In this episode, we sit down with leading AI engineers to discuss multi-agent architectures, LLM orchestration, and low-latency inference strategies for high-throughput enterprise systems.",
                        "/audio/ep1.mp3",
                        LocalDate.now().minusDays(10)
                );
                ep1.setCreatedBy(producer);
                ep1.setStatus(EpisodeStatus.PUBLISHED);
                ep1.setPodcastShow(show1);
                ep1 = saveAndInspect(ep1);
                auditLogRepository.save(new AuditLog(ep1.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));
                auditLogRepository.save(new AuditLog(ep1.getId(), "STATUS_CHANGE: DRAFT -> VALIDATED", producer));
                auditLogRepository.save(new AuditLog(ep1.getId(), "STATUS_CHANGE: VALIDATED -> PUBLISHED", host));

                // Episode 102 - Published
                Episode ep2 = new Episode(
                        "Ep 102: Zero Trust Security in Cloud Native Environments",
                        "Exploring identity-aware proxies, mutual TLS, fine-grained RBAC authorization policies, and automated secret rotation across production Kubernetes clusters.",
                        "/audio/ep2.mp3",
                        LocalDate.now().minusDays(5)
                );
                ep2.setCreatedBy(producer);
                ep2.setStatus(EpisodeStatus.PUBLISHED);
                ep2.setPodcastShow(show1);
                ep2 = saveAndInspect(ep2);
                auditLogRepository.save(new AuditLog(ep2.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));
                auditLogRepository.save(new AuditLog(ep2.getId(), "STATUS_CHANGE: DRAFT -> VALIDATED", producer));
                auditLogRepository.save(new AuditLog(ep2.getId(), "STATUS_CHANGE: VALIDATED -> PUBLISHED", admin));

                // Episode 103 - Published
                Episode ep3 = new Episode(
                        "Ep 103: Building Autonomous CI/CD Pipelines with Jenkins",
                        "A step-by-step masterclass on parameterizing Jenkins pipelines, containerizing Spring Boot microservices, running Selenium WebDriver test gates, and automating rollback triggers.",
                        "/audio/ep3.mp3",
                        LocalDate.now().minusDays(2)
                );
                ep3.setCreatedBy(producer);
                ep3.setStatus(EpisodeStatus.PUBLISHED);
                ep3.setPodcastShow(show2);
                ep3 = saveAndInspect(ep3);
                auditLogRepository.save(new AuditLog(ep3.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));
                auditLogRepository.save(new AuditLog(ep3.getId(), "STATUS_CHANGE: DRAFT -> VALIDATED", producer));
                auditLogRepository.save(new AuditLog(ep3.getId(), "STATUS_CHANGE: VALIDATED -> PUBLISHED", producer));

                // Episode 104 - Validated
                Episode ep4 = new Episode(
                        "Ep 104: Scaling Distributed Databases under High Concurrency",
                        "Lessons learned from managing global database clusters during peak traffic spikes, setting up read replicas, connection pooling, and sharding strategies.",
                        "/audio/ep4.mp3",
                        LocalDate.now().plusDays(3)
                );
                ep4.setCreatedBy(producer);
                ep4.setStatus(EpisodeStatus.VALIDATED);
                ep4.setPodcastShow(show1);
                ep4 = saveAndInspect(ep4);
                auditLogRepository.save(new AuditLog(ep4.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));
                auditLogRepository.save(new AuditLog(ep4.getId(), "STATUS_CHANGE: DRAFT -> VALIDATED", producer));

                // Episode 105 - Validated
                Episode ep5 = new Episode(
                        "Ep 105: Designing Accessible Design Systems for Enterprise Apps",
                        "How design tokens, accessible ARIA components, and strict UI design guidelines speed up product development across cross-functional frontend teams.",
                        "/audio/ep5.mp3",
                        LocalDate.now().plusDays(7)
                );
                ep5.setCreatedBy(producer);
                ep5.setStatus(EpisodeStatus.VALIDATED);
                ep5.setPodcastShow(show1);
                ep5 = saveAndInspect(ep5);
                auditLogRepository.save(new AuditLog(ep5.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));
                auditLogRepository.save(new AuditLog(ep5.getId(), "STATUS_CHANGE: DRAFT -> VALIDATED", producer));

                // Episode 106 - Draft
                Episode ep6 = new Episode(
                        "Ep 106: Microservices vs. Modular Monoliths in 2026",
                        "Deconstructing software architecture trends: when to split services, network latency overheads, and when a modular monolith is the superior operational choice.",
                        "/audio/ep6.mp3",
                        LocalDate.now().plusDays(12)
                );
                ep6.setCreatedBy(producer);
                ep6.setStatus(EpisodeStatus.DRAFT);
                ep6.setPodcastShow(show2);
                ep6 = saveAndInspect(ep6);
                auditLogRepository.save(new AuditLog(ep6.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));

                // Episode 107 - Draft
                Episode ep7 = new Episode(
                        "Ep 107: Automated Infrastructure Provisioning with Ansible & Puppet",
                        "Configuring multi-node server clusters idempotently using Ansible playbooks, Puppet manifests, and infrastructure-as-code principles.",
                        "/audio/ep7.mp3",
                        LocalDate.now().plusDays(15)
                );
                ep7.setCreatedBy(producer);
                ep7.setStatus(EpisodeStatus.DRAFT);
                ep7.setPodcastShow(show2);
                ep7 = saveAndInspect(ep7);
                auditLogRepository.save(new AuditLog(ep7.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));

                // Episode 108 - Failed
                Episode ep8 = new Episode(
                        "Ep 108: Post-Mortem: Overcoming Corrupted Metadata Release Pipelines",
                        "A deep-dive technical post-mortem into how automated pre-release validation caught malformed audio assets and prevented a broken release from going live.",
                        "/audio/ep8.mp3",
                        LocalDate.now().minusDays(1)
                );
                ep8.setCreatedBy(producer);
                ep8.setStatus(EpisodeStatus.FAILED);
                ep8.setPodcastShow(show2);
                ep8 = saveAndInspect(ep8);
                auditLogRepository.save(new AuditLog(ep8.getId(), "CREATE_EPISODE (Status: DRAFT)", producer));
                auditLogRepository.save(new AuditLog(ep8.getId(), "STATUS_CHANGE: DRAFT -> VALIDATED", producer));
                auditLogRepository.save(new AuditLog(ep8.getId(), "STATUS_CHANGE: VALIDATED -> FAILED (Validation Error)", admin));
            }
        }
    }

    private Episode saveAndInspect(Episode episode) {
        audioInspectorService.inspectAndPopulate(episode);
        return episodeRepository.save(episode);
    }

    private void ensureSampleAudioFilesExist() {
        Path audioDir = Paths.get("src/main/resources/static/audio").toAbsolutePath().normalize();
        Path targetDir = Paths.get("target/classes/static/audio").toAbsolutePath().normalize();
        Path uploadDir = Paths.get("uploads/audio").toAbsolutePath().normalize();

        try {
            Files.createDirectories(audioDir);
            Files.createDirectories(targetDir);
            Files.createDirectories(uploadDir);

            String[] sampleFiles = new String[]{"ep1.mp3", "ep2.mp3", "ep3.mp3", "ep4.mp3", "ep5.mp3", "ep6.mp3", "ep7.mp3", "ep8.mp3"};
            for (int i = 0; i < sampleFiles.length; i++) {
                String fileName = sampleFiles[i];
                File file1 = new File(audioDir.toFile(), fileName);
                File file2 = new File(targetDir.toFile(), fileName);
                File file3 = new File(uploadDir.toFile(), fileName);

                double frequency = 261.63 + (i * 40.0); // C4 scale progression
                if (!file1.exists()) generateToneAudioFile(file1, frequency);
                if (!file2.exists()) generateToneAudioFile(file2, frequency);
                if (!file3.exists()) generateToneAudioFile(file3, frequency);
            }
        } catch (Exception ignored) {}
    }

    private void generateToneAudioFile(File file, double frequency) {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            int durationSeconds = 4;
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
