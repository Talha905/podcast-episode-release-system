package com.podcastrelease.repository;

import com.podcastrelease.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findTop10ByRecipientIdOrderByCreatedAtDesc(Long userId);
    long countByRecipientIdAndIsReadFalse(Long userId);
}
