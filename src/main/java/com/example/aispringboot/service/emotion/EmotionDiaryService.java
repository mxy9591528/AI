package com.example.aispringboot.service.emotion;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispringboot.dto.command.EmotionDiaryCreateDTO;
import com.example.aispringboot.dto.response.EmotionDiaryVO;
import com.example.aispringboot.entity.EmotionDiary;
import com.example.aispringboot.entity.User;
import com.example.aispringboot.exception.BusinessException;
import com.example.aispringboot.mapper.EmotionDiaryMapper;
import com.example.aispringboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmotionDiaryService {

    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    @Resource
    private UserMapper userMapper;

    @SuppressWarnings("null")
    public EmotionDiaryVO getTodayDiary(Long userId) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, java.time.LocalDate.now());
        EmotionDiary diary = emotionDiaryMapper.selectOne(wrapper);
        return diary == null ? null : toVO(diary);
    }

    @SuppressWarnings("null")
    public void saveDiary(Long userId, EmotionDiaryCreateDTO dto) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, dto.getDiaryDate());
        EmotionDiary existing = emotionDiaryMapper.selectOne(wrapper);
        if (existing == null) {
            EmotionDiary diary = new EmotionDiary();
            diary.setUserId(userId);
            diary.setDiaryDate(dto.getDiaryDate());
            applyFields(diary, dto);
            emotionDiaryMapper.insert(diary);
        } else {
            applyFields(existing, dto);
            emotionDiaryMapper.updateById(existing);
        }
    }

    private void applyFields(EmotionDiary diary, EmotionDiaryCreateDTO dto) {
        diary.setMoodScore(dto.getMoodScore());
        diary.setDominantEmotion(dto.getDominantEmotion());
        diary.setEmotionTriggers(dto.getEmotionTriggers());
        diary.setDiaryContent(dto.getDiaryContent());
        diary.setSleepQuality(dto.getSleepQuality());
        diary.setStressLevel(dto.getStressLevel());
    }

    @SuppressWarnings("null")
    public Page<EmotionDiaryVO> adminPage(long current, long size, Long userId, String moodScoreRange,
                                          String username, LocalDate diaryDateStart, LocalDate diaryDateEnd) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        // userId 精确过滤（优先级最高）
        wrapper.eq(userId != null, EmotionDiary::getUserId, userId);

        // 用户名/昵称模糊过滤：先用子查询找到匹配的 userId
        if (StrUtil.isNotBlank(username)) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.and(w -> w.like(User::getUsername, username).or().like(User::getNickname, username));
            List<User> matchedUsers = userMapper.selectList(userWrapper);
            List<Long> matchedUserIds = matchedUsers.stream().map(User::getId).collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return new Page<>(current, size, 0);
            }
            wrapper.in(EmotionDiary::getUserId, matchedUserIds);
        }

        // 日期范围过滤
        wrapper.ge(diaryDateStart != null, EmotionDiary::getDiaryDate, diaryDateStart);
        wrapper.le(diaryDateEnd != null, EmotionDiary::getDiaryDate, diaryDateEnd);

        // 情绪评分区间
        if (StrUtil.isNotBlank(moodScoreRange) && moodScoreRange.contains("-")) {
            try {
                String[] parts = moodScoreRange.split("-");
                wrapper.ge(EmotionDiary::getMoodScore, Integer.parseInt(parts[0].trim()))
                        .le(EmotionDiary::getMoodScore, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException e) {
                // 非法区间格式则忽略过滤条件
            }
        }
        wrapper.orderByDesc(EmotionDiary::getDiaryDate);
        Page<EmotionDiary> page = emotionDiaryMapper.selectPage(new Page<>(current, size), wrapper);
        Page<EmotionDiaryVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<EmotionDiaryVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Set<Long> userIds = voList.stream().map(EmotionDiaryVO::getUserId).collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            for (EmotionDiaryVO vo : voList) {
                User user = userMap.get(vo.getUserId());
                if (user == null) {
                    continue;
                }
                vo.setUsername(user.getUsername());
                vo.setNickname(StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
        }
        resultPage.setRecords(voList);
        return resultPage;
    }

    public void deleteDiary(Long id) {
        EmotionDiary diary = emotionDiaryMapper.selectById(id);
        if (diary == null) {
            throw new BusinessException("记录不存在");
        }
        emotionDiaryMapper.deleteById(id);
    }

    private EmotionDiaryVO toVO(EmotionDiary diary) {
        EmotionDiaryVO vo = new EmotionDiaryVO();
        vo.setId(diary.getId());
        vo.setUserId(diary.getUserId());
        vo.setDiaryDate(diary.getDiaryDate());
        vo.setMoodScore(diary.getMoodScore());
        vo.setDominantEmotion(diary.getDominantEmotion());
        vo.setEmotionTriggers(diary.getEmotionTriggers());
        vo.setDiaryContent(diary.getDiaryContent());
        vo.setSleepQuality(diary.getSleepQuality());
        vo.setStressLevel(diary.getStressLevel());
        vo.setAiEmotionAnalysis(diary.getAiEmotionAnalysis());
        vo.setCreatedAt(diary.getCreatedAt());
        vo.setUpdatedAt(diary.getUpdatedAt());
        return vo;
    }
}
