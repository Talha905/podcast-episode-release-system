package com.podcastrelease.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "episodes")
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(length = 2000, nullable = false)
    private String description;

    private String audioFileUrl;

    @NotNull(message = "Publish date is required")
    @Column(nullable = false)
    private LocalDate publishDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EpisodeStatus status = EpisodeStatus.DRAFT;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Episode() {}

    public Episode(String title, String description, String audioFileUrl, LocalDate publishDate) {
        this.title = title;
        this.description = description;
        this.audioFileUrl = audioFileUrl;
        this.publishDate = publishDate;
        this.status = EpisodeStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAudioFileUrl() { return audioFileUrl; }
    public void setAudioFileUrl(String audioFileUrl) { this.audioFileUrl = audioFileUrl; }

    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }

    public EpisodeStatus getStatus() { return status; }
    public void setStatus(EpisodeStatus status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}