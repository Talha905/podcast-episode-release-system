package com.podcastrelease.repository;

import com.podcastrelease.model.PodcastShowMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PodcastShowMemberRepository extends JpaRepository<PodcastShowMember, Long> {

    List<PodcastShowMember> findByPodcastShowId(Long podcastShowId);

    List<PodcastShowMember> findByUserId(Long userId);

    List<PodcastShowMember> findByUserUsername(String username);

    boolean existsByPodcastShowIdAndUserId(Long podcastShowId, Long userId);

    Optional<PodcastShowMember> findByPodcastShowIdAndUserId(Long podcastShowId, Long userId);
}
