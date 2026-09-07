package com.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@TableName(value = "consultation_message")
public class ConsultationMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "会话ID不能为空")
    @TableField(value = "session_id")
    private Long sessionId;

    @NotNull(message = "发送者类型不能为空")
    @TableField(value = "sender_type")
    private Integer senderType;

    @NotNull(message = "消息类型不能为空")
    @TableField(value = "message_type")
    private Integer messageType;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Size(max = 50, message = "情绪标签长度不能超过50个字符")
    @TableField(value = "emotion_tag")
    private String emotionTag;

    @Size(max = 50, message = "AI模型名称长度不能超过50个字符")
    @TableField(value = "ai_model")
    private String aiModel;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public String getSenderTypeDesc() {
        if (this.senderType == null) {
            return "未知";
        }
        return switch (this.senderType) {
            case 1 -> "用户";
            case 2 -> "AI助手";
            default -> "未知";
        };
    }

    public String getMessageTypeDesc() {
        if (this.messageType == 1) {
            return "文本";
        }
        return "未知";
    }
}
