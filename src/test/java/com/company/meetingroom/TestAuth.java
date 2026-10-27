package com.company.meetingroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Test helper that obtains JWTs through the real HTTP API. */
public final class TestAuth {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestAuth() {
    }

    public static String adminToken(MockMvc mvc) throws Exception {
        return login(mvc, "admin@test.local", "AdminPass123!");
    }

    public static String newUserToken(MockMvc mvc) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.local";
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123\",\"fullName\":\"Test User\"}"));
        return login(mvc, email, "Password123");
    }

    public static String login(MockMvc mvc, String email, String password) throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = MAPPER.readTree(body);
        return "Bearer " + node.get("accessToken").asText();
    }
}
