package com.podcastrelease.repository;

import com.podcastrelease.model.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {

    Optional<UserOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<UserOtp> findByEmailAndOtpCodeAndVerifiedFalse(String email, String otpCode);
}
