package com.chatapp.exception;

/**
 * Exception thrown when a chat room is not found.
 */
public class ChatRoomNotFoundException extends RuntimeException {
    
    public ChatRoomNotFoundException(String roomId) {
        super("Chat room '" + roomId + "' does not exist.");
    }
    
    public ChatRoomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
