package com.podcastrelease.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/api/health")
    public String health() {
        return "Podcast Episode Release System is running.";
    }
}