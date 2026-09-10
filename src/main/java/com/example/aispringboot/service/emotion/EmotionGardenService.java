package com.example.aispringboot.service.emotion;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aispringboot.entity.ConsultationMessage;
import com.example.aispringboot.entity.ConsultationSession;
import com.example.aispringboot.entity.EmotionDiary;
import com.example.aispringboot.mapper.ConsultationMessageMapper;
import com.example.aispringboot.mapper.ConsultationSessionMapper;
import com.example.aispringboot.mapper.EmotionDiaryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 情绪花园动态评分：综合用户近 14 天的【情绪日记评分】与【AI 咨询对话内容】
 * 进行关键词情感分析，输出 0-100 的情绪健康分及对应的情绪名称、风险等级、建议。
 * 无任何数据时返回默认中性状态（50 分）。
 */
@Service
public class EmotionGardenService {

    /** 统计窗口：最近 14 天。 */
    private static final int WINDOW_DAYS = 14;
    /** 对话内容参与分析的用户消息最大条数。 */
    private static final int MAX_CHAT_MESSAGES = 100;
    /** 日记评分权重（日记是用户主动自评，权重高于对话）。 */
    private static final double DIARY_WEIGHT = 0.6;
    private static final double CHAT_WEIGHT = 0.4;

    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;
    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    /** 高危/危机关键词：命中即风险等级 3。 */
    private static final String[] CRISIS_WORDS = {
            "自杀", "轻生", "不想活", "活不下去", "结束生命", "了结自己", "自残", "自伤",
            "伤害自己", "割腕", "跳楼", "去死", "想死", "了此一生", "没意义", "解脱"
    };

    /** 消极情绪词（对话 + 日记内容匹配）。 */
    private static final String[] NEGATIVE_WORDS = {
            "难过", "伤心", "痛苦", "崩溃", "绝望", "抑郁", "焦虑", "烦躁", "害怕", "恐惧",
            "担心", "紧张", "不安", "压力", "压抑", "沮丧", "失落", "孤独", "寂寞", "委屈",
            "愤怒", "生气", "讨厌", "烦", "累", "疲惫", "失眠", "哭", "流泪", "自卑",
            "没用", "失败", "糟糕", "无力", "迷茫", "空虚", "郁闷", "折磨", "慌", "忧愁"
    };

    /** 积极情绪词。 */
    private static final String[] POSITIVE_WORDS = {
            "开心", "高兴", "快乐", "愉悦", "幸福", "兴奋", "惊喜", "满足", "轻松", "放松",
            "平静", "安心", "放心", "温暖", "感激", "感谢", "喜欢", "期待", "希望", "信心",
            "不错", "挺好", "还好", "舒服", "自在", "充实", "有趣", "笑", "棒", "治愈"
    };

    public Map<String, Object> buildGarden(Long userId) {
        LocalDate sinceDate = LocalDate.now().minusDays(WINDOW_DAYS - 1L);
        LocalDateTime sinceTime = sinceDate.atStartOfDay();

        // ---------- 1. 日记维度 ----------
        List<EmotionDiary> diaries = emotionDiaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>()
                        .eq(EmotionDiary::getUserId, userId)
                        .ge(EmotionDiary::getDiaryDate, sinceDate)
                        .orderByDesc(EmotionDiary::getDiaryDate));
        int diaryCount = diaries.size();
        Double diaryScore = null;
        int diaryPos = 0;
        int diaryNeg = 0;
        boolean crisis = false;
        if (!diaries.isEmpty()) {
            double sum = 0;
            int scored = 0;
            for (EmotionDiary d : diaries) {
                if (d.getMoodScore() != null) {
                    sum += d.getMoodScore();
                    scored++;
                }
                String text = joinText(d.getDiaryContent(), d.getDominantEmotion(), d.getEmotionTriggers());
                diaryPos += countMatches(text, POSITIVE_WORDS);
                diaryNeg += countMatches(text, NEGATIVE_WORDS);
                if (containsAny(text, CRISIS_WORDS)) {
                    crisis = true;
                }
            }
            if (scored > 0) {
                // moodScore 为 1-10 分制，换算为 0-100
                diaryScore = (sum / scored) * 10;
            }
        }

