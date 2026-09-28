package com.syncra.gestion_proyectos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(2)
public class ActivityLogSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE activity_log MODIFY COLUMN description VARCHAR(1000) NOT NULL");
            addColumnIfMissing("target_snippet", "VARCHAR(255) NULL");
            addColumnIfMissing("target_section_key", "VARCHAR(255) NULL");
            addColumnIfMissing("change_kind", "VARCHAR(32) NULL");
        } catch (Exception exception) {
            System.err.println("No se pudo actualizar el esquema de activity_log: " + exception.getMessage());
        }
    }

    private void addColumnIfMissing(String columnName, String definition) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'activity_log' "
                        + "AND column_name = ?",
                Integer.class,
                columnName);

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE activity_log ADD COLUMN " + columnName + " " + definition);
        }
    }
}
