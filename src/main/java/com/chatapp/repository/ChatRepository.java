package com.chatapp.repository;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Repository class for managing chat room data in Redis.
 * Uses different Redis data structures:
 * - Hash: Store chat room metadata
 * - Set: Store participants in a chat room
 * - List: Store chat messages in chronological order
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ChatRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis key prefixes
    private static final String ROOM_KEY_PREFIX = "chat:room:";
    private static final String PARTICIPANTS_KEY_PREFIX = "chat:participants:";
    private static final String MESSAGES_KEY_PREFIX = "chat:messages:";

    /**
     * Create a new chat room.
     * Uses Redis Hash to store room metadata.
     *
     * @param roomId   unique room identifier
     * @param roomName display name of the room
     * @return true if room created successfully, false if room already exists
     */
    public boolean createChatRoom(String roomId, String roomName) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        
        // Check if room already exists
        if (Boolean.TRUE.equals(redisTemplate.hasKey(roomKey))) {
            log.warn("Chat room already exists: {}", roomId);
            return false;
        }
        
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .roomName(roomName)
                .createdAt(LocalDateTime.now())
                .participantCount(0)
                .build();
        
        redisTemplate.opsForHash().put(roomKey, "roomId", chatRoom.getRoomId());
        redisTemplate.opsForHash().put(roomKey, "roomName", chatRoom.getRoomName());
        redisTemplate.opsForHash().put(roomKey, "createdAt", chatRoom.getCreatedAt().toString());
        redisTemplate.opsForHash().put(roomKey, "participantCount", "0");
        
        log.info("Chat room created: {}", roomId);
        return true;
    }

    /**
     * Check if a chat room exists.
     *
     * @param roomId room identifier
     * @return true if room exists
     */
    public boolean roomExists(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(roomKey));
    }

    /**
     * Get chat room metadata.
     *
     * @param roomId room identifier
     * @return ChatRoom object or null if not found
     */
    public ChatRoom getChatRoom(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(roomKey))) {
            return null;
        }
        
        String roomName = (String) redisTemplate.opsForHash().get(roomKey, "roomName");
        String createdAt = (String) redisTemplate.opsForHash().get(roomKey, "createdAt");
        String participantCount = (String) redisTemplate.opsForHash().get(roomKey, "participantCount");
        
        return ChatRoom.builder()
                .roomId(roomId)
                .roomName(roomName)
                .createdAt(LocalDateTime.parse(createdAt))
                .participantCount(Integer.parseInt(participantCount))
                .build();
    }

    /**
     * Add a participant to a chat room.
     * Uses Redis Set to store unique participants.
     *
     * @param roomId      room identifier
     * @param participant participant name
     * @return true if participant added, false if already in room
     */
    public boolean addParticipant(String roomId, String participant) {
        if (!roomExists(roomId)) {
            log.warn("Cannot add participant to non-existent room: {}", roomId);
            return false;
        }
        
        String participantsKey = PARTICIPANTS_KEY_PREFIX + roomId;
        Long added = redisTemplate.opsForSet().add(participantsKey, participant);
        
        if (added != null && added > 0) {
            // Update participant count in room metadata
            String roomKey = ROOM_KEY_PREFIX + roomId;
            Long count = redisTemplate.opsForSet().size(participantsKey);
            if (count != null) {
                redisTemplate.opsForHash().put(roomKey, "participantCount", count.toString());
            }
            log.info("Participant {} added to room {}", participant, roomId);
            return true;
        }
        
        return false;
    }

    /**
     * Check if a participant is in a chat room.
     *
     * @param roomId      room identifier
     * @param participant participant name
     * @return true if participant is in the room
     */
    public boolean isParticipantInRoom(String roomId, String participant) {
        String participantsKey = PARTICIPANTS_KEY_PREFIX + roomId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(participantsKey, participant));
    }

    /**
     * Get all participants in a chat room.
     *
     * @param roomId room identifier
     * @return Set of participant names
     */
    public Set<Object> getParticipants(String roomId) {
        String participantsKey = PARTICIPANTS_KEY_PREFIX + roomId;
        return redisTemplate.opsForSet().members(participantsKey);
    }

    /**
     * Send a message to a chat room.
     * Uses Redis List to store messages in chronological order.
     *
     * @param roomId   room identifier
     * @param message  message object
     * @return true if message sent successfully
     */
    public boolean sendMessage(String roomId, Message message) {
        if (!roomExists(roomId)) {
            log.warn("Cannot send message to non-existent room: {}", roomId);
            return false;
        }
        
        String messagesKey = MESSAGES_KEY_PREFIX + roomId;
        
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(messagesKey, messageJson);
            
            log.info("Message saved to room {} by {}", roomId, message.getParticipant());
            return true;
        } catch (JsonProcessingException e) {
            log.error("Error serializing message", e);
            return false;
        }
    }

    /**
     * Retrieve the last N messages from a chat room.
     * Uses Redis List range operation to get messages from the end.
     *
     * @param roomId room identifier
     * @param limit  number of messages to retrieve
     * @return List of messages
     */
    @SuppressWarnings("unchecked")
    public List<Message> getMessages(String roomId, int limit) {
        String messagesKey = MESSAGES_KEY_PREFIX + roomId;
        
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(messagesKey))) {
            return List.of();
        }
        
        Long size = redisTemplate.opsForList().size(messagesKey);
        if (size == null || size == 0) {
            return List.of();
        }
        
        // Get the last 'limit' messages
        long start = Math.max(0, size - limit);
        long end = size - 1;
        
        List<Object> messageObjects = redisTemplate.opsForList().range(messagesKey, start, end);
        
        if (messageObjects == null || messageObjects.isEmpty()) {
            return List.of();
        }
        
        try {
            return messageObjects.stream()
                    .map(obj -> {
                        try {
                            return objectMapper.readValue(obj.toString(), Message.class);
                        } catch (JsonProcessingException e) {
                            log.error("Error deserializing message", e);
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .toList();
        } catch (Exception e) {
            log.error("Error processing messages", e);
            return List.of();
        }
    }

    /**
     * Delete a chat room and all associated data.
     *
     * @param roomId room identifier
     * @return true if room deleted successfully
     */
    public boolean deleteChatRoom(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        String participantsKey = PARTICIPANTS_KEY_PREFIX + roomId;
        String messagesKey = MESSAGES_KEY_PREFIX + roomId;
        
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(roomKey))) {
            return false;
        }
        
        redisTemplate.delete(roomKey);
        redisTemplate.delete(participantsKey);
        redisTemplate.delete(messagesKey);
        
        log.info("Chat room deleted: {}", roomId);
        return true;
    }

    /**
     * Get the total number of messages in a room.
     *
     * @param roomId room identifier
     * @return message count
     */
    public long getMessageCount(String roomId) {
        String messagesKey = MESSAGES_KEY_PREFIX + roomId;
        Long size = redisTemplate.opsForList().size(messagesKey);
        return size != null ? size : 0;
    }

    /**
     * Get the participant count for a room.
     *
     * @param roomId room identifier
     * @return participant count
     */
    public long getParticipantCount(String roomId) {
        String participantsKey = PARTICIPANTS_KEY_PREFIX + roomId;
        Long size = redisTemplate.opsForSet().size(participantsKey);
        return size != null ? size : 0;
    }
}
