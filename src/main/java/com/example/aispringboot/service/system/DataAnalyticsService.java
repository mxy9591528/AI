package com.example.aispringboot.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aispringboot.dto.response.DataAnalyticsOverviewDTO;
import com.example.aispringboot.entity.ConsultationSession;
import com.example.aispringboot.entity.EmotionDiary;
import com.example.aispringboot.entity.User;
import com.example.aispringboot.mapper.ConsultationSessionMapper;
import com.example.aispringboot.mapper.EmotionDiaryMapper;
import com.example.aispringboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据看板统计。所有指标均下推到 SQL 聚合完成，避免全表数据加载进内存；
 * 结果整体缓存到 Redis（5 分钟 TTL），看板高频刷新不再反复打库。
 */
@Service
public class DataAnalyticsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TREND_DAYS = 7;

    @Resource
    private UserMapper userMapper;
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;
    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    @Cacheable(cacheNames = "analytics:overview")
    public DataAnalyticsOverviewDTO getOverview() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(TREND_DAYS - 1L);

        DataAnalyticsOverviewDTO dto = new DataAnalyticsOverviewDTO();
        fillSystemOverview(dto, today, windowStart);
        dto.setEmotionTrend(buildEmotionTrend(windowStart));
        dto.setConsultationStats(buildConsultationStats(windowStart));
        dto.setUserActivity(buildUserActivity(windowStart));
        return dto;
    }

    private void fillSystemOverview(DataAnalyticsOverviewDTO dto, LocalDate today, LocalDate windowStart) {
        DataAnalyticsOverviewDTO.SystemOverview overview = new DataAnalyticsOverviewDTO.SystemOverview();
        overview.setTotalUsers(userMapper.selectCount(null));
        overview.setTotalDiaries(emotionDiaryMapper.selectCount(null));
        overview.setTotalSessions(consultationSessionMapper.selectCount(null));
        overview.setTodayNewDiaries(emotionDiaryMapper.selectCount(
                new QueryWrapper<EmotionDiary>().ge("diary_date", today)));
        overview.setTodayNewSessions(consultationSessionMapper.selectCount(
                new QueryWrapper<ConsultationSession>().ge("started_at", today.atStartOfDay())));
        overview.setActiveUsers(countActiveUsers(windowStart));
        overview.setAvgMoodScore(round3(selectAvgMood(
                new QueryWrapper<EmotionDiary>().select("IFNULL(AVG(mood_score), 0) AS avg_mood"))));
        dto.setSystemOverview(overview);
    }

    private Long countActiveUsers(LocalDate windowStart) {
        Set<Object> userIds = new HashSet<>();
        consultationSessionMapper.selectMaps(new QueryWrapper<ConsultationSession>()
                .select("DISTINCT user_id").isNotNull("user_id").ge("started_at", windowStart.atStartOfDay()))
                .forEach(row -> userIds.add(row.get("user_id")));
        emotionDiaryMapper.selectMaps(new QueryWrapper<EmotionDiary>()
                .select("DISTINCT user_id").isNotNull("user_id").ge("diary_date", windowStart))
                .forEach(row -> userIds.add(row.get("user_id")));
        return (long) userIds.size();
    }

    private List<DataAnalyticsOverviewDTO.EmotionTrendItem> buildEmotionTrend(LocalDate windowStart) {
        List<Map<String, Object>> rows = emotionDiaryMapper.selectMaps(new QueryWrapper<EmotionDiary>()
                .select("DATE_FORMAT(diary_date, '%Y-%m-%d') AS stat_date",
                        "IFNULL(AVG(mood_score), 0) AS avg_mood", "COUNT(*) AS record_count")
                .ge("diary_date", windowStart)
                .groupBy("diary_date"));
        Map<String, Map<String, Object>> rowByDate = indexByDate(rows);

        List<DataAnalyticsOverviewDTO.EmotionTrendItem> trend = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> row = rowByDate.get(date.format(DATE_FORMATTER));
            DataAnalyticsOverviewDTO.EmotionTrendItem item = new DataAnalyticsOverviewDTO.EmotionTrendItem();
            item.setDate(date.format(DATE_FORMATTER));
            item.setAvgMoodScore(row == null ? 0.0 : round3(numberValue(row, "avg_mood").doubleValue()));
            item.setRecordCount(row == null ? 0L : numberValue(row, "record_count").longValue());
            trend.add(item);
        }
        return trend;
    }

    private DataAnalyticsOverviewDTO.ConsultationStats buildConsultationStats(LocalDate windowStart) {
        List<Map<String, Object>> rows = consultationSessionMapper.selectMaps(new QueryWrapper<ConsultationSession>()
                .select("DATE_FORMAT(started_at, '%Y-%m-%d') AS stat_date",
                        "COUNT(*) AS session_count", "COUNT(DISTINCT user_id) AS user_count")
                .ge("started_at", windowStart.atStartOfDay())
                .groupBy("DATE_FORMAT(started_at, '%Y-%m-%d')"));
        Map<String, Map<String, Object>> rowByDate = indexByDate(rows);

        List<DataAnalyticsOverviewDTO.DailyTrendItem> dailyTrend = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> row = rowByDate.get(date.format(DATE_FORMATTER));
            DataAnalyticsOverviewDTO.DailyTrendItem item = new DataAnalyticsOverviewDTO.DailyTrendItem();
            item.setDate(date.format(DATE_FORMATTER));
            item.setSessionCount(row == null ? 0L : numberValue(row, "session_count").longValue());
            item.setUserCount(row == null ? 0L : numberValue(row, "user_count").longValue());
            dailyTrend.add(item);
        }

        DataAnalyticsOverviewDTO.ConsultationStats stats = new DataAnalyticsOverviewDTO.ConsultationStats();
        stats.setTotalSessions(consultationSessionMapper.selectCount(null));
        stats.setAvgDurationMinutes(0L);
        stats.setDailyTrend(dailyTrend);
        return stats;
    }

    private List<DataAnalyticsOverviewDTO.UserActivityItem> buildUserActivity(LocalDate windowStart) {
        List<Map<String, Object>> diaryRows = emotionDiaryMapper.selectMaps(new QueryWrapper<EmotionDiary>()
                .select("DATE_FORMAT(diary_date, '%Y-%m-%d') AS stat_date", "COUNT(DISTINCT user_id) AS diary_users")
                .ge("diary_date", windowStart)
                .groupBy("diary_date"));
        List<Map<String, Object>> sessionRows = consultationSessionMapper.selectMaps(new QueryWrapper<ConsultationSession>()
                .select("DATE_FORMAT(started_at, '%Y-%m-%d') AS stat_date",
                        "COUNT(*) AS session_count", "COUNT(DISTINCT user_id) AS consultation_users")
                .ge("started_at", windowStart.atStartOfDay())
                .groupBy("DATE_FORMAT(started_at, '%Y-%m-%d')"));
        List<Map<String, Object>> newUserRows = userMapper.selectMaps(new QueryWrapper<User>()
                .select("DATE_FORMAT(created_at, '%Y-%m-%d') AS stat_date", "COUNT(*) AS new_users")
                .ge("created_at", windowStart.atStartOfDay())
                .groupBy("DATE_FORMAT(created_at, '%Y-%m-%d')"));
        Map<String, Map<String, Object>> diaryByDate = indexByDate(diaryRows);
        Map<String, Map<String, Object>> sessionByDate = indexByDate(sessionRows);
        Map<String, Map<String, Object>> newUserByDate = indexByDate(newUserRows);

        List<DataAnalyticsOverviewDTO.UserActivityItem> activity = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String key = date.format(DATE_FORMATTER);
            Map<String, Object> diaryRow = diaryByDate.get(key);
            Map<String, Object> sessionRow = sessionByDate.get(key);
            Map<String, Object> newUserRow = newUserByDate.get(key);
            long diaryUsers = diaryRow == null ? 0L : numberValue(diaryRow, "diary_users").longValue();
            long consultationUsers = sessionRow == null ? 0L : numberValue(sessionRow, "consultation_users").longValue();

            DataAnalyticsOverviewDTO.UserActivityItem item = new DataAnalyticsOverviewDTO.UserActivityItem();
            item.setDate(key);
            item.setDiaryUsers(diaryUsers);
            item.setConsultationUsers(consultationUsers);
            item.setNewUsers(newUserRow == null ? 0L : numberValue(newUserRow, "new_users").longValue());
            item.setActiveUsers(Math.max(diaryUsers, consultationUsers));
            activity.add(item);
        }
        return activity;
    }

    private double selectAvgMood(QueryWrapper<EmotionDiary> wrapper) {
        List<Map<String, Object>> rows = emotionDiaryMapper.selectMaps(wrapper);
        return rows.isEmpty() ? 0.0 : numberValue(rows.get(0), "avg_mood").doubleValue();
    }

    private Map<String, Map<String, Object>> indexByDate(List<Map<String, Object>> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row.get("stat_date")), row -> row, (a, b) -> a));
    }

    private static Number numberValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number : java.math.BigDecimal.ZERO;
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
