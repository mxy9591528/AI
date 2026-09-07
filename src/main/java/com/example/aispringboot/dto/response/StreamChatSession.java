package com.example.aispringboot.dto.response;

public record StreamChatSession(String sessionId, Long userHash, String initialMessage, Long startTime, Long expiryTime, Integer messageCount, String status) {
}
