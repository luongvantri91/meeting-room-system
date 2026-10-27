package com.company.meetingroom.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.meetingroom.AbstractIntegrationTest;
import com.company.meetingroom.TestAuth;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class BookingCreateApiTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    /** Base instant far enough in the future and aligned to the hour. */
    static final Instant BASE = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(Duration.ofHours(9));

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

    @Test
    void createsValidBookingForAuthenticatedUser() throws Exception {
        String token = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        mvc.perform(post("/api/v1/bookings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking(room, 5, BASE, BASE.plus(Duration.ofHours(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(room));
    }

    @Test
    void overlapScenariosAreRejectedButAdjacentIsAllowed() throws Exception {
        String token = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(1));
        Instant e = s.plus(Duration.ofHours(2));
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, s, e)).isEqualTo(201);

        // identical, partial left, partial right, contained, containing
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, s, e)).isEqualTo(409);
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, s.minus(Duration.ofHours(1)), s.plus(Duration.ofMinutes(30)))).isEqualTo(409);
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, e.minus(Duration.ofMinutes(30)), e.plus(Duration.ofHours(1)))).isEqualTo(409);
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, s.plus(Duration.ofMinutes(30)), e.minus(Duration.ofMinutes(30)))).isEqualTo(409);
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, s.minus(Duration.ofHours(1)), e.plus(Duration.ofHours(1)))).isEqualTo(409);

        // back-to-back meetings on both sides
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, e, e.plus(Duration.ofHours(1)))).isEqualTo(201);
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, s.minus(Duration.ofHours(1)), s)).isEqualTo(201);
    }

    @Test
    void sameSlotInDifferentRoomsIsAllowed() throws Exception {
        String token = TestAuth.newUserToken(mvc);
        long a = createRoom(10);
        long b = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(2));
        org.assertj.core.api.Assertions.assertThat(book(token, a, 2, s, s.plus(Duration.ofHours(1)))).isEqualTo(201);
        org.assertj.core.api.Assertions.assertThat(book(token, b, 2, s, s.plus(Duration.ofHours(1)))).isEqualTo(201);
    }

    @Test
    void validationAndBusinessErrors() throws Exception {
        String token = TestAuth.newUserToken(mvc);
        long room = createRoom(4);
        Instant s = BASE.plus(Duration.ofDays(3));

        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, s, s)).isEqualTo(400);
        org.assertj.core.api.Assertions.assertThat(book(token, room, 5, s, s.plus(Duration.ofHours(1)))).isEqualTo(422);
        org.assertj.core.api.Assertions.assertThat(book(token, 987654, 2, s, s.plus(Duration.ofHours(1)))).isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(book(token, room, 2, Instant.now().minus(Duration.ofDays(1)),
                Instant.now().minus(Duration.ofDays(1)).plus(Duration.ofHours(1)))).isEqualTo(422);

        mvc.perform(post("/api/v1/bookings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":" + room + ",\"title\":\"x\",\"attendees\":2,\"startTime\":\"nonsense\",\"endTime\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(booking(room, 2, s, s.plus(Duration.ofHours(1)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inactiveRoomCannotBeBooked() throws Exception {
        String token = TestAuth.newUserToken(mvc);
        long room = createRoom(10);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/rooms/" + room)
                .header(HttpHeaders.AUTHORIZATION, admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isOk());
        Instant s = BASE.plus(Duration.ofDays(4));
        mvc.perform(post("/api/v1/bookings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking(room, 2, s, s.plus(Duration.ofHours(1)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ROOM_INACTIVE"));
    }

    @Test
    void concurrentRequestsForSameSlotYieldExactlyOneBooking() throws Exception {
        long room = createRoom(10);
        Instant s = BASE.plus(Duration.ofDays(5));
        Instant e = s.plus(Duration.ofHours(1));
        int threads = 12;
        List<String> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tokens.add(TestAuth.newUserToken(mvc));
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new java.util.ArrayList<>();
        for (String token : tokens) {
            Callable<Integer> task = () -> {
                ready.countDown();
                go.await();
                return book(token, room, 2, s, e);
            };
            results.add(pool.submit(task));
        }
        ready.await();
        go.countDown();

        int created = 0;
        int conflicts = 0;
        for (Future<Integer> f : results) {
            int code = f.get();
            if (code == 201) {
                created++;
            } else if (code == 409) {
                conflicts++;
            }
        }
        pool.shutdown();

        org.assertj.core.api.Assertions.assertThat(created).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(conflicts).isEqualTo(threads - 1);
    }
}
