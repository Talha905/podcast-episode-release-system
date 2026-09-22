package com.podcastrelease;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.PodcastShowRepository;
import com.podcastrelease.service.RssFeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RssFeedServiceTest {

    @Autowired
    private RssFeedService rssFeedService;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PodcastShowRepository podcastShowRepository;

    @BeforeEach
    void setUp() {
        episodeRepository.deleteAll();
        podcastShowRepository.deleteAll();
    }

    @Test
    void generateMainRssFeed_returnsValidXmlWithPublishedEpisodes() {
        PodcastShow show = podcastShowRepository.save(new PodcastShow("Main Show", "main-show", "Main Description", "Tech", "Host Name", "host@test.com", "http://image.jpg"));

        Episode ep1 = new Episode("Ep 1 Published", "Desc 1", "http://audio.com/ep1.mp3", LocalDate.now().minusDays(1));
        ep1.setStatus(EpisodeStatus.PUBLISHED);
        ep1.setPodcastShow(show);
        ep1.setDurationSeconds(240);
        ep1.setFormattedDuration("00:04:00");
        ep1.setFileSizeBytes(102400L);
        episodeRepository.save(ep1);

        Episode ep2 = new Episode("Ep 2 Draft", "Desc 2", "http://audio.com/ep2.mp3", LocalDate.now());
        ep2.setStatus(EpisodeStatus.DRAFT);
        ep2.setPodcastShow(show);
        episodeRepository.save(ep2);

        String xml = rssFeedService.generateRssFeed(null, "http://localhost:8083");

        assertNotNull(xml);
        assertTrue(xml.contains("<rss version=\"2.0\""));
        assertTrue(xml.contains("xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\""));
        assertTrue(xml.contains("Ep 1 Published"));
        assertFalse(xml.contains("Ep 2 Draft"), "Draft episodes must not be present in public RSS feed");
        assertTrue(xml.contains("<itunes:duration>00:04:00</itunes:duration>"));
        assertTrue(xml.contains("enclosure url=\"http://audio.com/ep1.mp3\""));
    }

    @Test
    void generateShowRssFeed_returnsXmlFilteredByShow() {
        PodcastShow show1 = podcastShowRepository.save(new PodcastShow("Show One", "show-one", "Desc 1", "Tech", "Host 1", "h1@test.com", "http://img1.jpg"));
        PodcastShow show2 = podcastShowRepository.save(new PodcastShow("Show Two", "show-two", "Desc 2", "DevOps", "Host 2", "h2@test.com", "http://img2.jpg"));

        Episode ep1 = new Episode("Show 1 Ep", "Desc", "http://audio.com/ep1.mp3", LocalDate.now());
        ep1.setStatus(EpisodeStatus.PUBLISHED);
        ep1.setPodcastShow(show1);
        episodeRepository.save(ep1);

        Episode ep2 = new Episode("Show 2 Ep", "Desc", "http://audio.com/ep2.mp3", LocalDate.now());
        ep2.setStatus(EpisodeStatus.PUBLISHED);
        ep2.setPodcastShow(show2);
        episodeRepository.save(ep2);

        String xml1 = rssFeedService.generateRssFeed(show1.getId(), "http://localhost:8083");
        assertTrue(xml1.contains("Show 1 Ep"));
        assertFalse(xml1.contains("Show 2 Ep"));
    }
}
