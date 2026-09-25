package com.podcastrelease.controller;

import com.podcastrelease.model.*;
import com.podcastrelease.repository.AuditLogRepository;
import com.podcastrelease.repository.EpisodeRepository;
import com.podcastrelease.repository.TeamMembershipRepository;
import com.podcastrelease.repository.TeamRepository;
import com.podcastrelease.repository.UserRepository;
import com.podcastrelease.service.TeamSecurityService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;
    private final TeamSecurityService teamSecurityService;
    private final EpisodeRepository episodeRepository;
    private final AuditLogRepository auditLogRepository;

    public TeamController(TeamRepository teamRepository,
                          TeamMembershipRepository teamMembershipRepository,
                          UserRepository userRepository,
                          TeamSecurityService teamSecurityService,
                          EpisodeRepository episodeRepository,
                          AuditLogRepository auditLogRepository) {
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.userRepository = userRepository;
        this.teamSecurityService = teamSecurityService;
        this.episodeRepository = episodeRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createTeam(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required"));
        }

        String name = body.get("name");
        String description = body.get("description");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team name is required"));
        }

        if (teamRepository.findByName(name.trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team name '" + name.trim() + "' is already taken"));
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Team team = teamRepository.save(new Team(name.trim(), description));

        // Creator becomes OWNER with allowSelfReview = false default
        TeamMembership ownerMembership = new TeamMembership(team, currentUser, TeamRole.OWNER, false);
        teamMembershipRepository.save(ownerMembership);

        Map<String, Object> response = new HashMap<>();
        response.put("id", team.getId());
        response.put("name", team.getName());
        response.put("description", team.getDescription());
        response.put("createdAt", team.getCreatedAt());
        response.put("myRole", TeamRole.OWNER);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyTeams(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<Team> teams;

        if (currentUser.getPlatformRole() == PlatformRole.ADMIN) {
            teams = teamRepository.findAll();
        } else {
            teams = teamRepository.findByUserId(currentUser.getId());
        }

        List<Map<String, Object>> result = teams.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("description", t.getDescription());
            map.put("createdAt", t.getCreatedAt());
            teamMembershipRepository.findByTeamIdAndUserId(t.getId(), currentUser.getId())
                    .ifPresent(m -> map.put("myRole", m.getRole()));
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTeamById(@PathVariable Long id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("id", team.getId());
        response.put("name", team.getName());
        response.put("description", team.getDescription());
        response.put("createdAt", team.getCreatedAt());
        if (currentUser != null) {
            teamMembershipRepository.findByTeamIdAndUserId(team.getId(), currentUser.getId())
                    .ifPresent(m -> {
                        response.put("myRole", m.getRole());
                        response.put("allowSelfReview", m.isAllowSelfReview());
                    });
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<Map<String, Object>>> getTeamMembers(@PathVariable Long id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);

        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(team.getId());
        List<Map<String, Object>> result = memberships.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("membershipId", m.getId());
            map.put("userId", m.getUser().getId());
            map.put("username", m.getUser().getUsername());
            map.put("email", m.getUser().getEmail());
            map.put("role", m.getRole());
            map.put("allowSelfReview", m.isAllowSelfReview());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/members")
    @Transactional
    public ResponseEntity<Map<String, Object>> addOrUpdateMember(@PathVariable Long id,
                                                                 @RequestBody Map<String, Object> body,
                                                                 Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);
        teamSecurityService.verifyTeamOwner(id, currentUser);

        String username = (String) body.get("username");
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        String roleStr = (String) body.get("role");
        Boolean allowSelfReview = (Boolean) body.get("allowSelfReview");

        User targetUser = null;
        if (userId != null) {
            targetUser = userRepository.findById(userId).orElse(null);
        } else if (username != null) {
            targetUser = userRepository.findByUsername(username.trim()).orElse(null);
        }

        if (targetUser == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        TeamRole role = TeamRole.CREATOR;
        if (roleStr != null) {
            try {
                role = TeamRole.valueOf(roleStr.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), targetUser.getId())
                .orElse(new TeamMembership(team, targetUser, role));

        membership.setRole(role);
        if (allowSelfReview != null) {
            membership.setAllowSelfReview(allowSelfReview);
        }
        teamMembershipRepository.save(membership);

        return ResponseEntity.ok(Map.of(
                "message", "Member saved successfully",
                "userId", targetUser.getId(),
                "username", targetUser.getUsername(),
                "role", membership.getRole(),
                "allowSelfReview", membership.isAllowSelfReview()
        ));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> removeMember(@PathVariable Long id,
                                                             @PathVariable Long userId,
                                                             Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        teamSecurityService.verifyTeamMember(id, currentUser);
        teamSecurityService.verifyTeamOwner(id, currentUser);

        teamMembershipRepository.deleteByTeamIdAndUserId(id, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    @GetMapping(value = "/{id}/reports/episodes.csv", produces = "text/csv")
    public ResponseEntity<String> downloadEpisodesCsvReport(@PathVariable Long id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);

        List<Episode> episodes = episodeRepository.findByTeamId(team.getId());

        StringBuilder csv = new StringBuilder();
        csv.append("Episode ID,Title,Status,Publish Date,Show Title,Created By,Approved By,Audio File URL,YouTube Video ID,Created At,Updated At\n");

        for (Episode ep : episodes) {
            csv.append(ep.getId()).append(",")
               .append(escapeCsv(ep.getTitle())).append(",")
               .append(ep.getStatus()).append(",")
               .append(ep.getPublishDate() != null ? ep.getPublishDate() : "").append(",")
               .append(escapeCsv(ep.getPodcastShow() != null ? ep.getPodcastShow().getTitle() : "")).append(",")
               .append(escapeCsv(ep.getCreatedBy() != null ? ep.getCreatedBy().getUsername() : "")).append(",")
               .append(escapeCsv(ep.getApprovedBy() != null ? ep.getApprovedBy().getUsername() : "")).append(",")
               .append(escapeCsv(ep.getAudioFileUrl())).append(",")
               .append(escapeCsv(ep.getYoutubeVideoId())).append(",")
               .append(ep.getCreatedAt() != null ? ep.getCreatedAt() : "").append(",")
               .append(ep.getUpdatedAt() != null ? ep.getUpdatedAt() : "").append("\n");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"team_" + id + "_episodes.csv\"");
        return ResponseEntity.ok().headers(headers).body(csv.toString());
    }

    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getTeamAuditLogs(@PathVariable Long id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Team team = teamSecurityService.verifyTeamMember(id, currentUser);

        List<AuditLog> logs = auditLogRepository.findByTeamIdOrderByTimestampDesc(team.getId());
        List<Map<String, Object>> result = logs.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("episodeId", l.getEpisodeId());
            map.put("teamId", l.getTeamId());
            map.put("action", l.getAction());
            map.put("timestamp", l.getTimestamp());
            map.put("performedBy", l.getPerformedBy() != null ? l.getPerformedBy().getUsername() : "System");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
