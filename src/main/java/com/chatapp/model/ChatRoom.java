package com.chatapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * ChatRoom model representing a chat room with metadata.
 * Contains room ID, name, creation timestamp, and participant count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    
    private String roomId;
    private String roomName;
    private LocalDateTime createdAt;
    private Integer participantCount;
}
