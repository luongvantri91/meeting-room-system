package com.company.meetingroom.auth;

public record AuthUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
