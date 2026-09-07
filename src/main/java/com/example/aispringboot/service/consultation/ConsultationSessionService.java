package com.example.aispringboot.service.consultation;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispringboot.dto.command.ConsultationSessionCreateDTO;
import com.example.aispringboot.dto.response.SessionListItemVO;
import com.example.aispringboot.entity.ConsultationMessage;
import com.example.aispringboot.entity.ConsultationSession;
import com.example.aispringboot.entity.User;
import com.example.aispringboot.exception.BusinessException;
import com.example.aispringboot.mapper.ConsultationMessageMapper;
import com.example.aispringboot.mapper.ConsultationSessionMapper;
import com.example.aispringboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConsultationSessionService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;
    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        ConsultationSession session = ConsultationSession.builder()
                .userId(userId)
                .sessionTitle(StrUtil.isBlank(createDTO.getSessionTitle())
                        ? String.format("宁渡AI助手 - %s", DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm"))
                        : createDTO.getSessionTitle())
                .startedAt(LocalDateTime.now())
                .build();
        consultationSessionMapper.insert(session);
        return session;
    }

    public Page<SessionListItemVO> sessionPage(long current, long size, Long userId) {
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, ConsultationSession::getUserId, userId)
                .orderByDesc(ConsultationSession::getStartedAt);
        Page<ConsultationSession> page = consultationSessionMapper.selectPage(new Page<>(current, size), wrapper);
        Page<SessionListItemVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SessionListItemVO> voList = page.getRecords().stream().map(session -> {
            SessionListItemVO vo = new SessionListItemVO();
            vo.setId(session.getId());
            vo.setUserId(session.getUserId());
            vo.setSessionTitle(session.getSessionTitle());
            vo.setStartedAt(session.getStartedAt());
            vo.setDurationMinutes(0L);
            vo.setMessageCount(0);
            return vo;
        }).collect(Collectors.toList());
        fillUserInfo(voList);
        fillMessageSummary(voList);
        resultPage.setRecords(voList);
        return resultPage;
    }

    /** 批量补齐用户昵称，避免逐条查询的 N+1 问题。 */
    private void fillUserInfo(List<SessionListItemVO> voList) {
        Set<Long> userIds = voList.stream().map(SessionListItemVO::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId,userIds)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        for (SessionListItemVO vo : voList) {
            User user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setUserNickname(StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
        }
    }

    /** 批量补齐消息数与最后一条消息摘要。 */
    private void fillMessageSummary(List<SessionListItemVO> voList) {
        Set<Long> sessionIds = voList.stream().map(SessionListItemVO::getId).collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return;
        }
        List<ConsultationMessage> messages = consultationMessageMapper.selectList(
                new LambdaQueryWrapper<ConsultationMessage>().in(ConsultationMessage::getSessionId, sessionIds));
        Map<Long, List<ConsultationMessage>> messageMap = messages.stream()
                .collect(Collectors.groupingBy(ConsultationMessage::getSessionId));
        for (SessionListItemVO vo : voList) {
            List<ConsultationMessage> sessionMessages = messageMap.get(vo.getId());
            if (sessionMessages == null || sessionMessages.isEmpty()) {
                continue;
            }
            vo.setMessageCount(sessionMessages.size());
            sessionMessages.stream().max(Comparator.comparing(ConsultationMessage::getCreatedAt)).ifPresent(last -> {
                vo.setLastMessageContent(last.getContent());
                vo.setLastMessageTime(last.getCreatedAt());
            });
        }
    }

    public ConsultationSession getSessionById(Long id) {
        ConsultationSession session = consultationSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long id) {
        getSessionById(id);
        consultationMessageMapper.delete(new LambdaQueryWrapper<ConsultationMessage>().eq(ConsultationMessage::getSessionId, id));
        consultationSessionMapper.deleteById(id);
    }
}
