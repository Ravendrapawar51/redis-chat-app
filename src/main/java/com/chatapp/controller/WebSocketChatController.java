package com.chatapp.controller;

import com.chatapp.dto.SendMessageRequest;
import com.chatapp.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * Controller for handling incoming WebSocket messages via STOMP.
 * Receives messages sent to /app/chat/{roomId}/send and processes them through ChatService.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final ChatService chatService;

    /**
     * Handle message sent over WebSocket.
     * Destination: /app/chat/{roomId}/send
     * Broadcast destination: /topic/{roomId} (handled via Redis Pub/Sub -> RedisMessageSubscriber)
     */
    @MessageMapping("/chat/{roomId}/send")
    public void handleChatMessage(
            @DestinationVariable String roomId,
            @Payload SendMessageRequest request) {

        log.info("Received WebSocket chat message for room {}: from={}", roomId, request.getParticipant());
        
        // Auto-create room if it doesn't exist yet in Redis
        if (!chatService.roomExists(roomId)) {
            log.info("Room '{}' does not exist in Redis. Creating it automatically...", roomId);
            chatService.createChatRoom(roomId);
        }
        
        // Ensure participant has joined
        chatService.joinChatRoom(roomId, request.getParticipant());
        
        boolean sent = chatService.sendMessage(roomId, request.getParticipant(), request.getMessage());
        log.info("Message sent to room '{}' success={}", roomId, sent);
    }
}
