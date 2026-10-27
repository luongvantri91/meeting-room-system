package com.company.meetingroom.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.meetingroom.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifies that PostgreSQL itself protects the invariants, independent of application code. */
class BookingConstraintTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    long roomId;
    long userId;

    @BeforeEach
    void data() {
        roomId = jdbc.queryForObject(
                "insert into rooms(name, capacity) values (?, 5) returning id", Long.class, "C-" + UUID.randomUUID());
        userId = jdbc.queryForObject(
                "insert into users(email, password_hash, full_name, role) values (?, 'x', 'n', 'USER') returning id",
                Long.class, UUID.randomUUID() + "@t.local");
    }

    private void insert(String start, String end, String status) {
        jdbc.update("insert into bookings(room_id, user_id, title, attendees, start_time, end_time, status) "
                + "values (?, ?, 't', 1, ?::timestamptz, ?::timestamptz, ?)", roomId, userId, start, end, status);
    }

    @Test
    void overlappingConfirmedBookingsAreRejected() {
        insert("2040-01-01T09:00Z", "2040-01-01T10:00Z", "CONFIRMED");
        assertThatThrownBy(() -> insert("2040-01-01T09:30Z", "2040-01-01T10:30Z", "CONFIRMED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_bookings_no_overlap");
    }

    @Test
    void adjacentBookingsAreAllowed() {
        insert("2040-01-02T09:00Z", "2040-01-02T10:00Z", "CONFIRMED");
        insert("2040-01-02T10:00Z", "2040-01-02T11:00Z", "CONFIRMED");
        Integer count = jdbc.queryForObject("select count(*) from bookings where room_id = ?", Integer.class, roomId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void cancelledBookingsDoNotHoldTheSlot() {
        insert("2040-01-03T09:00Z", "2040-01-03T10:00Z", "CANCELLED");
        insert("2040-01-03T09:00Z", "2040-01-03T10:00Z", "CONFIRMED");
        insert("2040-01-03T09:00Z", "2040-01-03T10:00Z", "CANCELLED");
    }

    @Test
    void endMustBeAfterStart() {
        assertThatThrownBy(() -> insert("2040-01-04T10:00Z", "2040-01-04T10:00Z", "CONFIRMED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_bookings_time");
    }

    @Test
    void roomCapacityMustBePositive() {
        assertThatThrownBy(() -> jdbc.update("insert into rooms(name, capacity) values (?, 0)", "Z-" + UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
