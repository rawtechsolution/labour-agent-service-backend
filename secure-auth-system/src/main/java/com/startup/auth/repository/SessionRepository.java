package com.startup.auth.repository;

import com.startup.auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByRefreshToken(String refreshToken);

    Optional<Session> findByRefreshTokenAndRevokedFalse(String refreshToken);

    List<Session> findByUserIdAndRevokedFalse(Long userId);

    List<Session> findByUserIdAndRevokedFalseAndExpiresAtAfter(Long userId, LocalDateTime now);

    List<Session> findByUserIdAndRevokedFalseAndExpiresAtBefore(Long userId, LocalDateTime now);

    List<Session> findByRevokedFalseAndExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.revoked = true, s.updatedAt = :now WHERE s.user.id = :userId AND s.revoked = false")
    int revokeAllUserSessions(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.revoked = true, s.updatedAt = :now WHERE s.expiresAt < :now AND s.revoked = false")
    int revokeExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId AND s.revoked = false AND s.expiresAt > :now")
    long countActiveSessionsByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.deviceType = :deviceType AND s.revoked = false")
    List<Session> findByUserIdAndDeviceTypeAndRevokedFalse(@Param("userId") Long userId, 
                                                          @Param("deviceType") Session.DeviceType deviceType);
}