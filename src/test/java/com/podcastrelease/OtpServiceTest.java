package com.podcastrelease;

import com.podcastrelease.model.User;
import com.podcastrelease.model.UserOtp;
import com.podcastrelease.repository.*;
import com.podcastrelease.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OtpServiceTest {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOtpRepository userOtpRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PodcastShowMemberRepository podcastShowMemberRepository;

    @Autowired
    private TeamInviteRepository teamInviteRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private TeamRepository teamRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        episodeRepository.deleteAll();
        podcastShowMemberRepository.deleteAll();
        teamInviteRepository.deleteAll();
        teamMembershipRepository.deleteAll();
        teamRepository.deleteAll();
        userOtpRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("otpuser", "otpuser@example.com", "pass123");
        testUser.setEnabled(false);
        testUser = userRepository.save(testUser);
    }

    @Test
    void generateAndSendOtp_createsOtpRecord() {
        String code = otpService.generateAndSendOtp(testUser.getEmail());

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(userOtpRepository.findTopByEmailOrderByCreatedAtDesc(testUser.getEmail()).isPresent());
    }

    @Test
    void verifyOtp_withValidCode_activatesUserAndMarksVerified() {
        String code = otpService.generateAndSendOtp(testUser.getEmail());

        boolean verified = otpService.verifyOtp(testUser.getEmail(), code);

        assertTrue(verified);
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(updatedUser.isEnabled());
        UserOtp otp = userOtpRepository.findTopByEmailOrderByCreatedAtDesc(testUser.getEmail()).orElseThrow();
        assertTrue(otp.isVerified());
    }

    @Test
    void verifyOtp_withIncorrectCode_failsAndUserRemainsDisabled() {
        otpService.generateAndSendOtp(testUser.getEmail());

        boolean verified = otpService.verifyOtp(testUser.getEmail(), "000000");

        assertFalse(verified);
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertFalse(updatedUser.isEnabled());
    }

    @Test
    void verifyOtp_withExpiredCode_fails() {
        String code = otpService.generateAndSendOtp(testUser.getEmail());
        UserOtp otp = userOtpRepository.findTopByEmailOrderByCreatedAtDesc(testUser.getEmail()).orElseThrow();
        otp.setExpiryTime(LocalDateTime.now().minusMinutes(1));
        userOtpRepository.save(otp);

        boolean verified = otpService.verifyOtp(testUser.getEmail(), code);

        assertFalse(verified);
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertFalse(updatedUser.isEnabled());
    }
}
