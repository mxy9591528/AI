package com.example.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.command.EmotionDiaryCreateDTO;
import com.example.aispringboot.dto.response.EmotionDiaryVO;
import com.example.aispringboot.service.emotion.EmotionDiaryService;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 情绪日记：前台按用户保存（同日覆盖更新），管理端分页与删除。
 */
@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {

    @Resource
    private EmotionDiaryService emotionDiaryService;

    @PostMapping
    public Result<Void> addDiary(@Valid @RequestBody EmotionDiaryCreateDTO dto) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        emotionDiaryService.saveDiary(userId, dto);
        return Result.ok();
    }

    @GetMapping("/admin/page")
    @PreAuthorize("hasRole('2')")
    public Result<Page<EmotionDiaryVO>> adminPage(
            @RequestParam(required = false) Long currentPage,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(name = "moodScoreRange", required = false) String moodScoreRange) {
        long page = currentPage != null ? currentPage : (current != null ? current : (pageNum != null ? pageNum : 1L));
        long pageSizeVal = size != null ? size : (pageSize != null ? pageSize : 10L);
        return Result.ok(emotionDiaryService.adminPage(page, pageSizeVal, userId, moodScoreRange));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> deleteDiary(@PathVariable Long id) {
        emotionDiaryService.deleteDiary(id);
        return Result.ok();
    }
}
