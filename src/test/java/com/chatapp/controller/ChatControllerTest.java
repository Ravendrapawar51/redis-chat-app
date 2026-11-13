package com.chatapp.controller;

import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChatController.
 * Tests REST API endpoints with mocked service layer.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    private static final String ROOM_ID = "general";
    private static final String ROOM_NAME = "General";
    private static final String PARTICIPANT = "guest_user";
    private static final String MESSAGE_TEXT = "Hello, everyone!";

    @BeforeEach
    void setUp() {
        // Reset any state if needed
    }

    @Test
    void testCreateChatRoom_Success() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest(ROOM_NAME);
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(ROOM_ID)
                .roomName(ROOM_NAME)
                .createdAt(LocalDateTime.now())
                .participantCount(0)
                .build();
        
        when(chatService.createChatRoom(ROOM_NAME)).thenReturn(chatRoom);

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Chat room 'General' created successfully."))
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID));
    }

    @Test
    void testCreateChatRoom_Duplicate() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest(ROOM_NAME);
        when(chatService.createChatRoom(ROOM_NAME)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'General' already exists."));
    }

    @Test
    void testCreateChatRoom_ValidationFailed() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest(""); // Empty room name

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testJoinChatRoom_Success() throws Exception {
        // Arrange
        JoinRoomRequest request = new JoinRoomRequest(PARTICIPANT);
        when(chatService.roomExists(ROOM_ID)).thenReturn(true);
        when(chatService.joinChatRoom(ROOM_ID, PARTICIPANT)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User 'guest_user' joined chat room 'general'."));
    }

    @Test
    void testJoinChatRoom_RoomNotExists() throws Exception {
        // Arrange
        JoinRoomRequest request = new JoinRoomRequest(PARTICIPANT);
        when(chatService.roomExists(ROOM_ID)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' does not exist."));
    }

    @Test
    void testJoinChatRoom_ValidationFailed() throws Exception {
        // Arrange
        JoinRoomRequest request = new JoinRoomRequest(""); // Empty participant name

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMessage_Success() throws Exception {
        // Arrange
        SendMessageRequest request = new SendMessageRequest(PARTICIPANT, MESSAGE_TEXT);
        when(chatService.roomExists(ROOM_ID)).thenReturn(true);
        when(chatService.sendMessage(ROOM_ID, PARTICIPANT, MESSAGE_TEXT)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Message sent successfully."));
    }

    @Test
    void testSendMessage_RoomNotExists() throws Exception {
        // Arrange
        SendMessageRequest request = new SendMessageRequest(PARTICIPANT, MESSAGE_TEXT);
        when(chatService.roomExists(ROOM_ID)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' does not exist."));
    }

    @Test
    void testSendMessage_ValidationFailed() throws Exception {
        // Arrange
        SendMessageRequest request = new SendMessageRequest("", MESSAGE_TEXT); // Empty participant

        // Act & Assert
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetMessages_Success() throws Exception {
        // Arrange
        Message message = Message.builder()
                .participant(PARTICIPANT)
                .message(MESSAGE_TEXT)
                .timestamp(LocalDateTime.now())
                .build();
        
        List<Message> messages = List.of(message);
        
        when(chatService.roomExists(ROOM_ID)).thenReturn(true);
        when(chatService.getMessages(ROOM_ID, 10)).thenReturn(messages);

        // Act & Assert
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages[0].participant").value(PARTICIPANT))
                .andExpect(jsonPath("$.data.messages[0].message").value(MESSAGE_TEXT));
    }

    @Test
    void testGetMessages_RoomNotExists() throws Exception {
        // Arrange
        when(chatService.roomExists(ROOM_ID)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' does not exist."));
    }

    @Test
    void testGetChatRoom_Success() throws Exception {
        // Arrange
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(ROOM_ID)
                .roomName(ROOM_NAME)
                .createdAt(LocalDateTime.now())
                .participantCount(1)
                .build();
        
        when(chatService.getChatRoom(ROOM_ID)).thenReturn(chatRoom);

        // Act & Assert
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.data.roomName").value(ROOM_NAME));
    }

    @Test
    void testGetChatRoom_NotFound() throws Exception {
        // Arrange
        when(chatService.getChatRoom(ROOM_ID)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}", ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' does not exist."));
    }

    @Test
    void testDeleteChatRoom_Success() throws Exception {
        // Arrange
        when(chatService.roomExists(ROOM_ID)).thenReturn(true);
        when(chatService.deleteChatRoom(ROOM_ID)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/chatapp/chatrooms/{roomId}", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' deleted successfully."));
    }

    @Test
    void testDeleteChatRoom_NotFound() throws Exception {
        // Arrange
        when(chatService.roomExists(ROOM_ID)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/chatapp/chatrooms/{roomId}", ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' does not exist."));
    }
}
