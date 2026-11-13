package com.chatapp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message model representing a chat message in a room.
 * Contains participant name, message content, and timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    private String participant;
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
