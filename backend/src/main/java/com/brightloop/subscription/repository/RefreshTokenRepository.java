package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.RefreshToken;
import com.brightloop.subscription.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    void deleteByUser(User user);
}
