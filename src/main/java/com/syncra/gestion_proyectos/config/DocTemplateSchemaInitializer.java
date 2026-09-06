package com.syncra.gestion_proyectos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(1)
public class DocTemplateSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'doc_templates' "
                        + "AND column_name = 'published'",
                Integer.class);

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE doc_templates ADD COLUMN published BOOLEAN NOT NULL DEFAULT TRUE");
        }

        Integer parentTemplateColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'doc_templates' "
                + "AND column_name = 'parent_template_id'",
            Integer.class);

        if (parentTemplateColumnCount != null && parentTemplateColumnCount == 0) {
            jdbcTemplate.execute(
                "ALTER TABLE doc_templates ADD COLUMN parent_template_id BIGINT NULL");
        }
    }
}