package com.company.meetingroom.booking;

import com.company.meetingroom.auth.AuthUser;
import com.company.meetingroom.booking.BookingDtos.BookingResponse;
import com.company.meetingroom.booking.BookingDtos.CreateBookingRequest;
import com.company.meetingroom.booking.BookingDtos.UpdateBookingRequest;
import com.company.meetingroom.common.ApiException;
import com.company.meetingroom.room.Room;
import com.company.meetingroom.room.RoomDtos.PageResponse;
import com.company.meetingroom.room.RoomService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    static final String OVERLAP_CONSTRAINT = "ex_bookings_no_overlap";

    private final BookingRepository bookings;
    private final RoomService rooms;
    private final BookingProperties props;
    private final Clock clock;

    public BookingService(BookingRepository bookings, RoomService rooms, BookingProperties props, Clock clock) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse create(AuthUser actor, CreateBookingRequest req) {
        validateTime(req.startTime(), req.endTime());

        Room room = rooms.find(req.roomId());
        if (!room.isActive()) {
            throw ApiException.unprocessable("ROOM_INACTIVE", "Room is not available for booking");
        }
        if (req.attendees() > room.getCapacity()) {
            throw ApiException.unprocessable("CAPACITY_EXCEEDED",
                    "Attendees exceed room capacity of " + room.getCapacity());
        }
        if (bookings.existsOverlap(room.getId(), req.startTime(), req.endTime())) {
            throw conflict();
        }

        Booking booking = new Booking(room, actor.id(), req.title().trim(), req.attendees(),
                req.startTime(), req.endTime());
        try {
            return BookingResponse.from(bookings.saveAndFlush(booking));
        } catch (DataIntegrityViolationException e) {
            // A concurrent request won the race; the PostgreSQL exclusion constraint is the final guard.
            if (isOverlapViolation(e)) {
                throw conflict();
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id) {
        Booking booking = find(id);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> search(AuthUser actor, Long roomId, Instant from, Instant to,
                                                boolean mine, boolean includeCancelled, Pageable pageable) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw ApiException.badRequest("INVALID_TIME_RANGE", "to must be after from");
        }
        Specification<Booking> spec = (root, query, cb) -> cb.conjunction();
        if (roomId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("room").get("id"), roomId));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThan(root.get("endTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThan(root.get("startTime"), to));
        }
        if (mine) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("userId"), actor.id()));
        }
        if (!includeCancelled) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), BookingStatus.CONFIRMED));
        }
        return PageResponse.from(bookings.findAll(spec, pageable), BookingResponse::from);
    }

    @Transactional
    public BookingResponse cancel(AuthUser actor, Long id) {
        Booking booking = find(id);
        assertOwnerOrAdmin(actor, booking, "cancel");
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingResponse.from(booking);
        }
        Instant now = clock.instant();
        if (!booking.getStartTime().isAfter(now)) {
            throw ApiException.conflict("BOOKING_ALREADY_STARTED", "A booking that has started cannot be cancelled");
        }
        booking.cancel(now);
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse update(AuthUser actor, Long id, UpdateBookingRequest req) {
        if (req.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_ERROR", "At least one field must be provided");
        }
        if (req.title() != null && req.title().isBlank()) {
            throw ApiException.badRequest("VALIDATION_ERROR", "title must not be blank");
        }
        Booking booking = find(id);
        assertOwnerOrAdmin(actor, booking, "modify");
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("BOOKING_CANCELLED", "A cancelled booking cannot be modified");
        }
        Instant now = clock.instant();
        if (!booking.getEndTime().isAfter(now)) {
            throw ApiException.conflict("BOOKING_ALREADY_ENDED", "A booking that has ended cannot be modified");
        }
        boolean started = !booking.getStartTime().isAfter(now);

        Room room = booking.getRoom();
        boolean roomChanged = req.roomId() != null && !req.roomId().equals(room.getId());
        boolean startChanged = req.startTime() != null && !req.startTime().equals(booking.getStartTime());
        if (started && (roomChanged || startChanged)) {
            throw ApiException.conflict("BOOKING_IN_PROGRESS",
                    "A booking in progress can only change endTime, title and attendees");
        }
        if (roomChanged) {
            room = rooms.find(req.roomId());
        }
        Instant start = req.startTime() != null ? req.startTime() : booking.getStartTime();
        Instant end = req.endTime() != null ? req.endTime() : booking.getEndTime();
        String title = req.title() != null ? req.title().trim() : booking.getTitle();
        int attendees = req.attendees() != null ? req.attendees() : booking.getAttendees();

        if (started) {
            validateRange(start, end);
            if (!end.isAfter(now)) {
                throw ApiException.unprocessable("BOOKING_IN_PAST", "endTime must be in the future");
            }
            validateDuration(start, end);
        } else {
            validateTime(start, end);
        }
        if (!room.isActive()) {
            throw ApiException.unprocessable("ROOM_INACTIVE", "Room is not available for booking");
        }
        if (attendees > room.getCapacity()) {
            throw ApiException.unprocessable("CAPACITY_EXCEEDED",
                    "Attendees exceed room capacity of " + room.getCapacity());
        }
        if (bookings.existsOverlapExcluding(room.getId(), start, end, booking.getId())) {
            throw conflict();
        }

        booking.reschedule(room, title, attendees, start, end);
        try {
            return BookingResponse.from(bookings.saveAndFlush(booking));
        } catch (DataIntegrityViolationException e) {
            if (isOverlapViolation(e)) {
                throw conflict();
            }
            throw e;
        }
    }

    private Booking find(Long id) {
        return bookings.findById(id)
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Booking " + id + " not found"));
    }

    private static void assertOwnerOrAdmin(AuthUser actor, Booking booking, String action) {
        if (!actor.isAdmin() && !booking.getUserId().equals(actor.id())) {
            throw ApiException.forbidden("FORBIDDEN", "You can only " + action + " your own bookings");
        }
    }

    private void validateTime(Instant start, Instant end) {
        validateRange(start, end);
        if (start.isBefore(clock.instant())) {
            throw ApiException.unprocessable("BOOKING_IN_PAST", "Booking cannot start in the past");
        }
        validateDuration(start, end);
    }

    private static void validateRange(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("INVALID_TIME_RANGE", "endTime must be after startTime");
        }
    }

    private void validateDuration(Instant start, Instant end) {
        Duration duration = Duration.between(start, end);
        if (duration.compareTo(props.minDuration()) < 0 || duration.compareTo(props.maxDuration()) > 0) {
            throw ApiException.unprocessable("INVALID_DURATION", "Booking duration must be between "
                    + props.minDuration() + " and " + props.maxDuration());
        }
    }

    static boolean isOverlapViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(OVERLAP_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }

    private static ApiException conflict() {
        return ApiException.conflict("BOOKING_CONFLICT", "Room is already booked in the requested time range");
    }
}
