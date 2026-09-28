package com.syncra.gestion_proyectos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(1)
public class TaskSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer completedAtColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'tasks' "
                        + "AND column_name = 'completed_at'",
                Integer.class);

        if (completedAtColumnCount != null && completedAtColumnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN completed_at DATETIME NULL");
        }

        Integer dueTimeColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'tasks' "
                + "AND column_name = 'due_time'",
            Integer.class);

        if (dueTimeColumnCount != null && dueTimeColumnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN due_time TIME NULL");
        }
    }
}