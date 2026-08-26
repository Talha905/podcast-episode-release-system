package com.podcastrelease.repository;

import com.podcastrelease.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEpisodeIdOrderByTimestampDesc(Long episodeId);
}
