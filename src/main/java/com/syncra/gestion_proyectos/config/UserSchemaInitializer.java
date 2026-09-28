package com.syncra.gestion_proyectos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(0)
public class UserSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'users' "
                        + "AND column_name = 'has_seen_onboarding'",
                Integer.class);

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN has_seen_onboarding BOOLEAN NOT NULL DEFAULT FALSE");
        }
    }
}
