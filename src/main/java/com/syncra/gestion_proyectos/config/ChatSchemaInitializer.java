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
        addColumnIfMissing("project_chat_messages", "type", "VARCHAR(20) NOT NULL DEFAULT 'TEXT'");
        addColumnIfMissing("private_messages", "type", "VARCHAR(20) NOT NULL DEFAULT 'TEXT'");
        addColumnIfMissing("project_chat_messages", "reply_to_message_id", "BIGINT NULL");
        addColumnIfMissing("private_messages", "reply_to_message_id", "BIGINT NULL");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            Integer columnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                    Integer.class,
                    table, column);

            if (columnCount != null && columnCount == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        } catch (Exception exception) {
            System.err.println("No se pudo actualizar el esquema de " + table + ": " + exception.getMessage());
        }
    }
}
