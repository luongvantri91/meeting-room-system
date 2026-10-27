package com.company.meetingroom.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.meetingroom.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

class OperationalTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void responseCarriesGeneratedRequestId() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void safeIncomingRequestIdIsEchoedAndUnsafeOneReplaced() throws Exception {
        mvc.perform(get("/actuator/health").header("X-Request-Id", "abc-123"))
                .andExpect(header().string("X-Request-Id", "abc-123"));
        mvc.perform(get("/actuator/health").header("X-Request-Id", "bad id with spaces"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.not("bad id with spaces")));
    }

    @Test
    void healthReportsDatabaseUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void invalidTokenIsTreatedAsAnonymous() throws Exception {
        mvc.perform(get("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
