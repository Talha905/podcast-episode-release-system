package com.podcastrelease.controller;

import com.podcastrelease.model.PlatformRole;
import com.podcastrelease.model.PodcastShow;
import com.podcastrelease.model.PodcastShowMember;
import com.podcastrelease.model.User;
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
        model.addAttribute("roles", PlatformRole.values());
        return "users";
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam(required = false) PlatformRole platformRole,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setPlatformRole(platformRole);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Updated platform role for user '" + user.getUsername() + "' to " + platformRole);
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
}
