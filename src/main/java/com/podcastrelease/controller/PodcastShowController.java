package com.podcastrelease.controller;

import com.podcastrelease.exception.TeamNotFoundException;
import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.model.Team;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.PodcastShowRepository;
import com.podcastrelease.repository.TeamRepository;
import com.podcastrelease.repository.UserRepository;
import com.podcastrelease.service.TeamSecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shows")
public class PodcastShowController {

    private final PodcastShowRepository podcastShowRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamSecurityService teamSecurityService;

    public PodcastShowController(PodcastShowRepository podcastShowRepository,
                                 TeamRepository teamRepository,
                                 UserRepository userRepository,
                                 TeamSecurityService teamSecurityService) {
        this.podcastShowRepository = podcastShowRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamSecurityService = teamSecurityService;
    }

    @GetMapping
    public String listShows(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        List<PodcastShow> shows;
        if (currentUser != null && currentUser.getPlatformRole() != null &&
           (currentUser.getPlatformRole() == com.podcastrelease.model.PlatformRole.ADMIN || currentUser.getPlatformRole() == com.podcastrelease.model.PlatformRole.SYSTEM)) {
            shows = podcastShowRepository.findAll();
        } else if (currentUser != null) {
            List<Team> teams = teamRepository.findByUserId(currentUser.getId());
            shows = teams.stream()
                    .flatMap(t -> podcastShowRepository.findByTeamId(t.getId()).stream())
                    .distinct()
                    .toList();
        } else {
            shows = List.of();
        }

        model.addAttribute("shows", shows);
        model.addAttribute("teams", currentUser != null ? teamRepository.findByUserId(currentUser.getId()) : List.of());
        model.addAttribute("newShow", new PodcastShow());
        return "shows";
    }

    @GetMapping("/api/shows/{id}")
    @ResponseBody
    public ResponseEntity<PodcastShow> getShowApi(@PathVariable Long id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        PodcastShow show = podcastShowRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException("Show not found with id: " + id));

        if (show.getTeam() != null) {
            teamSecurityService.verifyTeamMember(show.getTeam().getId(), currentUser);
        }

        return ResponseEntity.ok(show);
    }

    @PostMapping("/create")
    public String createShow(@ModelAttribute PodcastShow show,
                             @RequestParam(value = "teamId", required = false) Long teamId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        if (show.getTitle() == null || show.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Show title is required.");
            return "redirect:/shows";
        }

        User currentUser = getCurrentUser(authentication);
        if (teamId != null) {
            Team team = teamSecurityService.verifyTeamMember(teamId, currentUser);
            show.setTeam(team);
        } else if (currentUser != null) {
            List<Team> teams = teamRepository.findByUserId(currentUser.getId());
            if (!teams.isEmpty()) {
                show.setTeam(teams.get(0));
            }
        }

        podcastShowRepository.save(show);
        redirectAttributes.addFlashAttribute("successMessage", "Podcast show '" + show.getTitle() + "' created successfully!");
        return "redirect:/shows";
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