        // ---------- 2. 对话维度（近 14 天用户发送的消息） ----------
        List<ConsultationSession> sessions = consultationSessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>()
                        .eq(ConsultationSession::getUserId, userId)
                        .ge(ConsultationSession::getStartedAt, sinceTime)
                        .select(ConsultationSession::getId));
        Double chatScore = null;
        int chatCount = 0;
        int chatPos = 0;
        int chatNeg = 0;
        if (!sessions.isEmpty()) {
            List<Long> sessionIds = sessions.stream().map(ConsultationSession::getId).toList();
            List<ConsultationMessage> messages = consultationMessageMapper.selectList(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .in(ConsultationMessage::getSessionId, sessionIds)
                            .eq(ConsultationMessage::getSenderType, 1)
                            .ge(ConsultationMessage::getCreatedAt, sinceTime)
                            .orderByDesc(ConsultationMessage::getCreatedAt)
                            .last("limit " + MAX_CHAT_MESSAGES));
            chatCount = messages.size();
            for (ConsultationMessage m : messages) {
                String text = m.getContent() == null ? "" : m.getContent();
                chatPos += countMatches(text, POSITIVE_WORDS);
                chatNeg += countMatches(text, NEGATIVE_WORDS);
                if (containsAny(text, CRISIS_WORDS)) {
                    crisis = true;
                }
            }
            if (chatCount > 0) {
                // 以 50 为基准，积极词 +4 分/个，消极词 -7 分/个（消极词权重更高）
                double raw = 50 + chatPos * 4.0 - chatNeg * 7.0;
                chatScore = clamp(raw, 5, 95);
            }
        }

        // ---------- 3. 加权融合 ----------
        int finalScore;
        String dataSource;
        if (diaryScore != null && chatScore != null) {
            finalScore = (int) Math.round(diaryScore * DIARY_WEIGHT + chatScore * CHAT_WEIGHT);
            dataSource = String.format("近%d天日记%d篇、对话%d条", WINDOW_DAYS, diaryCount, chatCount);
        } else if (diaryScore != null) {
            finalScore = (int) Math.round(diaryScore);
            dataSource = String.format("近%d天日记%d篇", WINDOW_DAYS, diaryCount);
        } else if (chatScore != null) {
            finalScore = (int) Math.round(chatScore);
            dataSource = String.format("近%d天对话%d条", WINDOW_DAYS, chatCount);
        } else {
            return defaultGarden();
        }
        finalScore = (int) clamp(finalScore, 5, 100);

        // 日记文本的情绪词也轻微影响最终分（±5 分内）
        if (diaryScore != null) {
            finalScore = (int) clamp(finalScore + (diaryPos - diaryNeg) * 0.5, 5, 100);
        }

        // ---------- 4. 情绪名称 / 风险等级 / 建议 ----------
        int riskLevel;
        if (crisis) {
            riskLevel = 3;
        } else if (finalScore < 30) {
            riskLevel = 2;
        } else if (finalScore < 45) {
            riskLevel = 1;
        } else {
            riskLevel = 0;
        }
        boolean isNegative = riskLevel > 0;
        String primaryEmotion = emotionName(finalScore, crisis);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primaryEmotion", primaryEmotion);
        result.put("emotionScore", finalScore);
        result.put("isNegative", isNegative);
        result.put("riskLevel", riskLevel);
        result.put("suggestion", buildSuggestion(riskLevel, finalScore));
        result.put("riskDescription", buildRiskDescription(riskLevel));
        result.put("improvementSuggestions", buildActions(riskLevel));
        result.put("dataSource", dataSource);
        result.put("diaryCount", diaryCount);
        result.put("chatCount", chatCount);
        return result;
    }

    /** 无日记、无对话时的默认状态。 */
    private Map<String, Object> defaultGarden() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primaryEmotion", "中性");
        result.put("emotionScore", 50);
        result.put("isNegative", false);
        result.put("riskLevel", 0);
        result.put("suggestion", "还没有情绪记录，写一篇情绪日记或和AI助手聊聊吧");
        result.put("riskDescription", "当前情绪状态稳定，保持良好的心态哦");
        result.put("improvementSuggestions", List.of("记录今日情绪日记", "和AI助手说说心情", "保持规律作息"));
        result.put("dataSource", "暂无数据");
        result.put("diaryCount", 0);
        result.put("chatCount", 0);
        return result;
    }

    private String emotionName(int score, boolean crisis) {
        if (crisis) {
            return "危机";
        }
        if (score >= 80) {
            return "愉悦";
        }
        if (score >= 65) {
            return "轻松";
        }
        if (score >= 45) {
            return "平静";
        }
        if (score >= 30) {
            return "低落";
        }
        if (score >= 15) {
            return "焦虑";
        }
        return "消沉";
    }

    private String buildSuggestion(int riskLevel, int score) {
        return switch (riskLevel) {
            case 3 -> "我感受到你正在经历非常艰难的时刻，请一定不要独自承受，立刻联系信任的人或专业心理援助热线";
            case 2 -> "最近的情绪状态比较低落，建议多和身边的人倾诉，必要时寻求专业心理咨询师的帮助";
            case 1 -> "情绪略有波动，做些让自己放松的事情，好好照顾自己的感受";
            default -> score >= 75 ? "状态很不错！继续保持这份积极的心态" : "情绪状态平稳，保持良好的生活节奏";
        };
    }

    private String buildRiskDescription(int riskLevel) {
        return switch (riskLevel) {
            case 3 -> "检测到强烈的负面情绪表达，你的安全最重要，请立即联系心理援助热线（如400-161-9995）或前往医院";
            case 2 -> "近期情绪持续偏低，建议密切关注并主动寻求支持";
            case 1 -> "近期有轻微的情绪波动，注意自我调节";
            default -> "当前情绪状态稳定，无需特别关注";
        };
    }

    private List<String> buildActions(int riskLevel) {
        List<String> actions = new ArrayList<>();
        switch (riskLevel) {
            case 3 -> {
                actions.add("立即拨打心理援助热线 400-161-9995");
                actions.add("联系身边信任的亲友陪伴");
                actions.add("尽快前往医院心理科就诊");
            }
            case 2 -> {
                actions.add("预约一次专业心理咨询");
                actions.add("每天记录三件值得感恩的小事");
                actions.add("和AI助手聊聊心里的感受");
            }
            case 1 -> {
                actions.add("试试深呼吸或冥想10分钟");
                actions.add("到户外散步晒晒太阳");
                actions.add("写一篇情绪日记梳理心情");
            }
            default -> {
                actions.add("保持规律作息");
                actions.add("适当运动");
                actions.add("与朋友多交流");
            }
        }
        return actions;
    }

    private String joinText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (StrUtil.isNotBlank(p)) {
                sb.append(p).append(' ');
            }
        }
        return sb.toString();
    }

    private boolean containsAny(String text, String[] words) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private int countMatches(String text, String[] words) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        int count = 0;
        for (String w : words) {
            int idx = 0;
            while ((idx = text.indexOf(w, idx)) != -1) {
                count++;
                idx += w.length();
            }
        }
        return count;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
