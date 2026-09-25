package com.podcastrelease.repository;

import com.podcastrelease.model.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {
    List<PlatformAccount> findByEnabledTrue();
    List<PlatformAccount> findByTeamId(Long teamId);
    List<PlatformAccount> findByTeamIdAndEnabledTrue(Long teamId);
}
