package com.company.meetingroom.booking;

import static org.assertj.core.api.Assertions.assertThat;
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

class BookingQueryCancelApiTest extends BookingApiTestBase {

    private static final Duration H = Duration.ofHours(1);

    @Test
    void ownerCancelsAndSlotBecomesFreeAgain() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(10));
        long id = bookId(owner, room, s, s.plus(H));
        assertThat(book(owner, room, 2, s, s.plus(H))).isEqualTo(409);

        mvc.perform(post("/api/v1/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        assertThat(book(TestAuth.newUserToken(mvc), room, 2, s, s.plus(H))).isEqualTo(201);
    }

    @Test
    void cancelIsIdempotent() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(11));
        long id = bookId(owner, room, s, s.plus(H));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, owner))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Test
    void otherUserCanViewButCannotCancelWhileAdminCan() throws Exception {
        String owner = TestAuth.newUserToken(mvc);
        String other = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(12));
        long id = bookId(owner, room, s, s.plus(H));

        mvc.perform(get("/api/v1/bookings/" + id).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(post("/api/v1/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
    }

    @Test
    void unknownBookingIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/bookings/99999999").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
        mvc.perform(post("/api/v1/bookings/99999999/cancel").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void calendarFiltersByRoomAndRangeAndHidesCancelled() throws Exception {
        String user = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        long otherRoom = createRoom(10);
        Instant day = BASE.plus(Duration.ofDays(20));
        bookId(user, room, day, day.plus(H));
        long cancelled = bookId(user, room, day.plus(Duration.ofHours(2)), day.plus(Duration.ofHours(3)));
        bookId(user, room, day.plus(Duration.ofDays(2)), day.plus(Duration.ofDays(2)).plus(H));
        bookId(user, otherRoom, day, day.plus(H));
        mvc.perform(post("/api/v1/bookings/" + cancelled + "/cancel").header(HttpHeaders.AUTHORIZATION, user));

        String url = "/api/v1/bookings?roomId=" + room + "&from=" + day.minus(H) + "&to=" + day.plus(Duration.ofDays(1));
        mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].roomId").value(room));

        mvc.perform(get(url + "&includeCancelled=true").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void mineReturnsOnlyOwnBookings() throws Exception {
        String a = TestAuth.newUserToken(mvc);
        String b = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(30));
        bookId(a, room, s, s.plus(H));
        bookId(b, room, s.plus(Duration.ofHours(2)), s.plus(Duration.ofHours(3)));

        mvc.perform(get("/api/v1/bookings?mine=true&roomId=" + room).header(HttpHeaders.AUTHORIZATION, a))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/bookings?roomId=" + room).header(HttpHeaders.AUTHORIZATION, a))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void invalidRangeIsBadRequest() throws Exception {
        String user = TestAuth.newUserToken(mvc);
        mvc.perform(get("/api/v1/bookings?from=" + BASE.plus(H) + "&to=" + BASE).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/bookings?from=garbage").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availabilityExcludesBusyInactiveAndSmallRooms() throws Exception {
        String user = TestAuth.newUserToken(mvc);
        Instant s = BASE.plus(Duration.ofDays(40));
        long busy = createRoom(10);
        long free = createRoom(10);
        long small = createRoom(2);
        long inactive = createRoom(10);
        bookId(user, busy, s, s.plus(H));
        mvc.perform(patch("/api/v1/rooms/" + inactive).header(HttpHeaders.AUTHORIZATION, admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());

        String body = mvc.perform(get("/api/v1/rooms/available?minCapacity=5&from=" + s.plus(Duration.ofMinutes(30))
                        + "&to=" + s.plus(Duration.ofMinutes(90))).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"id\":" + free + ",").doesNotContain("\"id\":" + busy + ",")
                .doesNotContain("\"id\":" + small + ",").doesNotContain("\"id\":" + inactive + ",");

        // back-to-back: the busy room is free right after its booking ends
        String after = mvc.perform(get("/api/v1/rooms/available?from=" + s.plus(H) + "&to=" + s.plus(H).plus(H))
                .header(HttpHeaders.AUTHORIZATION, user)).andReturn().getResponse().getContentAsString();
        assertThat(after).contains("\"id\":" + busy + ",");
    }

    @Test
    void availabilityRequiresValidRange() throws Exception {
        String user = TestAuth.newUserToken(mvc);
        mvc.perform(get("/api/v1/rooms/available?from=" + BASE + "&to=" + BASE).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/rooms/available").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotDeactivateRoomWithUpcomingBookings() throws Exception {
        String user = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(50));
        long id = bookId(user, room, s, s.plus(H));
        mvc.perform(patch("/api/v1/rooms/" + room).header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_HAS_UPCOMING_BOOKINGS"));

        mvc.perform(post("/api/v1/bookings/" + id + "/cancel").header(HttpHeaders.AUTHORIZATION, user));
        mvc.perform(patch("/api/v1/rooms/" + room).header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
    }
}
