package com.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "consultation_session")
public class ConsultationSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @Size(max = 200, message = "会话标题长度不能超过200个字符")
    @TableField(value = "session_title")
    private String sessionTitle;

    @TableField(value = "started_at")
    private LocalDateTime startedAt;

    @TableField(value = "last_emotion_analysis")
    private String lastEmotionAnalysis;

    @TableField(value = "last_emotion_updated_at")
    private LocalDateTime lastEmotionUpdatedAt;
}
