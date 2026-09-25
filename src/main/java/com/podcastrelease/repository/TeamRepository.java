package com.podcastrelease.repository;

import com.podcastrelease.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);

    @Query("SELECT t FROM Team t JOIN TeamMembership tm ON t.id = tm.team.id WHERE tm.user.id = :userId")
    List<Team> findByUserId(@Param("userId") Long userId);
}
