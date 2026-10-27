package com.company.meetingroom.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private final User user = userWithId();

    private static User userWithId() {
        User u = new User("a@test.local", "hash", "A", Role.ADMIN);
        org.springframework.test.util.ReflectionTestUtils.setField(u, "id", 7L);
        return u;
    }

    @Test
    void roundTripPreservesIdentityAndRole() {
        JwtService jwt = new JwtService(SECRET, Duration.ofHours(1));
        AuthUser parsed = jwt.parse(jwt.issue(user)).orElseThrow();
        assertThat(parsed.email()).isEqualTo("a@test.local");
        assertThat(parsed.id()).isEqualTo(7L);
        assertThat(parsed.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void rejectsExpiredTamperedAndForeignTokens() {
        JwtService jwt = new JwtService(SECRET, Duration.ofHours(1));
        JwtService expired = new JwtService(SECRET, Duration.ofSeconds(-10));
        JwtService other = new JwtService("another-secret-another-secret-123456", Duration.ofHours(1));

        assertThat(jwt.parse(expired.issue(user))).isEmpty();
        assertThat(jwt.parse(other.issue(user))).isEmpty();
        assertThat(jwt.parse(jwt.issue(user) + "x")).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> new JwtService("short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
