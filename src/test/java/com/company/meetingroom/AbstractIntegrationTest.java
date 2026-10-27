package com.company.meetingroom;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Real PostgreSQL (embedded binaries, no Docker needed) shared by all integration tests. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-123");
        registry.add("app.admin.email", () -> "admin@test.local");
        registry.add("app.admin.password", () -> "AdminPass123!");
    }
}
