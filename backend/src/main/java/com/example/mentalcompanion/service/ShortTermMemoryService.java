package com.example.mentalcompanion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShortTermMemoryService {

    private static final int MAX_TURNS = 10;
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ShortTermMemoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String recentContext(Long userId, Long sessionId) {
        try {
            List<String> turns = redisTemplate.opsForList().range(key(userId, sessionId), 0, -1);
            if (turns == null || turns.isEmpty()) {
                return "暂无短期记忆。";
            }
            StringBuilder builder = new StringBuilder();
            for (String item : turns) {
                MemoryTurn turn = objectMapper.readValue(item, MemoryTurn.class);
                builder.append("用户：").append(turn.userMessage()).append('\n');
                builder.append("助手：").append(turn.aiReply()).append("\n\n");
            }
            return builder.toString();
        } catch (Exception ex) {
            return "短期记忆暂不可用。";
        }
    }

    public void appendTurn(Long userId, Long sessionId, String userMessage, String aiReply) {
        try {
            String value = objectMapper.writeValueAsString(new MemoryTurn(userMessage, aiReply, LocalDateTime.now().toString()));
            String key = key(userId, sessionId);
            redisTemplate.opsForList().rightPush(key, value);
            redisTemplate.opsForList().trim(key, -MAX_TURNS, -1);
            redisTemplate.expire(key, TTL);
        } catch (JsonProcessingException ignored) {
            // Memory must not break the chat workflow.
        } catch (RuntimeException ignored) {
            // Redis can be temporarily unavailable during local demos.
        }
    }

    private String key(Long userId, Long sessionId) {
        return "chat:memory:" + userId + ":" + sessionId;
    }

    private record MemoryTurn(String userMessage, String aiReply, String createTime) {
    }
}

