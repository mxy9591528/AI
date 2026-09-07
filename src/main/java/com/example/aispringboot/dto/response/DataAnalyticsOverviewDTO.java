package com.example.aispringboot.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class DataAnalyticsOverviewDTO {
    private SystemOverview systemOverview;
    private List<EmotionTrendItem> emotionTrend;
    private ConsultationStats consultationStats;
    private List<UserActivityItem> userActivity;

    @Data
    public static class SystemOverview {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalDiaries;
        private Long todayNewDiaries;
        private Long totalSessions;
        private Long todayNewSessions;
        private Double avgMoodScore;
    }

    @Data
    public static class EmotionTrendItem {
        private String date;
        private Double avgMoodScore;
        private Long recordCount;
    }

    @Data
    public static class ConsultationStats {
        private Long totalSessions;
        private Long avgDurationMinutes;
        private List<DailyTrendItem> dailyTrend;
    }

    @Data
    public static class DailyTrendItem {
        private String date;
        private Long sessionCount;
        private Long userCount;
    }

    @Data
    public static class UserActivityItem {
        private String date;
        private Long activeUsers;
        private Long newUsers;
        private Long diaryUsers;
        private Long consultationUsers;
    }
}
