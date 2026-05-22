package com.example.mentalcompanion.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("knowledgeSchemaInitializer")
public class KnowledgeSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_chunk (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    document_id BIGINT NOT NULL,
                    document_name VARCHAR(255) NOT NULL,
                    chunk_index INT NOT NULL,
                    content MEDIUMTEXT NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_knowledge_chunk_doc (document_id),
                    INDEX idx_knowledge_chunk_time (create_time)
                )
                """);
    }
}
