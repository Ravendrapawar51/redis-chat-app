package com.chatapp.exception;

/**
 * Exception thrown when attempting to create a duplicate chat room.
 */
public class DuplicateChatRoomException extends RuntimeException {
    
    public DuplicateChatRoomException(String roomName) {
        super("Chat room '" + roomName + "' already exists.");
    }
}
