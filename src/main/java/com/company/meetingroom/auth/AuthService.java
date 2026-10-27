package com.company.meetingroom.auth;

import com.company.meetingroom.auth.AuthDtos.LoginRequest;
import com.company.meetingroom.auth.AuthDtos.RegisterRequest;
import com.company.meetingroom.auth.AuthDtos.TokenResponse;
import com.company.meetingroom.auth.AuthDtos.UserResponse;
import com.company.meetingroom.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dummyHash = encoder.encode("timing-equalizer");
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        return UserResponse.from(create(req.email(), req.password(), req.fullName(), Role.USER));
    }

    @Transactional
    public User create(String email, String rawPassword, String fullName, Role role) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw emailTaken();
        }
        try {
            return users.saveAndFlush(new User(normalized, encoder.encode(rawPassword), fullName.trim(), role));
        } catch (DataIntegrityViolationException e) {
            throw emailTaken();
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        // Always run a hash comparison so unknown emails and wrong passwords look alike.
        boolean ok = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        return new TokenResponse(jwt.issue(user), "Bearer", jwt.getTtl().toSeconds());
    }

    private static ApiException emailTaken() {
        return ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email is already registered");
    }
}
