package com.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value="emotion_diary")
public class EmotionDiary {
    @TableId(type=IdType.AUTO)
    private Long id;
    @TableField(value="user_id")
    private Long userId;
    @TableField(value="diary_date")
    private LocalDate diaryDate;
    @TableField(value="mood_score")
    private Integer moodScore;
    @TableField(value="dominant_emotion")
    private String dominantEmotion;
    @TableField(value="emotion_triggers")
    private String emotionTriggers;
    @TableField(value="diary_content")
    private String diaryContent;
    @TableField(value="sleep_quality")
    private Integer sleepQuality;
    @TableField(value="stress_level")
    private Integer stressLevel;
    @TableField(value="ai_emotion_analysis")
    private String aiEmotionAnalysis;
    @TableField(value="ai_analysis_updated_at")
    private LocalDateTime aiAnalysisUpdatedAt;
    @TableField(value="created_at", fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value="updated_at", fill=FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

