package com.chatapp.controller;

import com.chatapp.dto.*;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for chat application endpoints.
 * Provides APIs for creating rooms, joining rooms, sending messages, and
 * retrieving history.
 */
@RestController
@RequestMapping("/api/chatapp/chatrooms")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Create a new chat room.
     * POST /api/chatapp/chatrooms
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createChatRoom(
            @Valid @RequestBody CreateRoomRequest request) {

        log.info("Creating chat room: {}", request.getRoomName());

        ChatRoom chatRoom = chatService.createChatRoom(request.getRoomName());

        if (chatRoom == null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Chat room '" + request.getRoomName() + "' already exists."));
        }

        Map<String, String> responseData = new HashMap<>();
        responseData.put("roomId", chatRoom.getRoomId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Chat room '" + request.getRoomName() + "' created successfully.",
                        responseData));
    }

    /**
     * Join a chat room.
     * POST /api/chatapp/chatrooms/{roomId}/join
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<Void>> joinChatRoom(
            @PathVariable String roomId,
            @Valid @RequestBody JoinRoomRequest request) {

        log.info("User {} joining room {}", request.getParticipant(), roomId);

        if (!chatService.roomExists(roomId)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Chat room '" + roomId + "' does not exist."));
        }

        boolean joined = chatService.joinChatRoom(roomId, request.getParticipant());

        if (joined) {
            return ResponseEntity.ok(
                    ApiResponse.success("User '" + request.getParticipant() + "' joined chat room '" + roomId + "'."));
        } else {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("User '" + request.getParticipant() + "' is already in the room."));
        }
    }

    /**
     * Send a message to a chat room.
     * POST /api/chatapp/chatrooms/{roomId}/messages
     */
    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<Void>> sendMessage(
            @PathVariable String roomId,
            @Valid @RequestBody SendMessageRequest request) {

        log.info("Sending message from {} to room {}", request.getParticipant(), roomId);

        if (!chatService.roomExists(roomId)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Chat room '" + roomId + "' does not exist."));
        }

        boolean sent = chatService.sendMessage(roomId, request.getParticipant(), request.getMessage());

        if (sent) {
            return ResponseEntity.ok(ApiResponse.success("Message sent successfully."));
        } else {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send message."));
        }
    }

    /**
     * Retrieve chat history from a room.
     * GET /api/chatapp/chatrooms/{roomId}/messages?limit=N
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<Map<String, List<Message>>>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Retrieving messages from room {} with limit {}", roomId, limit);

        if (!chatService.roomExists(roomId)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Chat room '" + roomId + "' does not exist."));
        }

        List<Message> messages = chatService.getMessages(roomId, limit);

        Map<String, List<Message>> responseData = new HashMap<>();
        responseData.put("messages", messages);

        return ResponseEntity.ok(
                ApiResponse.success("Messages retrieved successfully.", responseData));
    }

    /**
     * Get chat room information.
     * GET /api/chatapp/chatrooms/{roomId}
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoom>> getChatRoom(@PathVariable String roomId) {
        log.info("Retrieving chat room: {}", roomId);

        ChatRoom chatRoom = chatService.getChatRoom(roomId);

        if (chatRoom == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Chat room '" + roomId + "' does not exist."));
        }

        return ResponseEntity.ok(ApiResponse.success("Chat room retrieved successfully.", chatRoom));
    }

    /**
     * Delete a chat room (optional feature).
     * DELETE /api/chatapp/chatrooms/{roomId}
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatRoom(@PathVariable String roomId) {
        log.info("Deleting room {}", roomId);

        if (!chatService.roomExists(roomId)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Chat room '" + roomId + "' does not exist."));
        }

        boolean deleted = chatService.deleteChatRoom(roomId);

        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Chat room '" + roomId + "' deleted successfully."));
        } else {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete chat room."));
        }
    }
}
