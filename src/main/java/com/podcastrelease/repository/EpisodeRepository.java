package com.podcastrelease.repository;

import com.podcastrelease.model.Episode;
import com.podcastrelease.model.EpisodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface EpisodeRepository extends JpaRepository<Episode, Long>, JpaSpecificationExecutor<Episode> {

    long countByStatus(EpisodeStatus status);

    @Query("SELECT e FROM Episode e WHERE " +
           "(:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:fromDate IS NULL OR e.publishDate >= :fromDate) AND " +
           "(:toDate IS NULL OR e.publishDate <= :toDate) " +
           "ORDER BY e.createdAt DESC")
    List<Episode> searchEpisodes(@Param("title") String title,
                                @Param("status") EpisodeStatus status,
                                @Param("fromDate") LocalDate fromDate,
                                @Param("toDate") LocalDate toDate);
}