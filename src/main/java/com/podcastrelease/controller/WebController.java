package com.podcastrelease.controller;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.UserRepository;
import com.podcastrelease.service.EpisodeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Controller
public class WebController {

    private final EpisodeService episodeService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.podcastrelease.repository.PodcastShowRepository podcastShowRepository;

    public WebController(EpisodeService episodeService,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         com.podcastrelease.repository.PodcastShowRepository podcastShowRepository) {
        this.episodeService = episodeService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.podcastShowRepository = podcastShowRepository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("roles", com.podcastrelease.model.UserRole.values());
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(defaultValue = "PRODUCER") com.podcastrelease.model.UserRole role,
            RedirectAttributes redirectAttributes) {

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username and password are required.");
            return "redirect:/signup";
        }

        if (userRepository.existsByUsername(username.trim())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username '" + username.trim() + "' is already taken. Please choose another.");
            return "redirect:/signup";
        }

        User user = new User(username.trim(), passwordEncoder.encode(password), role);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Account created successfully for '" + username.trim() + "'! Please sign in below.");
        return "redirect:/login";
    }

    @GetMapping({"/", "/episodes", "/dashboard"})
    public String dashboard(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) EpisodeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model,
            Authentication authentication) {

        List<Episode> episodes = episodeService.search(title, status, from, to);
        model.addAttribute("episodes", episodes);
        model.addAttribute("summary", episodeService.getDashboardSummary());
        model.addAttribute("titleFilter", title);
        model.addAttribute("statusFilter", status);
        model.addAttribute("fromDateFilter", from);
        model.addAttribute("toDateFilter", to);
        model.addAttribute("statuses", EpisodeStatus.values());
        model.addAttribute("shows", podcastShowRepository.findAll());

        if (authentication != null) {
            User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            model.addAttribute("currentUser", currentUser);
        }

        return "episodes";
    }

    @GetMapping("/episodes/new")
    public String newEpisodeForm(Model model) {
        model.addAttribute("episode", new Episode());
        model.addAttribute("shows", podcastShowRepository.findAll());
        return "create";
    }

    @PostMapping("/episodes/create")
    public String createEpisode(
            @ModelAttribute Episode episode,
            @RequestParam(value = "podcastShowId", required = false) Long podcastShowId,
            @RequestParam(value = "audioFile", required = false) org.springframework.web.multipart.MultipartFile audioFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            if (podcastShowId != null) {
                podcastShowRepository.findById(podcastShowId).ifPresent(episode::setPodcastShow);
            }
            if (audioFile != null && !audioFile.isEmpty()) {
                String uploadedUrl = episodeService.saveAudioFile(audioFile);
                episode.setAudioFileUrl(uploadedUrl);
            }
            String username = authentication != null ? authentication.getName() : null;
            Episode saved = episodeService.create(episode, username);
            redirectAttributes.addFlashAttribute("successMessage", "Episode '" + saved.getTitle() + "' created successfully!");
            return "redirect:/episodes/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/episodes/new";
        }
    }

    @GetMapping("/episodes/{id}")
    public String episodeDetail(@PathVariable Long id, Model model, Authentication authentication) {
        Episode episode = episodeService.findById(id);
        model.addAttribute("episode", episode);
        model.addAttribute("auditLogs", episodeService.getAuditLogs(id));
        model.addAttribute("statuses", EpisodeStatus.values());

        if (authentication != null) {
            User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            model.addAttribute("currentUser", currentUser);
        }

        return "detail";
    }

    @GetMapping("/episodes/{id}/edit")
    public String editEpisodeForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Episode episode = episodeService.findById(id);
        if (episode.getStatus() == EpisodeStatus.PUBLISHED || episode.getStatus() == EpisodeStatus.FAILED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot edit episode in " + episode.getStatus() + " status.");
            return "redirect:/episodes/" + id;
        }
        model.addAttribute("episode", episode);
        model.addAttribute("shows", podcastShowRepository.findAll());
        return "edit";
    }

    @PostMapping("/episodes/{id}/edit")
    public String updateEpisode(
            @PathVariable Long id,
            @ModelAttribute Episode updatedEpisode,
            @RequestParam(value = "podcastShowId", required = false) Long podcastShowId,
            @RequestParam(value = "audioFile", required = false) org.springframework.web.multipart.MultipartFile audioFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            if (podcastShowId != null) {
                updatedEpisode.setPodcastShow(podcastShowRepository.findById(podcastShowId).orElse(null));
            }
            if (audioFile != null && !audioFile.isEmpty()) {
                String uploadedUrl = episodeService.saveAudioFile(audioFile);
                updatedEpisode.setAudioFileUrl(uploadedUrl);
            }
            String username = authentication != null ? authentication.getName() : null;
            episodeService.update(id, updatedEpisode, username);
            redirectAttributes.addFlashAttribute("successMessage", "Episode metadata updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/episodes/" + id;
    }

    @PostMapping("/episodes/{id}/status")
    public String transitionStatus(
            @PathVariable Long id,
            @RequestParam EpisodeStatus status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String username = authentication != null ? authentication.getName() : null;
            episodeService.updateStatus(id, status, username);
            redirectAttributes.addFlashAttribute("successMessage", "Episode status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/episodes/" + id;
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Uploaded audio file exceeds maximum allowed limit (100MB). Please choose a smaller file.");
        return "redirect:/episodes";
    }
}
