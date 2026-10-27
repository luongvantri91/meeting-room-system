package com.company.meetingroom.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.company.meetingroom.AbstractIntegrationTest;
import com.company.meetingroom.TestAuth;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

abstract class BookingApiTestBase extends AbstractIntegrationTest {

    /** Base instant far enough in the future and aligned to the hour. */
    static final Instant BASE = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
            .plus(Duration.ofHours(9));

    @Autowired
    MockMvc mvc;

    String admin() throws Exception {
        return TestAuth.adminToken(mvc);
    }

    long createRoom(int capacity) throws Exception {
        String body = mvc.perform(post("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"R-" + UUID.randomUUID() + "\",\"capacity\":" + capacity + "}"))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    static String booking(long roomId, int attendees, Instant start, Instant end) {
        return "{\"roomId\":" + roomId + ",\"title\":\"Meeting\",\"attendees\":" + attendees
                + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    int book(String token, long roomId, int attendees, Instant start, Instant end) throws Exception {
        return mvc.perform(post("/api/v1/bookings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(booking(roomId, attendees, start, end)))
                .andReturn().getResponse().getStatus();
    }

    /** Creates a booking and returns its id. */
    long bookId(String token, long roomId, Instant start, Instant end) throws Exception {
        String body = mvc.perform(post("/api/v1/bookings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(booking(roomId, 2, start, end)))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+),\"roomId.*", "$1"));
    }
}
