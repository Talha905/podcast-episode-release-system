package com.podcastrelease.model;

import jakarta.persistence.*;

@Entity
@Table(name = "team_memberships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "user_id"})
})
public class TeamMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role;

    private boolean allowSelfReview;

    public TeamMembership() {
        this.allowSelfReview = false;
    }

    public TeamMembership(Team team, User user, TeamRole role) {
        this();
        this.team = team;
        this.user = user;
        this.role = role;
    }

    public TeamMembership(Team team, User user, TeamRole role, boolean allowSelfReview) {
        this(team, user, role);
        this.allowSelfReview = allowSelfReview;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }

    public boolean isAllowSelfReview() { return allowSelfReview; }
    public void setAllowSelfReview(boolean allowSelfReview) { this.allowSelfReview = allowSelfReview; }
}
