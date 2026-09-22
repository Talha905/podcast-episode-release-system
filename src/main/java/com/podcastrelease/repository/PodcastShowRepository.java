package com.podcastrelease.repository;

import com.podcastrelease.model.PodcastShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PodcastShowRepository extends JpaRepository<PodcastShow, Long> {
    Optional<PodcastShow> findBySlug(String slug);
}
