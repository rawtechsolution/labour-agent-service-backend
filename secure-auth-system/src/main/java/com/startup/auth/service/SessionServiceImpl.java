package com.startup.auth.service;

import com.startup.auth.entity.Session;
import com.startup.auth.entity.User;
import com.startup.auth.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Override
    public Session createSession(User user, String refreshToken, String deviceInfo) {
        Session session = Session.builder()
                .user(user)
                .refreshToken(refreshToken)
                .deviceInfo(deviceInfo)
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7)) // Example: 7 days expiry
                .revoked(false)
                .build();
        return sessionRepository.save(session);
    }

    @Override
    public void revokeSession(Long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setRevoked(true);
            session.setExpiresAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    @Override
    public void revokeAllUserSessions(Long userId) {
        List<Session> sessions = sessionRepository.findByUserIdAndRevokedFalse(userId);
        for (Session session : sessions) {
            session.setRevoked(true);
            session.setExpiresAt(LocalDateTime.now());
        }
        sessionRepository.saveAll(sessions);
    }

    @Override
    public List<Session> getActiveSessions(Long userId) {
        return sessionRepository.findByUserIdAndRevokedFalse(userId);
    }

    @Override
    public boolean isSessionValid(String refreshToken) {
        return sessionRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }
}
