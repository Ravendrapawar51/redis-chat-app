package com.chatapp.service;

import com.chatapp.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Redis message subscriber for handling real-time chat messages.
 * Implements MessageListener interface to receive messages from Redis Pub/Sub channels
 * and broadcasts them to connected WebSocket clients subscribed to /topic/{roomId}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String CHANNEL_PREFIX = "chat:";

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());
            
            Message chatMessage;
            if (messageBody.trim().startsWith("\"")) {
                // RedisTemplate double-serialized the JSON string
                String json = objectMapper.readValue(messageBody, String.class);
                chatMessage = objectMapper.readValue(json, Message.class);
            } else {
                chatMessage = objectMapper.readValue(messageBody, Message.class);
            }
            
            log.info("Received message on Redis channel {}: from={}, content={}", 
                    channel, chatMessage.getParticipant(), chatMessage.getMessage());
            
            // Extract roomId from channel name (e.g., "chat:general" -> "general")
            String roomId = channel.startsWith(CHANNEL_PREFIX) 
                    ? channel.substring(CHANNEL_PREFIX.length()) 
                    : channel;
            
            // Broadcast to connected WebSocket subscribers at /topic/{roomId}
            String destination = "/topic/" + roomId;
            messagingTemplate.convertAndSend(destination, chatMessage);
            log.info("Forwarded message to WebSocket topic: {}", destination);
            
        } catch (IOException e) {
            log.error("Error processing message from Redis", e);
        }
    }
}
