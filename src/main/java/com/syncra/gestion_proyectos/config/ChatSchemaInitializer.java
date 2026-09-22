package com.syncra.gestion_proyectos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(1)
public class ChatSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("project_chat_messages");
        addColumnIfMissing("private_messages");
    }

    private void addColumnIfMissing(String table) {
        try {
            Integer columnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'type'",
                    Integer.class,
                    table);

            if (columnCount != null && columnCount == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE " + table
                                + " ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'TEXT'");
            }
        } catch (Exception exception) {
            System.err.println("No se pudo actualizar el esquema de " + table + ": " + exception.getMessage());
        }
    }
}
