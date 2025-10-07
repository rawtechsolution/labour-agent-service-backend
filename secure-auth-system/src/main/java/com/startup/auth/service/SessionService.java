package com.startup.auth.service;

import com.startup.auth.entity.Session;
import com.startup.auth.entity.User;
import java.util.List;

public interface SessionService {
    Session createSession(User user, String refreshToken, String deviceInfo);
    void revokeSession(Long sessionId);
    void revokeAllUserSessions(Long userId);
    List<Session> getActiveSessions(Long userId);
    boolean isSessionValid(String refreshToken);
}
