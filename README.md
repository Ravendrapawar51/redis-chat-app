# Redis Chat Application

A simple chat application built with Spring Boot and Redis as the primary data store. The application provides REST APIs for creating chat rooms, joining them, sending messages, and retrieving chat history with real-time messaging capabilities using Redis Pub/Sub.

## Features

- **Chat Room Management**: Create unique chat rooms with custom names
- **Participant Management**: Join chat rooms as participants
- **Messaging**: Send and retrieve messages in chronological order
- **Real-time Updates**: Redis Pub/Sub for real-time message broadcasting
- **Message History**: Retrieve last N messages from any chat room
- **Error Handling**: Comprehensive validation and error responses
- **Redis Data Structures**: Efficient use of Hash, Set, List, and Pub/Sub

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data Redis**
- **Lombok** (for reducing boilerplate code)
- **JUnit 5** (for testing)
- **Mockito** (for unit testing)
- **Maven** (build tool)

## Prerequisites

- Java 17 or higher
- **Either** Maven 3.6+ **or** Gradle 8.x
- Redis Server (running on localhost:6379 by default)

## Installation

### 1. Install Java 17

Ensure Java 17 or higher is installed. Check with:
```bash
java -version
```

If not installed, download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/#java17) or [OpenJDK](https://adoptium.net/).

### 2. Install Maven on Windows

**Option A: Using Chocolatey (Recommended)**
```powershell
# First install Chocolatey if not already installed
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Then install Maven
choco install maven
```

**Option B: Manual Installation**
1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to a directory (e.g., `C:\Program Files\Apache\maven`)
3. Add Maven to PATH:
   - Search for "Environment Variables" in Windows
   - Edit "Path" under System Variables
   - Add: `C:\Program Files\Apache\maven\bin`
4. Set `MAVEN_HOME` environment variable to: `C:\Program Files\Apache\maven`
5. Restart PowerShell and verify: `mvn -version`

**Option C: Using Scoop**
```powershell
# Install Scoop if not already installed
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression

# Install Maven
scoop install maven
```

### 3. Install Redis

