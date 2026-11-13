package com.chatapp.service;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Service layer for chat application business logic.
 * Handles chat room operations, messaging, and participant management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final RedisMessagePublisher messagePublisher;

    /**
     * Create a new chat room.
     *
     * @param roomName name of the room to create
     * @return created ChatRoom or null if room already exists
     */
    public ChatRoom createChatRoom(String roomName) {
        // Use roomName as roomId for simplicity (can be enhanced with UUID generation)
        String roomId = roomName.toLowerCase().replaceAll("\\s+", "_");
        
        boolean created = chatRepository.createChatRoom(roomId, roomName);
        
        if (created) {
            return chatRepository.getChatRoom(roomId);
        }
        
        log.warn("Failed to create chat room: {} (already exists)", roomName);
        return null;
    }

    /**
     * Join a chat room as a participant.
     *
     * @param roomId      room identifier
     * @param participant participant name
     * @return true if joined successfully, false otherwise
     */
    public boolean joinChatRoom(String roomId, String participant) {
        if (!chatRepository.roomExists(roomId)) {
            log.warn("Cannot join non-existent room: {}", roomId);
            return false;
        }
        
        return chatRepository.addParticipant(roomId, participant);
    }

    /**
     * Send a message to a chat room.
     *
     * @param roomId      room identifier
     * @param participant sender name
     * @param messageText message content
     * @return true if message sent successfully
     */
    public boolean sendMessage(String roomId, String participant, String messageText) {
        if (!chatRepository.roomExists(roomId)) {
            log.warn("Cannot send message to non-existent room: {}", roomId);
            return false;
        }
        
        Message message = Message.builder()
                .participant(participant)
                .message(messageText)
                .build();
        
        boolean sent = chatRepository.sendMessage(roomId, message);
        
        if (sent) {
            // Publish to Pub/Sub for real-time updates
            messagePublisher.publish(roomId, message);
        }
        
        return sent;
    }

    /**
     * Retrieve message history from a chat room.
     *
     * @param roomId room identifier
     * @param limit  maximum number of messages to retrieve
     * @return list of messages
     */
    public List<Message> getMessages(String roomId, int limit) {
        if (!chatRepository.roomExists(roomId)) {
            log.warn("Cannot retrieve messages from non-existent room: {}", roomId);
            return List.of();
        }
        
        return chatRepository.getMessages(roomId, limit);
    }

    /**
     * Get chat room information.
     *
     * @param roomId room identifier
     * @return ChatRoom or null if not found
     */
    public ChatRoom getChatRoom(String roomId) {
        return chatRepository.getChatRoom(roomId);
    }

    /**
     * Get all participants in a chat room.
     *
     * @param roomId room identifier
     * @return set of participant names
     */
    public Set<Object> getParticipants(String roomId) {
        if (!chatRepository.roomExists(roomId)) {
            return Set.of();
        }
        return chatRepository.getParticipants(roomId);
    }

    /**
     * Delete a chat room.
     *
     * @param roomId room identifier
     * @return true if deleted successfully
     */
    public boolean deleteChatRoom(String roomId) {
        return chatRepository.deleteChatRoom(roomId);
    }

    /**
     * Check if a chat room exists.
     *
     * @param roomId room identifier
     * @return true if room exists
     */
    public boolean roomExists(String roomId) {
        return chatRepository.roomExists(roomId);
    }
}
