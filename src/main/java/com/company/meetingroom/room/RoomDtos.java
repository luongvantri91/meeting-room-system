package com.company.meetingroom.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.domain.Page;

public final class RoomDtos {

    private RoomDtos() {
    }

    public record CreateRoomRequest(
            @NotBlank @Size(max = 100) String name,
            @Min(1) int capacity,
            @Size(max = 200) String location) {
    }

    /** Partial update: null fields are left unchanged. */
    public record UpdateRoomRequest(
            @Size(min = 1, max = 100) String name,
            @Min(1) Integer capacity,
            @Size(max = 200) String location,
            Boolean active) {
    }

    public record RoomResponse(Long id, String name, int capacity, String location, boolean active,
                               Instant createdAt, Instant updatedAt) {

        public static RoomResponse from(Room r) {
            return new RoomResponse(r.getId(), r.getName(), r.getCapacity(), r.getLocation(), r.isActive(),
                    r.getCreatedAt(), r.getUpdatedAt());
        }
    }

    public record PageResponse<T>(java.util.List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <E, T> PageResponse<T> from(Page<E> page, java.util.function.Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
