package com.podcastrelease.controller;

import com.podcastrelease.model.User;
import com.podcastrelease.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        response.put("role", user != null ? user.getRole() : null);
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
        response.put("role", user != null ? user.getRole() : null);
        return ResponseEntity.ok(response);
    }
}
