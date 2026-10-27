package com.company.meetingroom.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record CreateBookingRequest(
            @NotNull Long roomId,
            @NotBlank @Size(max = 200) String title,
            @NotNull @Min(1) Integer attendees,
            @NotNull Instant startTime,
            @NotNull Instant endTime) {
    }

    public record BookingResponse(Long id, Long roomId, String roomName, Long userId, String title, int attendees,
                                  Instant startTime, Instant endTime, BookingStatus status, Instant createdAt,
                                  Instant cancelledAt) {

        public static BookingResponse from(Booking b) {
            return new BookingResponse(b.getId(), b.getRoom().getId(), b.getRoom().getName(), b.getUserId(),
                    b.getTitle(), b.getAttendees(), b.getStartTime(), b.getEndTime(), b.getStatus(),
                    b.getCreatedAt(), b.getCancelledAt());
        }
    }
}
