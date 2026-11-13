package com.chatapp.integration;

import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.dto.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the chat application.
 * Tests the full stack including controllers, services, and Redis repository.
 * Requires a running Redis instance or embedded Redis configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ROOM_NAME = "Test Room";
    private static final String ROOM_ID = "test_room";
    private static final String PARTICIPANT = "test_user";
    private static final String MESSAGE_TEXT = "Test message";

    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        cleanupRedis();
    }

    @AfterEach
    void tearDown() {
        // Clean up Redis after each test
        cleanupRedis();
    }

    private void cleanupRedis() {
        // Delete all chat-related keys
        redisTemplate.keys("chat:*").forEach(key -> redisTemplate.delete(key));
    }

    @Test
    void testCreateAndJoinChatRoom() throws Exception {
        // Step 1: Create a chat room
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(ROOM_NAME))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("created successfully")))
                .andExpect(jsonPath("$.data.roomId").exists());

        // Step 2: Join the chat room
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinRoomRequest(PARTICIPANT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("joined chat room")));
    }

    @Test
    void testSendAndRetrieveMessages() throws Exception {
        // Step 1: Create a chat room
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(ROOM_NAME))))
                .andExpect(status().isCreated());

        // Step 2: Join the chat room
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinRoomRequest(PARTICIPANT))))
                .andExpect(status().isOk());

        // Step 3: Send first message
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendMessageRequest(PARTICIPANT, "First message"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Step 4: Send second message
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendMessageRequest(PARTICIPANT, "Second message"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Step 5: Retrieve messages
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages", hasSize(2)))
                .andExpect(jsonPath("$.data.messages[0].message").value("First message"))
                .andExpect(jsonPath("$.data.messages[1].message").value("Second message"));
    }

    @Test
    void testSendToNonExistentRoom() throws Exception {
        // Try to send a message to a room that doesn't exist
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendMessageRequest(PARTICIPANT, MESSAGE_TEXT))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("does not exist")));
    }

    @Test
    void testDuplicateChatRoom() throws Exception {
        // Create a chat room
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(ROOM_NAME))))
                .andExpect(status().isCreated());

        // Try to create the same room again
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(ROOM_NAME))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void testDeleteChatRoom() throws Exception {
        // Create a chat room
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(ROOM_NAME))))
                .andExpect(status().isCreated());

        // Delete the chat room
        mockMvc.perform(delete("/api/chatapp/chatrooms/{roomId}", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("deleted successfully")));

        // Verify room is deleted
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}", ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testMultipleParticipants() throws Exception {
        // Create a chat room
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(ROOM_NAME))))
                .andExpect(status().isCreated());

        // Join with first participant
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinRoomRequest("user1"))))
                .andExpect(status().isOk());

        // Join with second participant
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinRoomRequest("user2"))))
                .andExpect(status().isOk());

        // Send message from first participant
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendMessageRequest("user1", "Hello from user1"))))
                .andExpect(status().isOk());

        // Send message from second participant
        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendMessageRequest("user2", "Hello from user2"))))
                .andExpect(status().isOk());

        // Retrieve messages
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(2)));
    }

    @Test
    void testMessagePagination() throws Exception {
        // Create a chat room and join
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateRoomRequest(ROOM_NAME))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/join", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinRoomRequest(PARTICIPANT))))
                .andExpect(status().isOk());

        // Send 5 messages
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new SendMessageRequest(PARTICIPANT, "Message " + i))))
                    .andExpect(status().isOk());
        }

        // Retrieve last 3 messages
        mockMvc.perform(get("/api/chatapp/chatrooms/{roomId}/messages", ROOM_ID)
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(3)))
                .andExpect(jsonPath("$.data.messages[0].message").value("Message 3"))
                .andExpect(jsonPath("$.data.messages[2].message").value("Message 5"));
    }
}
