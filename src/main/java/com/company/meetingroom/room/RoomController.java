package com.company.meetingroom.room;

import com.company.meetingroom.room.RoomDtos.CreateRoomRequest;
import com.company.meetingroom.room.RoomDtos.PageResponse;
import com.company.meetingroom.room.RoomDtos.RoomResponse;
import com.company.meetingroom.room.RoomDtos.UpdateRoomRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest request) {
        return service.create(request);
    }

    @GetMapping
    public PageResponse<RoomResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "1") @Min(1) int minCapacity,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(active, minCapacity, PageRequest.of(page, size, Sort.by("name")));
    }

    @GetMapping("/available")
    public List<RoomResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "1") @Min(1) int minCapacity) {
        return service.findAvailable(from, to, minCapacity);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRoomRequest request) {
        return service.update(id, request);
    }
}
