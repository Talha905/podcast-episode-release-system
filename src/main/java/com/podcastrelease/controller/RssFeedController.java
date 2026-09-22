package com.podcastrelease.controller;

import com.podcastrelease.service.RssFeedService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RssFeedController {

    private final RssFeedService rssFeedService;

    public RssFeedController(RssFeedService rssFeedService) {
        this.rssFeedService = rssFeedService;
    }

    @GetMapping(value = {"/feed.xml", "/rss"}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getGlobalRssFeed(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        String xml = rssFeedService.generateRssFeed(null, baseUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8")
                .body(xml);
    }

    @GetMapping(value = "/shows/{showId}/feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getShowRssFeed(@PathVariable Long showId, HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        String xml = rssFeedService.generateRssFeed(showId, baseUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8")
                .body(xml);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if ((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
            return scheme + "://" + serverName;
        } else {
            return scheme + "://" + serverName + ":" + serverPort;
        }
    }
}
