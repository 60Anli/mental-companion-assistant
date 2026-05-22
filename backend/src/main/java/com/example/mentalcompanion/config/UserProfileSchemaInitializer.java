package com.example.mentalcompanion.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserProfileSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public UserProfileSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        addColumnIfMissing("real_name", "VARCHAR(64)");
        addColumnIfMissing("college", "VARCHAR(128)");
        addColumnIfMissing("department", "VARCHAR(128)");
        addColumnIfMissing("email", "VARCHAR(128)");
        fillDemoProfiles();
    }

    private void addColumnIfMissing(String columnName, String columnType) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'sys_user'
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN " + columnName + " " + columnType);
        }
    }

    private void fillDemoProfiles() {
        jdbcTemplate.update("""
                UPDATE sys_user
                SET real_name = COALESCE(real_name, '王老师'),
                    department = COALESCE(department, '学生心理健康中心')
                WHERE username = 'admin'
                """);
        jdbcTemplate.update("""
                UPDATE sys_user
                SET real_name = COALESCE(real_name, '张同学'),
                    college = COALESCE(college, '人工智能学院')
                WHERE username = 'user'
                """);
    }
}
