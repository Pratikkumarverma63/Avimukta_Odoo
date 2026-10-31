package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.PasswordResetToken;
import com.brightloop.subscription.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);
    void deleteByUser(User user);
}
