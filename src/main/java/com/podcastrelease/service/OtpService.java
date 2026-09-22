package com.podcastrelease.service;

import com.podcastrelease.model.User;
import com.podcastrelease.model.UserOtp;
import com.podcastrelease.repository.UserOtpRepository;
import com.podcastrelease.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final UserOtpRepository userOtpRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public OtpService(UserOtpRepository userOtpRepository,
                      UserRepository userRepository,
                      @Autowired(required = false) JavaMailSender mailSender) {
        this.userOtpRepository = userOtpRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public String generateAndSendOtp(String email) {
        String otpCode = String.format("%06d", random.nextInt(1_000_000));
        UserOtp userOtp = new UserOtp(email, otpCode, 15);
        userOtpRepository.save(userOtp);

        boolean emailSent = false;
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("[Podcast Release System] Your Account Verification Code");
                message.setText("Welcome to Podcast Release System!\n\n" +
                        "Your account registration verification OTP code is: " + otpCode + "\n\n" +
                        "This code will expire in 15 minutes.\n" +
                        "If you did not request this code, please ignore this email.");
                mailSender.send(message);
                emailSent = true;
                logger.info("Sent registration OTP email to {}", email);
            } catch (Exception e) {
                logger.warn("Failed to send email to {} via JavaMailSender: {}. Falling back to log verification code.", email, e.getMessage());
            }
        }

        if (!emailSent) {
            logger.info("\n========================================================\n" +
                    "[OTP SERVICE DEV FALLBACK] Account Verification Code for {}: [{}]\n" +
                    "========================================================", email, otpCode);
        }

        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        if (email == null || otpCode == null) return false;

        UserOtp otp = userOtpRepository.findByEmailAndOtpCodeAndVerifiedFalse(email.trim(), otpCode.trim())
                .orElse(null);

        if (otp == null || otp.isExpired()) {
            return false;
        }

        otp.setVerified(true);
        userOtpRepository.save(otp);

        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user != null) {
            user.setEnabled(true);
            userRepository.save(user);
            logger.info("Activated user account for username: {}, email: {}", user.getUsername(), email);
        }

        return true;
    }
}
