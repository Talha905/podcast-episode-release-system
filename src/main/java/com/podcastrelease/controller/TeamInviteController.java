package com.podcastrelease.controller;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.NotificationRepository;
import com.podcastrelease.repository.TeamInviteRepository;
import com.podcastrelease.repository.TeamMembershipRepository;
import com.podcastrelease.repository.UserRepository;
import com.podcastrelease.service.TeamSecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class TeamInviteController {

    private final TeamInviteRepository teamInviteRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;
    private final TeamSecurityService teamSecurityService;
    private final NotificationRepository notificationRepository;

    public TeamInviteController(TeamInviteRepository teamInviteRepository,
                                TeamMembershipRepository teamMembershipRepository,
                                UserRepository userRepository,
                                TeamSecurityService teamSecurityService,
                                NotificationRepository notificationRepository) {
        this.teamInviteRepository = teamInviteRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.userRepository = userRepository;
        this.teamSecurityService = teamSecurityService;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/api/teams/{id}/invites")
    @Transactional
    public ResponseEntity<Map<String, Object>> createInvite(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body,
                                                            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);
        teamSecurityService.verifyTeamOwner(id, currentUser);

        String email = body.get("email");
        String roleStr = body.get("role");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        TeamRole role = TeamRole.CREATOR;
        if (roleStr != null) {
            try {
                role = TeamRole.valueOf(roleStr.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        String token = UUID.randomUUID().toString();
        TeamInvite invite = new TeamInvite(team, email.trim(), role, token, currentUser);
        teamInviteRepository.save(invite);

        userRepository.findByEmail(email.trim()).ifPresent(recipient -> {
            Notification notification = new Notification(
                    recipient,
                    "Team Invitation",
                    "You have been invited to join team '" + team.getName() + "' as " + invite.getRole(),
                    "TEAM_INVITE",
                    "/invites/" + token + "/accept"
            );
            notificationRepository.save(notification);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("id", invite.getId());
        response.put("teamId", team.getId());
        response.put("email", invite.getEmail());
        response.put("role", invite.getRole());
        response.put("token", invite.getToken());
        response.put("expiresAt", invite.getExpiresAt());
        response.put("acceptUrl", "/api/invites/" + invite.getToken() + "/accept");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/invites/{token}/decline")
    @Transactional
    public ResponseEntity<Map<String, Object>> declineInvite(@PathVariable String token) {
        TeamInvite invite = teamInviteRepository.findByToken(token).orElse(null);
        if (invite != null) {
            teamInviteRepository.delete(invite);
            return ResponseEntity.ok(Map.of("message", "Invitation declined successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired invitation token"));
    }

    @GetMapping("/api/teams/{id}/invites")
    public ResponseEntity<List<Map<String, Object>>> getPendingInvites(@PathVariable Long id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);

        List<TeamInvite> invites = teamInviteRepository.findByTeamId(team.getId());
        List<Map<String, Object>> result = invites.stream()
                .filter(i -> !i.isAccepted() && i.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("email", i.getEmail());
                    map.put("role", i.getRole());
                    map.put("token", i.getToken());
                    map.put("expiresAt", i.getExpiresAt());
                    return map;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/invites/{token}/accept")
    @Transactional
    public ResponseEntity<Map<String, Object>> acceptInvite(@PathVariable String token, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required to accept invite"));
        }

        TeamInvite invite = teamInviteRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        if (invite.isAccepted()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invite has already been accepted"));
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invite token has expired"));
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElseThrow();

        // Create or update membership
        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(invite.getTeam().getId(), currentUser.getId())
                .orElse(new TeamMembership(invite.getTeam(), currentUser, invite.getRole()));

        membership.setRole(invite.getRole());
        teamMembershipRepository.save(membership);

        invite.setAccepted(true);
        teamInviteRepository.save(invite);

        return ResponseEntity.ok(Map.of(
                "message", "Successfully joined team '" + invite.getTeam().getName() + "' as " + invite.getRole(),
                "teamId", invite.getTeam().getId(),
                "role", invite.getRole()
        ));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
