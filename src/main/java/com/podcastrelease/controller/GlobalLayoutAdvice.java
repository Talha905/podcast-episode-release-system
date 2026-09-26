package com.podcastrelease.controller;

import com.podcastrelease.model.Team;
import com.podcastrelease.model.TeamMembership;
import com.podcastrelease.model.TeamRole;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.TeamMembershipRepository;
import com.podcastrelease.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalLayoutAdvice {

    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public GlobalLayoutAdvice(UserRepository userRepository, TeamMembershipRepository teamMembershipRepository) {
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    @ModelAttribute
    public void addGlobalLayoutAttributes(
            @RequestParam(required = false) Long teamId,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) return;

        List<TeamMembership> memberships = teamMembershipRepository.findByUserId(currentUser.getId());
        List<Team> myTeams = memberships.stream().map(TeamMembership::getTeam).collect(Collectors.toList());

        Map<Long, TeamRole> myRoles = new HashMap<>();
        for (TeamMembership m : memberships) {
            myRoles.put(m.getTeam().getId(), m.getRole());
        }

        Team activeTeam = null;
        if (teamId != null) {
            activeTeam = myTeams.stream().filter(t -> t.getId().equals(teamId)).findFirst().orElse(null);
        }
        if (activeTeam == null && !myTeams.isEmpty()) {
            activeTeam = myTeams.get(0);
        }

        TeamRole myRoleInActiveTeam = activeTeam != null ? myRoles.get(activeTeam.getId()) : null;

        if (!model.containsAttribute("currentUser")) {
            model.addAttribute("currentUser", currentUser);
        }
        if (!model.containsAttribute("myTeams")) {
            model.addAttribute("myTeams", myTeams);
        }
        if (!model.containsAttribute("myRoles")) {
            model.addAttribute("myRoles", myRoles);
        }
        if (!model.containsAttribute("activeTeam")) {
            model.addAttribute("activeTeam", activeTeam);
        }
        if (!model.containsAttribute("myRoleInActiveTeam")) {
            model.addAttribute("myRoleInActiveTeam", myRoleInActiveTeam);
        }
    }
}
