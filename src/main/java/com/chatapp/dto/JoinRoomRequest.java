package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for joining a chat room.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {
    
    @NotBlank(message = "Participant name is required")
    private String participant;
}
