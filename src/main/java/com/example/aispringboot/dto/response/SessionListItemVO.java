package com.example.aispringboot.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SessionListItemVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String sessionTitle;
    private LocalDateTime startedAt;
    private Integer messageCount;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Long durationMinutes;
}

