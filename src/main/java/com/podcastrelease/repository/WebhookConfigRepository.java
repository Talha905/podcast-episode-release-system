package com.podcastrelease.repository;

import com.podcastrelease.model.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {
}
