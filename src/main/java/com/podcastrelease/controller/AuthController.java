package com.podcastrelease.controller;

import com.podcastrelease.model.PlatformRole;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        String platformRoleStr = body.get("platformRole");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
        }

        if (userRepository.existsByUsername(username.trim())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username '" + username + "' is already taken"));
        }

        PlatformRole platformRole = null;
        if (platformRoleStr != null && !platformRoleStr.trim().isEmpty()) {
            try {
                platformRole = PlatformRole.valueOf(platformRoleStr.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        User user = new User(username.trim(), email != null ? email.trim() : null, passwordEncoder.encode(password), platformRole);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", username.trim());
        response.put("platformRole", platformRole);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("username", username);
        response.put("platformRole", user != null ? user.getPlatformRole() : null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("email", user != null ? user.getEmail() : null);
        response.put("platformRole", user != null ? user.getPlatformRole() : null);
        return ResponseEntity.ok(response);
    }
}
