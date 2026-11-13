package com.chatapp.service;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService.
 * Tests business logic for chat room operations, messaging, and participant management.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private RedisMessagePublisher messagePublisher;

    @InjectMocks
    private ChatService chatService;

    private static final String ROOM_ID = "general";
    private static final String ROOM_NAME = "General";
    private static final String PARTICIPANT = "guest_user";
    private static final String MESSAGE_TEXT = "Hello, everyone!";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(chatRepository, messagePublisher);
    }

    @Test
    void testCreateChatRoom_Success() {
        // Arrange
        ChatRoom expectedRoom = ChatRoom.builder()
                .roomId(ROOM_ID)
                .roomName(ROOM_NAME)
                .build();
        
        when(chatRepository.createChatRoom(anyString(), anyString())).thenReturn(true);
        when(chatRepository.getChatRoom(ROOM_ID)).thenReturn(expectedRoom);

        // Act
        ChatRoom result = chatService.createChatRoom(ROOM_NAME);

        // Assert
        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals(ROOM_NAME, result.getRoomName());
        verify(chatRepository).createChatRoom(ROOM_ID, ROOM_NAME);
        verify(chatRepository).getChatRoom(ROOM_ID);
    }

    @Test
    void testCreateChatRoom_Duplicate() {
        // Arrange
        when(chatRepository.createChatRoom(anyString(), anyString())).thenReturn(false);

        // Act
        ChatRoom result = chatService.createChatRoom(ROOM_NAME);

        // Assert
        assertNull(result);
        verify(chatRepository).createChatRoom(ROOM_ID, ROOM_NAME);
        verify(chatRepository, never()).getChatRoom(anyString());
    }

    @Test
    void testJoinChatRoom_Success() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(true);
        when(chatRepository.addParticipant(ROOM_ID, PARTICIPANT)).thenReturn(true);

        // Act
        boolean result = chatService.joinChatRoom(ROOM_ID, PARTICIPANT);

        // Assert
        assertTrue(result);
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository).addParticipant(ROOM_ID, PARTICIPANT);
    }

    @Test
    void testJoinChatRoom_RoomNotExists() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(false);

        // Act
        boolean result = chatService.joinChatRoom(ROOM_ID, PARTICIPANT);

        // Assert
        assertFalse(result);
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository, never()).addParticipant(anyString(), anyString());
    }

    @Test
    void testJoinChatRoom_AlreadyJoined() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(true);
        when(chatRepository.addParticipant(ROOM_ID, PARTICIPANT)).thenReturn(false);

        // Act
        boolean result = chatService.joinChatRoom(ROOM_ID, PARTICIPANT);

        // Assert
        assertFalse(result);
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository).addParticipant(ROOM_ID, PARTICIPANT);
    }

    @Test
    void testSendMessage_Success() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(true);
        when(chatRepository.sendMessage(anyString(), any(Message.class))).thenReturn(true);

        // Act
        boolean result = chatService.sendMessage(ROOM_ID, PARTICIPANT, MESSAGE_TEXT);

        // Assert
        assertTrue(result);
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository).sendMessage(eq(ROOM_ID), any(Message.class));
        verify(messagePublisher).publish(eq(ROOM_ID), any(Message.class));
    }

    @Test
    void testSendMessage_RoomNotExists() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(false);

        // Act
        boolean result = chatService.sendMessage(ROOM_ID, PARTICIPANT, MESSAGE_TEXT);

        // Assert
        assertFalse(result);
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository, never()).sendMessage(anyString(), any(Message.class));
        verify(messagePublisher, never()).publish(anyString(), any(Message.class));
    }

    @Test
    void testGetMessages_Success() {
        // Arrange
        Message message = Message.builder()
                .participant(PARTICIPANT)
                .message(MESSAGE_TEXT)
                .build();
        
        List<Message> expectedMessages = List.of(message);
        
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(true);
        when(chatRepository.getMessages(ROOM_ID, 10)).thenReturn(expectedMessages);

        // Act
        List<Message> result = chatService.getMessages(ROOM_ID, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PARTICIPANT, result.get(0).getParticipant());
        assertEquals(MESSAGE_TEXT, result.get(0).getMessage());
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository).getMessages(ROOM_ID, 10);
    }

    @Test
    void testGetMessages_RoomNotExists() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(false);

        // Act
        List<Message> result = chatService.getMessages(ROOM_ID, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository, never()).getMessages(anyString(), anyInt());
    }

    @Test
    void testGetChatRoom_Success() {
        // Arrange
        ChatRoom expectedRoom = ChatRoom.builder()
                .roomId(ROOM_ID)
                .roomName(ROOM_NAME)
                .build();
        
        when(chatRepository.getChatRoom(ROOM_ID)).thenReturn(expectedRoom);

        // Act
        ChatRoom result = chatService.getChatRoom(ROOM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
        verify(chatRepository).getChatRoom(ROOM_ID);
    }

    @Test
    void testGetChatRoom_NotFound() {
        // Arrange
        when(chatRepository.getChatRoom(ROOM_ID)).thenReturn(null);

        // Act
        ChatRoom result = chatService.getChatRoom(ROOM_ID);

        // Assert
        assertNull(result);
        verify(chatRepository).getChatRoom(ROOM_ID);
    }

    @Test
    void testGetParticipants_Success() {
        // Arrange
        Set<Object> expectedParticipants = Set.of(PARTICIPANT, "another_user");
        
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(true);
        when(chatRepository.getParticipants(ROOM_ID)).thenReturn(expectedParticipants);

        // Act
        Set<Object> result = chatService.getParticipants(ROOM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository).getParticipants(ROOM_ID);
    }

    @Test
    void testGetParticipants_RoomNotExists() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(false);

        // Act
        Set<Object> result = chatService.getParticipants(ROOM_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(chatRepository).roomExists(ROOM_ID);
        verify(chatRepository, never()).getParticipants(anyString());
    }

    @Test
    void testDeleteChatRoom_Success() {
        // Arrange
        when(chatRepository.deleteChatRoom(ROOM_ID)).thenReturn(true);

        // Act
        boolean result = chatService.deleteChatRoom(ROOM_ID);

        // Assert
        assertTrue(result);
        verify(chatRepository).deleteChatRoom(ROOM_ID);
    }

    @Test
    void testDeleteChatRoom_Failed() {
        // Arrange
        when(chatRepository.deleteChatRoom(ROOM_ID)).thenReturn(false);

        // Act
        boolean result = chatService.deleteChatRoom(ROOM_ID);

        // Assert
        assertFalse(result);
        verify(chatRepository).deleteChatRoom(ROOM_ID);
    }

    @Test
    void testRoomExists_True() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(true);

        // Act
        boolean result = chatService.roomExists(ROOM_ID);

        // Assert
        assertTrue(result);
        verify(chatRepository).roomExists(ROOM_ID);
    }

    @Test
    void testRoomExists_False() {
        // Arrange
        when(chatRepository.roomExists(ROOM_ID)).thenReturn(false);

        // Act
        boolean result = chatService.roomExists(ROOM_ID);

        // Assert
        assertFalse(result);
        verify(chatRepository).roomExists(ROOM_ID);
    }
}
