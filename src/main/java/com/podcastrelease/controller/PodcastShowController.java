package com.podcastrelease.controller;

import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.repository.PodcastShowRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shows")
public class PodcastShowController {

    private final PodcastShowRepository podcastShowRepository;

    public PodcastShowController(PodcastShowRepository podcastShowRepository) {
        this.podcastShowRepository = podcastShowRepository;
    }

    @GetMapping
    public String listShows(Model model) {
        model.addAttribute("shows", podcastShowRepository.findAll());
        model.addAttribute("newShow", new PodcastShow());
        return "shows";
    }

    @PostMapping("/create")
    public String createShow(@ModelAttribute PodcastShow show, RedirectAttributes redirectAttributes) {
        if (show.getTitle() == null || show.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Show title is required.");
            return "redirect:/shows";
        }
        podcastShowRepository.save(show);
        redirectAttributes.addFlashAttribute("successMessage", "Podcast show '" + show.getTitle() + "' created successfully!");
        return "redirect:/shows";
    }
}
