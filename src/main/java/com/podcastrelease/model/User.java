package com.podcastrelease.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String username;

    private String email;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private PlatformRole platformRole;

    private boolean enabled;

    public User() {
        this.enabled = true;
    }

    public User(String username, String passwordHash, PlatformRole platformRole) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.platformRole = platformRole;
    }

    public User(String username, String email, String passwordHash, PlatformRole platformRole) {
        this(username, passwordHash, platformRole);
        this.email = email;
    }

    public User(String username, String email, String passwordHash) {
        this(username, email, passwordHash, null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public PlatformRole getPlatformRole() { return platformRole; }
    public void setPlatformRole(PlatformRole platformRole) { this.platformRole = platformRole; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
