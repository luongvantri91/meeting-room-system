package com.company.meetingroom.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.meetingroom.auth.AuthUser;
import com.company.meetingroom.auth.Role;
import com.company.meetingroom.booking.BookingDtos.CreateBookingRequest;
import com.company.meetingroom.booking.BookingDtos.UpdateBookingRequest;
import com.company.meetingroom.common.ApiException;
import com.company.meetingroom.room.Room;
import com.company.meetingroom.room.RoomService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @Mock
    BookingRepository bookings;
    @Mock
    RoomService rooms;

    BookingService service;
    final AuthUser user = new AuthUser(1L, "u@test.local", Role.USER);

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, rooms,
                new BookingProperties(Duration.ofMinutes(15), Duration.ofHours(8)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Room room(int capacity, boolean active) {
        Room r = new Room("A", capacity, null);
        r.setActive(active);
        return r;
    }

    private CreateBookingRequest req(int attendees, Instant start, Instant end) {
        return new CreateBookingRequest(1L, "Sync", attendees, start, end);
    }

    private void assertRejected(CreateBookingRequest r, HttpStatus status, String code) {
        assertThatThrownBy(() -> service.create(user, r))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(status);
                    assertThat(e.getCode()).isEqualTo(code);
                });
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void rejectsEndNotAfterStart() {
        Instant s = NOW.plus(Duration.ofHours(1));
        assertRejected(req(2, s, s), HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
        assertRejected(req(2, s, s.minusSeconds(60)), HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
    }

    @Test
    void rejectsPastStart() {
        assertRejected(req(2, NOW.minusSeconds(60), NOW.plus(Duration.ofHours(1))),
                HttpStatus.UNPROCESSABLE_ENTITY, "BOOKING_IN_PAST");
    }

    @Test
    void rejectsTooShortAndTooLong() {
        Instant s = NOW.plus(Duration.ofHours(1));
        assertRejected(req(2, s, s.plus(Duration.ofMinutes(10))), HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DURATION");
        assertRejected(req(2, s, s.plus(Duration.ofHours(9))), HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DURATION");
    }

    @Test
    void rejectsInactiveRoom() {
        when(rooms.find(1L)).thenReturn(room(10, false));
        Instant s = NOW.plus(Duration.ofHours(1));
        assertRejected(req(2, s, s.plus(Duration.ofHours(1))), HttpStatus.UNPROCESSABLE_ENTITY, "ROOM_INACTIVE");
    }

    @Test
    void rejectsOverCapacity() {
        when(rooms.find(1L)).thenReturn(room(4, true));
        Instant s = NOW.plus(Duration.ofHours(1));
        assertRejected(req(5, s, s.plus(Duration.ofHours(1))), HttpStatus.UNPROCESSABLE_ENTITY, "CAPACITY_EXCEEDED");
    }

    @Test
    void rejectsOverlap() {
        when(rooms.find(1L)).thenReturn(room(10, true));
        Instant s = NOW.plus(Duration.ofHours(1));
        Instant e = s.plus(Duration.ofHours(1));
        when(bookings.existsOverlap(any(), any(), any())).thenReturn(true);
        assertRejected(req(2, s, e), HttpStatus.CONFLICT, "BOOKING_CONFLICT");
    }

    @Test
    void cancelRejectsBookingThatAlreadyStarted() {
        Room r = room(10, true);
        Booking b = new Booking(r, 1L, "t", 2, NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(bookings.findById(5L)).thenReturn(java.util.Optional.of(b));
        assertThatThrownBy(() -> service.cancel(user, 5L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo("BOOKING_ALREADY_STARTED"));
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void inProgressBookingCanExtendEndTimeButNotChangeStartOrRoom() {
        Instant start = NOW.minusSeconds(1800);
        Booking b = new Booking(room(10, true), 1L, "t", 2, start, NOW.plusSeconds(1800));
        when(bookings.findById(5L)).thenReturn(java.util.Optional.of(b));
        when(bookings.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        service.update(user, 5L, new UpdateBookingRequest(null, null, null, null, NOW.plusSeconds(5400)));
        assertThat(b.getEndTime()).isEqualTo(NOW.plusSeconds(5400));

        assertThatThrownBy(() -> service.update(user, 5L,
                new UpdateBookingRequest(null, null, null, start.minusSeconds(60), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("BOOKING_IN_PROGRESS"));
        assertThatThrownBy(() -> service.update(user, 5L, new UpdateBookingRequest(99L, null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("BOOKING_IN_PROGRESS"));
    }

    @Test
    void inProgressBookingCannotBeShortenedIntoThePast() {
        Booking b = new Booking(room(10, true), 1L, "t", 2, NOW.minusSeconds(3600), NOW.plusSeconds(1800));
        when(bookings.findById(5L)).thenReturn(java.util.Optional.of(b));
        assertThatThrownBy(() -> service.update(user, 5L,
                new UpdateBookingRequest(null, null, null, null, NOW.minusSeconds(60))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("BOOKING_IN_PAST"));
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void endedBookingCannotBeUpdated() {
        Booking b = new Booking(room(10, true), 1L, "t", 2, NOW.minusSeconds(7200), NOW.minusSeconds(3600));
        when(bookings.findById(5L)).thenReturn(java.util.Optional.of(b));
        assertThatThrownBy(() -> service.update(user, 5L, new UpdateBookingRequest(null, "new", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCode()).isEqualTo("BOOKING_ALREADY_ENDED");
                });
    }

    @Test
    void cancelByAnotherNonAdminUserIsForbidden() {
        Booking b = new Booking(room(10, true), 99L, "t", 2, NOW.plusSeconds(3600), NOW.plusSeconds(7200));
        when(bookings.findById(5L)).thenReturn(java.util.Optional.of(b));
        assertThatThrownBy(() -> service.cancel(user, 5L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void detectsExclusionConstraintViolationInCauseChain() {
        Throwable root = new RuntimeException("conflicting key value violates exclusion constraint \""
                + BookingService.OVERLAP_CONSTRAINT + "\"");
        assertThat(BookingService.isOverlapViolation(new RuntimeException("wrapper", root))).isTrue();
        assertThat(BookingService.isOverlapViolation(new RuntimeException("other"))).isFalse();
    }
}
