package com.company.meetingroom.room;

import com.company.meetingroom.common.ApiException;
import com.company.meetingroom.room.RoomDtos.CreateRoomRequest;
import com.company.meetingroom.room.RoomDtos.PageResponse;
import com.company.meetingroom.room.RoomDtos.RoomResponse;
import com.company.meetingroom.room.RoomDtos.UpdateRoomRequest;
import com.company.meetingroom.booking.BookingRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final Clock clock;

    public RoomService(RoomRepository rooms, BookingRepository bookings, Clock clock) {
        this.rooms = rooms;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> findAvailable(Instant from, Instant to, int minCapacity) {
        if (!to.isAfter(from)) {
            throw ApiException.badRequest("INVALID_TIME_RANGE", "to must be after from");
        }
        return rooms.findAvailable(from, to, minCapacity).stream().map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse create(CreateRoomRequest req) {
        String name = req.name().trim();
        if (rooms.existsByNameIgnoreCase(name)) {
            throw nameTaken();
        }
        try {
            return RoomResponse.from(rooms.saveAndFlush(new Room(name, req.capacity(), req.location())));
        } catch (DataIntegrityViolationException e) {
            throw nameTaken();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(Boolean active, int minCapacity, Pageable pageable) {
        return PageResponse.from(rooms.search(active, minCapacity, pageable), RoomResponse::from);
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long id) {
        return RoomResponse.from(find(id));
    }

    @Transactional
    public RoomResponse update(Long id, UpdateRoomRequest req) {
        Room room = find(id);
        if (req.name() != null) {
            String name = req.name().trim();
            if (name.isEmpty()) {
                throw ApiException.badRequest("VALIDATION_ERROR", "name must not be blank");
            }
            if (rooms.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw nameTaken();
            }
            room.setName(name);
        }
        if (req.capacity() != null) {
            room.setCapacity(req.capacity());
        }
        if (req.location() != null) {
            room.setLocation(req.location());
        }
        if (req.active() != null) {
            if (!req.active() && room.isActive()
                    && bookings.existsUpcomingConfirmed(room.getId(), clock.instant())) {
                throw ApiException.conflict("ROOM_HAS_UPCOMING_BOOKINGS",
                        "Room has upcoming confirmed bookings; cancel them before deactivating");
            }
            room.setActive(req.active());
        }
        try {
            return RoomResponse.from(rooms.saveAndFlush(room));
        } catch (DataIntegrityViolationException e) {
            throw nameTaken();
        }
    }

    public Room find(Long id) {
        return rooms.findById(id)
                .orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND", "Room " + id + " not found"));
    }

    private static ApiException nameTaken() {
        return ApiException.conflict("ROOM_NAME_ALREADY_EXISTS", "A room with this name already exists");
    }
}
