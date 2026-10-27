package com.company.meetingroom;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseConnectivityTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsApplyAndTablesExist() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name in ('users','rooms','bookings')",
                Integer.class);
        assertThat(tables).isEqualTo(3);
    }
}
