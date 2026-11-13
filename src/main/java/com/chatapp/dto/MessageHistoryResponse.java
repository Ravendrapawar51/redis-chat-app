package com.chatapp.dto;

import com.chatapp.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for retrieving message history from a chat room.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryResponse {
    
    private List<Message> messages;
}
