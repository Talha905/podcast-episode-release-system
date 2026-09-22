package com.podcastrelease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "podcast_show_members")
public class PodcastShowMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "podcast_show_id", nullable = false)
    private PodcastShow podcastShow;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole roleInShow; // PRODUCER or HOST

    private LocalDateTime assignedAt;

    public PodcastShowMember() {
        this.assignedAt = LocalDateTime.now();
    }

    public PodcastShowMember(PodcastShow podcastShow, User user, UserRole roleInShow) {
        this();
        this.podcastShow = podcastShow;
        this.user = user;
        this.roleInShow = roleInShow;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PodcastShow getPodcastShow() {
        return podcastShow;
    }

    public void setPodcastShow(PodcastShow podcastShow) {
        this.podcastShow = podcastShow;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserRole getRoleInShow() {
        return roleInShow;
    }

    public void setRoleInShow(UserRole roleInShow) {
        this.roleInShow = roleInShow;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
