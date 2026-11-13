package com.chatapp.service;

import com.chatapp.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis message publisher for broadcasting chat messages.
 * Publishes messages to Redis Pub/Sub channels for real-time distribution.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL_PREFIX = "chat:";

    /**
     * Publish a message to a specific chat room's Pub/Sub channel.
     *
     * @param roomId  room identifier
     * @param message message to publish
     */
    public void publish(String roomId, Message message) {
        try {
            String channel = CHANNEL_PREFIX + roomId;
            String messageJson = objectMapper.writeValueAsString(message);
            
            redisTemplate.convertAndSend(channel, messageJson);
            
            log.info("Published message to channel {}: from={}", channel, message.getParticipant());
        } catch (JsonProcessingException e) {
            log.error("Error serializing message for publishing", e);
        }
    }
}
