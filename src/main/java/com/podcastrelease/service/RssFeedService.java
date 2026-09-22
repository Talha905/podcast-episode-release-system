package com.podcastrelease.service;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.PodcastShowRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RssFeedService {

    private final EpisodeRepository episodeRepository;
    private final PodcastShowRepository podcastShowRepository;

    public RssFeedService(EpisodeRepository episodeRepository, PodcastShowRepository podcastShowRepository) {
        this.episodeRepository = episodeRepository;
        this.podcastShowRepository = podcastShowRepository;
    }

    public String generateRssFeed(Long showId, String baseUrl) {
        PodcastShow show = showId != null ? podcastShowRepository.findById(showId).orElse(null) : null;

        String showTitle = show != null ? show.getTitle() : "Podcast Episode Release System";
        String showDescription = show != null ? show.getDescription() : "Automated distribution & release feed";
        String author = show != null ? show.getAuthor() : "Podcast Producer";
        String category = show != null ? show.getCategory() : "Technology";
        String language = show != null ? show.getLanguage() : "en-us";
        String coverImage = (show != null && show.getCoverImageUrl() != null) ? show.getCoverImageUrl() : baseUrl + "/images/cover.jpg";

        List<Episode> publishedEpisodes = (showId != null) ?
                episodeRepository.findByPodcastShowIdAndStatus(showId, EpisodeStatus.PUBLISHED) :
                episodeRepository.findByStatus(EpisodeStatus.PUBLISHED);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>").append(escapeXml(showTitle)).append("</title>\n");
        xml.append("    <link>").append(baseUrl).append("</link>\n");
        xml.append("    <description>").append(escapeXml(showDescription)).append("</description>\n");
        xml.append("    <language>").append(language).append("</language>\n");
        xml.append("    <itunes:author>").append(escapeXml(author)).append("</itunes:author>\n");
        xml.append("    <itunes:category text=\"").append(escapeXml(category)).append("\"/>\n");
        xml.append("    <itunes:image href=\"").append(coverImage).append("\"/>\n");

        DateTimeFormatter rfc822Formatter = DateTimeFormatter.RFC_1123_DATE_TIME;

        for (Episode ep : publishedEpisodes) {
            String audioUrl = ep.getAudioFileUrl();
            if (audioUrl != null && !audioUrl.startsWith("http://") && !audioUrl.startsWith("https://")) {
                audioUrl = baseUrl + (audioUrl.startsWith("/") ? "" : "/") + audioUrl;
            }

            String pubDateRfc = ep.getPublishDate().atStartOfDay(ZoneId.of("UTC")).format(rfc822Formatter);
            long length = ep.getFileSizeBytes() != null ? ep.getFileSizeBytes() : 352844L;
            String durationStr = ep.getFormattedDuration() != null ? ep.getFormattedDuration() : "00:04:00";

            xml.append("    <item>\n");
            xml.append("      <title>").append(escapeXml(ep.getTitle())).append("</title>\n");
            xml.append("      <description>").append(escapeXml(ep.getDescription())).append("</description>\n");
            xml.append("      <pubDate>").append(pubDateRfc).append("</pubDate>\n");
            xml.append("      <itunes:duration>").append(durationStr).append("</itunes:duration>\n");
            if (audioUrl != null) {
                xml.append("      <enclosure url=\"").append(escapeXml(audioUrl))
                   .append("\" length=\"").append(length)
                   .append("\" type=\"audio/mpeg\"/>\n");
            }
            xml.append("      <guid isPermaLink=\"false\">ep-").append(ep.getId()).append("</guid>\n");
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>");

        return xml.toString();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
