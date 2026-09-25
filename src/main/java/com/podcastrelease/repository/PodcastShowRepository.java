package com.podcastrelease.repository;

import com.podcastrelease.model.PodcastShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PodcastShowRepository extends JpaRepository<PodcastShow, Long> {
    Optional<PodcastShow> findBySlug(String slug);
    List<PodcastShow> findByTeamId(Long teamId);
    Optional<PodcastShow> findByIdAndTeamId(Long id, Long teamId);
}
