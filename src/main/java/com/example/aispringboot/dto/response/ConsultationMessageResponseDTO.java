package com.example.aispringboot.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ConsultationMessageResponseDTO {
    private Long id;
    private Long sessionId;
    private Integer senderType;
    private String senderTypeDesc;
    private Integer messageType;
    private String messageTypeDesc;
    private String content;
    private String emotionTag;
    private String aiModel;
    private LocalDateTime createdAt;
    private Integer contentLength;

    public void calculateContentLength() {
        this.contentLength = this.content != null ? this.content.length() : 0;
    }
}

