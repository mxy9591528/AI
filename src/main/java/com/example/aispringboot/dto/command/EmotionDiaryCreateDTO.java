package com.example.aispringboot.dto.command;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class EmotionDiaryCreateDTO {
    @NotNull(message="日记日期不能为空")
    @JsonFormat(pattern="yyyy-MM-dd")
    private @NotNull(message="日记日期不能为空") LocalDate diaryDate;
    @NotNull(message="请选择情绪评分")
    @Min(value=1L, message="情绪评分最小为1")
    @Max(value=10L, message="情绪评分最大为10")
    private @NotNull(message="请选择情绪评分") @Min(value=1L, message="情绪评分最小为1") @Max(value=10L, message="情绪评分最大为10") Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    @Min(value=1L, message="睡眠质量最小为1")
    @Max(value=5L, message="睡眠质量最大为5")
    private @Min(value=1L, message="睡眠质量最小为1") @Max(value=5L, message="睡眠质量最大为5") Integer sleepQuality;
    @Min(value=1L, message="压力水平最小为1")
    @Max(value=5L, message="压力水平最大为5")
    private @Min(value=1L, message="压力水平最小为1") @Max(value=5L, message="压力水平最大为5") Integer stressLevel;
}

