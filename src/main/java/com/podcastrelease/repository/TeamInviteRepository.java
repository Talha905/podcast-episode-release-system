package com.podcastrelease.repository;

import com.podcastrelease.model.TeamInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
    Optional<TeamInvite> findByToken(String token);
    List<TeamInvite> findByTeamId(Long teamId);
    List<TeamInvite> findByEmailAndAcceptedFalse(String email);
    Optional<TeamInvite> findByTeamIdAndEmailAndAcceptedFalse(Long teamId, String email);
}
