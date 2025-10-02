package com.startup.auth.service;

import com.startup.auth.dto.request.LoginRequest;
import com.startup.auth.dto.request.RefreshTokenRequest;
import com.startup.auth.dto.request.RegisterRequest;
import com.startup.auth.dto.response.AuthResponse;
import com.startup.auth.entity.Role;
import com.startup.auth.entity.Session;
import com.startup.auth.entity.User;
import com.startup.auth.exception.BadRequestException;
import com.startup.auth.exception.ResourceNotFoundException;
import com.startup.auth.repository.RoleRepository;
import com.startup.auth.repository.SessionRepository;
import com.startup.auth.repository.UserRepository;
import com.startup.auth.security.JwtUtils;
import com.startup.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SessionService sessionService;

    public AuthResponse registerUser(RegisterRequest registerRequest) {
        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email is already in use!");
        }

        if (registerRequest.getPhone() != null && userRepository.existsByPhone(registerRequest.getPhone())) {
            throw new BadRequestException("Phone number is already in use!");
        }

        // Create new user account
        User user = User.builder()
                .email(registerRequest.getEmail())
                .phone(registerRequest.getPhone())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .isActive(true)
                .mfaEnabled(false)
                .build();

        // Assign default role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtils.generateTokenFromUserId(savedUser.getId());
        String refreshToken = jwtUtils.generateRefreshToken(savedUser.getId());

        // Create session
        Session session = sessionService.createSession(savedUser, refreshToken, registerRequest.getDeviceInfo());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .roles(savedUser.getRoles().stream().map(Role::getName).toList())
                .build();
    }

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtils.generateJwtToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(userPrincipal.getId());

        // Create session
        Session session = sessionService.createSession(user, refreshToken, loginRequest.getDeviceInfo());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest tokenRefreshRequest) {
        String requestRefreshToken = tokenRefreshRequest.getRefreshToken();

        // Validate refresh token
        if (!jwtUtils.validateJwtToken(requestRefreshToken)) {
            throw new BadRequestException("Invalid refresh token!");
        }

        // Check token type
        String tokenType = jwtUtils.getTokenType(requestRefreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new BadRequestException("Token is not a refresh token!");
        }

        // Find session by refresh token
        Session session = sessionRepository.findByRefreshTokenAndRevokedFalse(requestRefreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token not found or has been revoked!"));

        // Check if session is expired
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionService.revokeSession(session.getId());
            throw new BadRequestException("Refresh token was expired. Please make a new signin request!");
        }

        // Generate new access token
        Long userId = jwtUtils.getUserIdFromJwtToken(requestRefreshToken);
        String newAccessToken = jwtUtils.generateTokenFromUserId(userId);

        // Update session last used time
        session.setLastUsedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Get user details
        User user = session.getUser();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(requestRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }

    public void logout(String refreshToken) {
        Optional<Session> sessionOpt = sessionRepository.findByRefreshTokenAndRevokedFalse(refreshToken);
        if (sessionOpt.isPresent()) {
            sessionService.revokeSession(sessionOpt.get().getId());
        }
    }

    public void logoutFromAllDevices(Long userId) {
        sessionService.revokeAllUserSessions(userId);
    }
}