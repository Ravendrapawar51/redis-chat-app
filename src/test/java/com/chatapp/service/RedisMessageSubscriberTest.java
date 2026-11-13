package com.chatapp.service;

import com.chatapp.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisMessageSubscriberTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RedisMessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
    }

    @Test
    void testOnMessage_BroadcastsToWebSocketTopic() throws Exception {
        // Arrange
        String roomId = "general";
        String channel = "chat:" + roomId;
        Message chatMessage = Message.builder()
                .participant("john_doe")
                .message("Hello WebSocket!")
                .timestamp(LocalDateTime.now())
                .build();

        byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
        byte[] bodyBytes = objectMapper.writeValueAsBytes(chatMessage);
        org.springframework.data.redis.connection.Message redisMessage =
                new DefaultMessage(channelBytes, bodyBytes);

        // Act
        subscriber.onMessage(redisMessage, null);

        // Assert
        verify(messagingTemplate).convertAndSend(eq("/topic/general"), (Message) argThat(m ->
                m instanceof Message msg &&
                "john_doe".equals(msg.getParticipant()) &&
                "Hello WebSocket!".equals(msg.getMessage())));
    }

    @Test
    void testOnMessage_DoubleSerializedPayload_BroadcastsToWebSocketTopic() throws Exception {
        // Arrange
        String roomId = "general";
        String channel = "chat:" + roomId;
        Message chatMessage = Message.builder()
                .participant("alice")
                .message("Double serialized message")
                .timestamp(LocalDateTime.now())
                .build();

        byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
        // Simulate RedisTemplate GenericJackson2JsonRedisSerializer serializing a String
        String innerJson = objectMapper.writeValueAsString(chatMessage);
        byte[] doubleSerializedBytes = objectMapper.writeValueAsBytes(innerJson);
        org.springframework.data.redis.connection.Message redisMessage =
                new DefaultMessage(channelBytes, doubleSerializedBytes);

        // Act
        subscriber.onMessage(redisMessage, null);

        // Assert
        verify(messagingTemplate).convertAndSend(eq("/topic/general"), (Message) argThat(m ->
                m instanceof Message msg &&
                "alice".equals(msg.getParticipant()) &&
                "Double serialized message".equals(msg.getMessage())));
    }
}
