package com.company.meetingroom.room;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.meetingroom.AbstractIntegrationTest;
import com.company.meetingroom.TestAuth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class RoomApiTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static String room(String name, int capacity) {
        return "{\"name\":\"" + name + "\",\"capacity\":" + capacity + ",\"location\":\"Floor 3\"}";
    }

    private static String unique() {
        return "Room-" + UUID.randomUUID();
    }

    @Test
    void adminCreatesAndReadsRoom() throws Exception {
        String admin = TestAuth.adminToken(mvc);
        String name = unique();
        String body = mvc.perform(post("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(room(name, 8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(get("/api/v1/rooms/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.capacity").value(8));
    }

    @Test
    void regularUserCannotCreateButCanRead() throws Exception {
        String user = TestAuth.newUserToken(mvc);
        mvc.perform(post("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON).content(room(unique(), 4)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, user)).andExpect(status().isOk());
    }

    @Test
    void invalidRoomIsRejected() throws Exception {
        String admin = TestAuth.adminToken(mvc);
        mvc.perform(post("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(room("", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.capacity").exists());
    }

    @Test
    void duplicateNameIsConflictIgnoringCase() throws Exception {
        String admin = TestAuth.adminToken(mvc);
        String name = unique();
        mvc.perform(post("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(room(name, 5))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(room(name.toUpperCase(), 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_NAME_ALREADY_EXISTS"));
    }

    @Test
    void unknownRoomIsNotFound() throws Exception {
        String admin = TestAuth.adminToken(mvc);
        mvc.perform(get("/api/v1/rooms/999999").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void patchUpdatesOnlyProvidedFieldsAndCanDeactivate() throws Exception {
        String admin = TestAuth.adminToken(mvc);
        String name = unique();
        String created = mvc.perform(post("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(room(name, 6)))
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(patch("/api/v1/rooms/" + id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false,\"capacity\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void invalidPaginationIsBadRequest() throws Exception {
        String admin = TestAuth.adminToken(mvc);
        mvc.perform(get("/api/v1/rooms?size=1000").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest());
    }
}
