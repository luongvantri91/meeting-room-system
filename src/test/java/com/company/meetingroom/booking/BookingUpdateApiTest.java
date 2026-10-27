package com.company.meetingroom.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.meetingroom.TestAuth;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class BookingUpdateApiTest extends BookingApiTestBase {

    static final Duration H = Duration.ofHours(1);

    private ResultActions patchBooking(String token, long id, String json) throws Exception {
        return mvc.perform(patch("/api/v1/bookings/" + id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String times(Instant s, Instant e) {
        return "{\"startTime\":\"" + s + "\",\"endTime\":\"" + e + "\"}";
    }

    @Test
    void ownerCanChangeTimeTitleAndAttendees() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(40));
        long id = bookId(owner, room, s, s.plus(H));

        Instant ns = s.plus(Duration.ofHours(3));
        patchBooking(owner, id, "{\"title\":\"Retro\",\"attendees\":5,\"startTime\":\"" + ns + "\",\"endTime\":\""
                + ns.plus(Duration.ofMinutes(90)) + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Retro"))
                .andExpect(jsonPath("$.attendees").value(5))
                .andExpect(jsonPath("$.startTime").value(ns.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void partialUpdateKeepsOtherFields() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(41));
        long id = bookId(owner, room, s, s.plus(H));

        patchBooking(owner, id, "{\"title\":\"Renamed\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.startTime").value(s.toString()))
                .andExpect(jsonPath("$.roomId").value(room));
    }

    @Test
    void canMoveToAnotherRoom() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        long other = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(42));
        long id = bookId(owner, room, s, s.plus(H));

        patchBooking(owner, id, "{\"roomId\":" + other + "}").andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(other));
        // the old room is free again
        org.assertj.core.api.Assertions.assertThat(book(owner, room, 2, s, s.plus(H))).isEqualTo(201);
    }

    @Test
    void movingIntoOccupiedSlotIsConflictButOwnSlotIsNot() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(43));
        long mine = bookId(owner, room, s, s.plus(H));
        bookId(TestAuth.newUserToken(mvc), room, s.plus(Duration.ofHours(2)), s.plus(Duration.ofHours(3)));

        // overlaps another booking
        patchBooking(owner, mine, times(s.plus(Duration.ofMinutes(90)), s.plus(Duration.ofMinutes(150))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"));
        // back-to-back with the other booking is fine
        patchBooking(owner, mine, times(s.plus(Duration.ofHours(1)), s.plus(Duration.ofHours(2))))
                .andExpect(status().isOk());
        // overlapping its own previous slot is fine (extend)
        patchBooking(owner, mine, times(s, s.plus(Duration.ofMinutes(90)))).andExpect(status().isOk());
    }

    @Test
    void validationRulesApplyToTheResultingBooking() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(4);
        Instant s = BASE.plus(Duration.ofDays(44));
        long id = bookId(owner, room, s, s.plus(H));

        patchBooking(owner, id, "{\"endTime\":\"" + s.minusSeconds(60) + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
        patchBooking(owner, id, "{\"endTime\":\"" + s.plus(Duration.ofMinutes(5)) + "\"}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_DURATION"));
        patchBooking(owner, id, times(Instant.now().minus(H), Instant.now().plus(H)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("BOOKING_IN_PAST"));
        patchBooking(owner, id, "{\"attendees\":5}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
        patchBooking(owner, id, "{}").andExpect(status().isBadRequest());
        patchBooking(owner, id, "{\"title\":\"  \"}").andExpect(status().isBadRequest());
        patchBooking(owner, id, "{\"attendees\":0}").andExpect(status().isBadRequest());
    }

    @Test
    void cannotMoveToInactiveOrUnknownRoom() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        long inactive = createRoom(10);
        mvc.perform(patch("/api/v1/rooms/" + inactive).header(HttpHeaders.AUTHORIZATION, admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());
        Instant s = BASE.plus(Duration.ofDays(45));
        long id = bookId(owner, room, s, s.plus(H));

        patchBooking(owner, id, "{\"roomId\":" + inactive + "}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ROOM_INACTIVE"));
        patchBooking(owner, id, "{\"roomId\":99999999}").andExpect(status().isNotFound());
    }

    @Test
    void onlyOwnerOrAdminCanUpdate() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(46));
        long id = bookId(owner, room, s, s.plus(H));

        patchBooking(TestAuth.newUserToken(mvc), id, "{\"title\":\"Hijack\"}").andExpect(status().isForbidden());
        patchBooking(admin(), id, "{\"title\":\"By admin\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("By admin"));
        mvc.perform(get("/api/v1/bookings/" + id).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(jsonPath("$.title").value("By admin"));
    }

    @Test
    void cancelledBookingCannotBeUpdatedAndUnknownIs404() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(47));
        long id = bookId(owner, room, s, s.plus(H));
        mvc.perform(post("/api/v1/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, owner));

        patchBooking(owner, id, "{\"title\":\"x\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CANCELLED"));
        patchBooking(owner, 99999999L, "{\"title\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(patch("/api/v1/bookings/1").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
