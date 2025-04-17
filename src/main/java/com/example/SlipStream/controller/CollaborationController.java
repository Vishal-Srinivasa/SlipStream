package com.example.SlipStream.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map; // Import Map

@Controller
public class CollaborationController {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    // In-memory storage for presence (replace with Akka Distributed Data later)
    // Map<pageId, Map<sessionId, userEmail>>
    private static final Map<String, Map<String, String>> presence = new java.util.concurrent.ConcurrentHashMap<>();
    // Map<pageId, List<UserInfo>> - Simpler structure for broadcasting active users
    private static final Map<String, java.util.List<UserInfo>> activeUsersByPage = new java.util.concurrent.ConcurrentHashMap<>();


    /**
     * Handles users joining a page collaboration session.
     * Stores user info and broadcasts updated presence list.
     */
    @MessageMapping("/join/{pageId}")
    @SendTo("/topic/presence/{pageId}") // Broadcast updated presence to subscribers of this page
    public PresenceInfo handleJoin(@DestinationVariable String pageId, @Payload UserInfo userInfo, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("User joined page {}: {} (Session: {})", pageId, userInfo.getEmail(), sessionId);

        // Store user presence
        activeUsersByPage.computeIfAbsent(pageId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                         .removeIf(u -> u.getSessionId() != null && u.getSessionId().equals(sessionId)); // Remove old entry if rejoining
        userInfo.setSessionId(sessionId); // Associate session ID
        activeUsersByPage.get(pageId).add(userInfo);


        // Return the current list of active users for this page
        return new PresenceInfo(activeUsersByPage.get(pageId));
    }

     /**
     * Handles users leaving a page collaboration session.
     * Removes user info and broadcasts updated presence list.
     * Note: This relies on explicit leave messages. Need WebSocket disconnect listener for robust handling.
     */
    @MessageMapping("/leave/{pageId}")
    @SendTo("/topic/presence/{pageId}")
    public PresenceInfo handleLeave(@DestinationVariable String pageId, @Payload UserInfo userInfo, SimpMessageHeaderAccessor headerAccessor) {
         String sessionId = headerAccessor.getSessionId();
         logger.info("User left page {}: {} (Session: {})", pageId, userInfo.getEmail(), sessionId);

         java.util.List<UserInfo> usersOnPage = activeUsersByPage.get(pageId);
         if (usersOnPage != null) {
             usersOnPage.removeIf(u -> u.getSessionId() != null && u.getSessionId().equals(sessionId));
             if (usersOnPage.isEmpty()) {
                 activeUsersByPage.remove(pageId); // Clean up if page is empty
             }
         }

         return new PresenceInfo(usersOnPage != null ? usersOnPage : java.util.Collections.emptyList());
    }


    /**
     * Handles incoming editor commands (edits).
     * Placeholder: Echoes the command back to all subscribers of the page topic.
     * TODO: Integrate with Command Pattern and Akka for processing and conflict resolution.
     */
    @MessageMapping("/edit/{pageId}")
    @SendTo("/topic/pages/{pageId}") // Send processed/validated updates to page subscribers
    public Object handleEdit(@DestinationVariable String pageId, @Payload Object command, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        // String userEmail = findUserEmailBySessionId(pageId, sessionId); // Need to implement this lookup
        logger.info("Received edit command for page {}: {} from session {}", pageId, command, sessionId);

        // --- Placeholder Logic ---
        // 1. Validate command
        // 2. Apply command using Command Pattern (potentially via Akka Actor)
        // 3. Handle conflicts (using Akka Distributed Data or other strategy)
        // 4. Persist changes to Firebase (likely triggered by Akka Actor after state update)
        // 5. Broadcast the confirmed change (or delta) to clients
        // For now, just echo the command back
        return command;
        // --- End Placeholder ---
    }

     /**
     * Handles incoming cursor position updates.
     * Placeholder: Echoes the cursor update back to all subscribers of the cursor topic for the page.
     * TODO: Add user identification to the cursor data.
     */
    @MessageMapping("/cursor/{pageId}")
    @SendTo("/topic/cursors/{pageId}") // Send cursor updates to cursor subscribers for this page
    public Object handleCursor(@DestinationVariable String pageId, @Payload Object cursorData, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        // String userEmail = findUserEmailBySessionId(pageId, sessionId); // Need to implement this lookup
        logger.debug("Received cursor update for page {}: {} from session {}", pageId, cursorData, sessionId);

        // --- Placeholder Logic ---
        // Add user identifier (email/ID) and session ID to the cursor data before broadcasting
        // For now, just echo back. The client needs to know *who* the cursor belongs to.
        // Example structure to send back: { "sessionId": "...", "userEmail": "...", "position": cursorData }
        return cursorData; // Needs enhancement to include user info
        // --- End Placeholder ---
    }


    // --- Helper DTOs ---

    public static class UserInfo {
        private String email;
        private String sessionId; // Added to track connection

        // Default constructor for JSON deserialization
        public UserInfo() {}

        public UserInfo(String email) {
            this.email = email;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserInfo userInfo = (UserInfo) o;
            return java.util.Objects.equals(sessionId, userInfo.sessionId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(sessionId);
        }
    }

    public static class PresenceInfo {
        private java.util.List<UserInfo> activeUsers;

        public PresenceInfo(java.util.List<UserInfo> activeUsers) {
            // Ensure we don't send session IDs to the client if not needed/secure
            // Create a copy without session IDs for broadcasting
            this.activeUsers = activeUsers.stream()
                                          .map(u -> new UserInfo(u.getEmail())) // Create new UserInfo without sessionId
                                          .collect(java.util.stream.Collectors.toList());
        }

        public java.util.List<UserInfo> getActiveUsers() { return activeUsers; }
        public void setActiveUsers(java.util.List<UserInfo> activeUsers) { this.activeUsers = activeUsers; }
    }

     // TODO: Implement robust disconnect handling using ApplicationListener<SessionDisconnectEvent>
     // to remove users from presence when their WebSocket session ends abruptly.
}
