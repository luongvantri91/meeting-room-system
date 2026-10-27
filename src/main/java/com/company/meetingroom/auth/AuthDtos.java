package com.company.meetingroom.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 150) String fullName) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
    }

    public record UserResponse(Long id, String email, String fullName, Role role, Instant createdAt) {

        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getCreatedAt());
        }
    }
}
