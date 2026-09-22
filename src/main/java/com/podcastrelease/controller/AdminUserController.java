package com.podcastrelease.controller;

import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.model.PodcastShowMember;
import com.podcastrelease.model.User;
import com.podcastrelease.model.UserRole;
import com.podcastrelease.repository.PodcastShowMemberRepository;
import com.podcastrelease.repository.PodcastShowRepository;
import com.podcastrelease.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PodcastShowRepository podcastShowRepository;
    private final PodcastShowMemberRepository podcastShowMemberRepository;

    public AdminUserController(UserRepository userRepository,
                               PodcastShowRepository podcastShowRepository,
                               PodcastShowMemberRepository podcastShowMemberRepository) {
        this.userRepository = userRepository;
        this.podcastShowRepository = podcastShowRepository;
        this.podcastShowMemberRepository = podcastShowMemberRepository;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        List<PodcastShow> shows = podcastShowRepository.findAll();
        List<PodcastShowMember> members = podcastShowMemberRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("shows", shows);
        model.addAttribute("members", members);
        model.addAttribute("roles", UserRole.values());
        return "users";
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam UserRole role,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setRole(role);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Updated role for user '" + user.getUsername() + "' to " + role);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        String statusStr = user.isEnabled() ? "ENABLED (Active)" : "DISABLED (Inactive)";
        redirectAttributes.addFlashAttribute("successMessage", "User '" + user.getUsername() + "' is now " + statusStr);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/assign-show")
    public String assignShowToUser(@PathVariable Long id,
                                   @RequestParam Long showId,
                                   @RequestParam UserRole roleInShow,
                                   RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        PodcastShow show = podcastShowRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));

        if (!podcastShowMemberRepository.existsByPodcastShowIdAndUserId(showId, id)) {
            podcastShowMemberRepository.save(new PodcastShowMember(show, user, roleInShow));
            redirectAttributes.addFlashAttribute("successMessage", "Assigned '" + user.getUsername() + "' to show '" + show.getTitle() + "' as " + roleInShow);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "User '" + user.getUsername() + "' is already assigned to show '" + show.getTitle() + "'");
        }
        return "redirect:/admin/users";
    }
}
