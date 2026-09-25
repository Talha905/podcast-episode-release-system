package com.podcastrelease.service;

import com.podcastrelease.exception.TeamNotFoundException;
import com.podcastrelease.model.PlatformRole;
import com.podcastrelease.model.Team;
import com.podcastrelease.model.TeamMembership;
import com.podcastrelease.model.TeamRole;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.TeamMembershipRepository;
import com.podcastrelease.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TeamSecurityService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public TeamSecurityService(TeamRepository teamRepository, TeamMembershipRepository teamMembershipRepository) {
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    public Team verifyTeamMember(Long teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found: " + teamId));

        if (user != null && user.getPlatformRole() != null &&
            (user.getPlatformRole() == PlatformRole.ADMIN || user.getPlatformRole() == PlatformRole.SYSTEM)) {
            return team;
        }

        if (user == null || !teamMembershipRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
            // Strictly throw 404 NOT_FOUND so unauthorized users cannot probe resource existence
            throw new TeamNotFoundException("Team not found: " + teamId);
        }
        return team;
    }

    public TeamMembership verifyTeamOwner(Long teamId, User user) {
        verifyTeamMember(teamId, user);
        if (user != null && user.getPlatformRole() == PlatformRole.ADMIN) {
            return teamMembershipRepository.findByTeamIdAndUserId(teamId, user.getId())
                    .orElse(null);
        }
        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new TeamNotFoundException("Team not found: " + teamId));

        if (membership.getRole() != TeamRole.OWNER) {
            throw new TeamNotFoundException("Team not found: " + teamId);
        }
        return membership;
    }

    public Optional<TeamMembership> getMembership(Long teamId, User user) {
        if (user == null) return Optional.empty();
        return teamMembershipRepository.findByTeamIdAndUserId(teamId, user.getId());
    }

    public boolean isMember(Long teamId, User user) {
        if (user == null) return false;
        if (user.getPlatformRole() != null &&
            (user.getPlatformRole() == PlatformRole.ADMIN || user.getPlatformRole() == PlatformRole.SYSTEM)) {
            return true;
        }
        return teamMembershipRepository.existsByTeamIdAndUserId(teamId, user.getId());
    }
}