#### Windows:
Download Redis for Windows from [Microsoft Archive](https://github.com/microsoftarchive/redis/releases) or use Docker:
```bash
docker run -d -p 6379:6379 redis:latest
```

#### macOS:
```bash
brew install redis
brew services start redis
```

#### Linux:
```bash
sudo apt-get install redis-server
sudo systemctl start redis
```

### 3. Configure Redis Connection

Edit `src/main/resources/application.yml` if your Redis is not running on localhost:6379:

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:  # Leave empty if no password
    database: 0
```

### 4. Clone the Repository

```bash
git clone <repository-url>
cd redis-chat-app
```

### 5. Build the Project

**Using Maven:**
```bash
mvn clean install
```

**Using Gradle:**
```bash
gradlew.bat clean build
```
*(On Windows, use `gradlew.bat`; on Linux/Mac, use `./gradlew`)*

### 6. Run the Application

**Using Maven:**
```bash
mvn spring-boot:run
```

**Using Gradle:**
```bash
gradlew.bat bootRun
```

The application will start on `http://localhost:8080`

## API Documentation

### Base URL
```
http://localhost:8080/api/chatapp
```

### Endpoints

#### 1. Create a Chat Room

**Endpoint:** `POST /api/chatapp/chatrooms`

**Request Body:**
```json
{
  "roomName": "general"
}
```

**Response (201 Created):**
```json
{
  "message": "Chat room 'general' created successfully.",
  "status": "success",
  "data": {
    "roomId": "general"
  }
}
```

**Error Response (409 Conflict):**
```json
{
  "message": "Chat room 'general' already exists.",
  "status": "error"
}
```

---

#### 2. Join a Chat Room

**Endpoint:** `POST /api/chatapp/chatrooms/{roomId}/join`

**Example URL:** `POST /api/chatapp/chatrooms/general/join`

**Request Body:**
```json
{
  "participant": "guest_user"
}
```

**Response (200 OK):**
```json
{
  "message": "User 'guest_user' joined chat room 'general'.",
  "status": "success"
}
```

**Error Response (404 Not Found):**
```json
{
  "message": "Chat room 'general' does not exist.",
  "status": "error"
}
```

---

#### 3. Send a Message

**Endpoint:** `POST /api/chatapp/chatrooms/{roomId}/messages`

**Example URL:** `POST /api/chatapp/chatrooms/general/messages`

**Request Body:**
```json
{
  "participant": "guest_user",
  "message": "Hello, everyone!"
}
```

**Response (200 OK):**
```json
{
  "message": "Message sent successfully.",
  "status": "success"
}
```

**Error Response (404 Not Found):**
```json
{
  "message": "Chat room 'general' does not exist.",
  "status": "error"
}
```

---

#### 4. Retrieve Chat History

**Endpoint:** `GET /api/chatapp/chatrooms/{roomId}/messages?limit=N`

**Example URL:** `GET /api/chatapp/chatrooms/general/messages?limit=10`

**Response (200 OK):**
```json
{
  "message": "Messages retrieved successfully.",
  "status": "success",
  "data": {
    "messages": [
      {
        "participant": "guest_user",
        "message": "Hello, everyone!",
        "timestamp": "2024-01-01T10:00:00Z"
      },
      {
        "participant": "another_user",
        "message": "Hi, guest_user!",
        "timestamp": "2024-01-01T10:01:00Z"
      }
    ]
  }
}
```

**Error Response (404 Not Found):**
```json
{
  "message": "Chat room 'general' does not exist.",
  "status": "error"
}
```

---

#### 5. Get Chat Room Information

**Endpoint:** `GET /api/chatapp/chatrooms/{roomId}`

**Example URL:** `GET /api/chatapp/chatrooms/general`

**Response (200 OK):**
```json
{
  "message": "Chat room retrieved successfully.",
  "status": "success",
  "data": {
    "roomId": "general",
    "roomName": "General",
    "createdAt": "2024-01-01T09:00:00",
    "participantCount": 2
  }
}
```

---

#### 6. Delete a Chat Room (Optional)

**Endpoint:** `DELETE /api/chatapp/chatrooms/{roomId}`

**Example URL:** `DELETE /api/chatapp/chatrooms/general`

**Response (200 OK):**
```json
{
  "message": "Chat room 'general' deleted successfully.",
  "status": "success"
}
```

---

### Real-Time WebSocket Delivery (STOMP)

The application bridges Redis Pub/Sub directly to WebSockets, delivering messages in real time to connected clients across nodes.

- **WebSocket Handshake URL:** `ws://localhost:8080/ws-chat` (or `http://localhost:8080/ws-chat` with SockJS)
- **Subscribe Destination:** `/topic/{roomId}` (e.g. `/topic/general`)
- **Send Destination (STOMP):** `/app/chat/{roomId}/send`
- **Interactive Web Demo:** Visit `http://localhost:8080/index.html` in your browser to test live messaging between multiple tabs.

## Testing with Postman

### Test Case 1: Create and Join Chat Room

1. **Create Room:**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms`
   - Body (JSON):
     ```json
     {
       "roomName": "general"
     }
     ```
   - Expected: 201 Created with roomId

2. **Join Room:**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms/general/join`
   - Body (JSON):
     ```json
     {
       "participant": "guest_user"
     }
     ```
   - Expected: 200 OK

### Test Case 2: Send and Retrieve Messages

1. **Send Message 1:**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms/general/messages`
   - Body (JSON):
     ```json
     {
       "participant": "guest_user",
       "message": "Hello, everyone!"
     }
     ```

2. **Send Message 2:**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms/general/messages`
   - Body (JSON):
     ```json
     {
       "participant": "another_user",
       "message": "Hi, guest_user!"
     }
     ```

3. **Retrieve Messages:**
   - Method: GET
   - URL: `http://localhost:8080/api/chatapp/chatrooms/general/messages?limit=10`
   - Expected: Array of 2 messages in chronological order

### Test Case 3: Error Handling - Non-existent Room

1. **Send to Non-existent Room:**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms/nonexistent/messages`
   - Body (JSON):
     ```json
     {
       "participant": "guest_user",
       "message": "Test"
     }
     ```
   - Expected: 404 Not Found with error message

### Test Case 4: Duplicate Chat Room

1. **Create Room (first time):**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms`
   - Body (JSON):
     ```json
     {
       "roomName": "test"
     }
     ```
   - Expected: 201 Created

2. **Create Room (duplicate):**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms`
   - Body (JSON):
     ```json
     {
       "roomName": "test"
     }
     ```
   - Expected: 409 Conflict with error message

### Test Case 5: Chat Room Deletion

1. **Create Room:**
   - Method: POST
   - URL: `http://localhost:8080/api/chatapp/chatrooms`
   - Body (JSON):
     ```json
     {
       "roomName": "temp_room"
     }
     ```

2. **Delete Room:**
   - Method: DELETE
   - URL: `http://localhost:8080/api/chatapp/chatrooms/temp_room`
   - Expected: 200 OK

3. **Verify Deletion:**
   - Method: GET
   - URL: `http://localhost:8080/api/chatapp/chatrooms/temp_room`
   - Expected: 404 Not Found

## Running Tests

### Using Gradle (Recommended)
```bash
# Run all tests (Windows)
gradlew.bat test

# Run all tests (Linux/macOS)
./gradlew test
```

### Using Maven
```bash
mvn test
mvn verify
```

### Test Coverage
The project includes:
- **Unit Tests**: Service layer tests with Mockito
- **Controller Tests**: REST API endpoint tests
- **Integration Tests**: Full-stack tests with real Redis

## Redis Data Structures

The application uses the following Redis data structures:

1. **Hash** (`chat:room:{roomId}`): Stores chat room metadata
   - roomId
   - roomName
   - createdAt
   - participantCount

2. **Set** (`chat:participants:{roomId}`): Stores unique participants in a room

3. **List** (`chat:messages:{roomId}`): Stores messages in chronological order

4. **Pub/Sub** (`chat:{roomId}`): Channel for real-time message broadcasting

## Project Structure

```
redis-chat-app/
├── src/
│   ├── main/
│   │   ├── java/com/chatapp/
│   │   │   ├── ChatApplication.java          # Main application class
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java          # Redis configuration (PatternTopic)
│   │   │   │   └── WebSocketConfig.java      # WebSocket STOMP configuration
│   │   │   ├── controller/
│   │   │   │   ├── ChatController.java       # REST API endpoints
│   │   │   │   └── WebSocketChatController.java # STOMP WebSocket controller
│   │   │   ├── service/
│   │   │   │   ├── ChatService.java          # Business logic
│   │   │   │   ├── RedisMessagePublisher.java # Redis Pub/Sub publisher
│   │   │   │   └── RedisMessageSubscriber.java # Pub/Sub listener & WebSocket bridge
│   │   │   ├── repository/
│   │   │   │   └── ChatRepository.java        # Redis operations (Hash, Set, List)
│   │   │   ├── model/
│   │   │   │   ├── ChatRoom.java
│   │   │   │   └── Message.java
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── CreateRoomRequest.java
│   │   │   │   ├── JoinRoomRequest.java
│   │   │   │   ├── SendMessageRequest.java
│   │   │   │   └── MessageHistoryResponse.java
│   │   │   └── exception/
│   │   │       ├── ChatRoomNotFoundException.java
│   │   │       ├── DuplicateChatRoomException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── static/
│   │       │   └── index.html                # Live multi-tab WebSocket chat UI
│   │       └── application.yml               # Application configuration
│   └── test/
│       ├── java/com/chatapp/
│       │   ├── controller/
│       │   │   └── ChatControllerTest.java   # Controller tests
│       │   ├── service/
│       │   │   ├── ChatServiceTest.java      # Service tests
│       │   │   └── RedisMessageSubscriberTest.java # Pub/Sub & WebSocket tests
│       │   └── integration/
│       │       └── ChatIntegrationTest.java  # Full-stack Redis integration tests
│       └── resources/
│           └── application-test.yml          # Test configuration
├── build.gradle                               # Gradle build configuration
└── pom.xml                                    # Maven configuration
```

## Design Patterns and Best Practices

- **Separation of Concerns**: Clear separation between controller, service, and repository layers
- **Dependency Injection**: Using Spring's @Autowired and constructor injection
- **DTO Pattern**: Data Transfer Objects for API requests/responses
- **Exception Handling**: Global exception handler with @RestControllerAdvice
- **Validation**: Using Jakarta Validation annotations for request validation
- **Builder Pattern**: Using Lombok @Builder for object creation
- **Immutability**: Using final fields and constructor injection
- **Logging**: SLF4J for structured logging

## Scalability Considerations

- **Connection Pooling**: Configured Redis connection pool with Lettuce
- **Efficient Data Structures**: Using appropriate Redis data structures for each use case
- **Pub/Sub**: Real-time messaging without polling
- **Pagination**: Support for retrieving limited number of messages

## Error Handling

The application handles the following error scenarios:

- **Duplicate Chat Room**: Returns 409 Conflict when creating an existing room
- **Non-existent Room**: Returns 404 Not Found for operations on invalid rooms
- **Validation Errors**: Returns 400 Bad Request for invalid input
- **Internal Server Error**: Returns 500 for unexpected errors

## License

This project is created for educational purposes.

## Author

- **Created By:** Ravendra Kurmi
- **Date:** November 14, 2026
- **Project:** Backend Assignment - Simple Chat Application Using Redis
