package com.example.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.command.EmotionDiaryCreateDTO;
import com.example.aispringboot.dto.response.EmotionDiaryVO;
import com.example.aispringboot.service.emotion.EmotionDiaryService;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    /** 查询当前用户今天的情绪日记（不存在则返回 null），用于前端加载已有记录进行修改。 */
    @GetMapping("/today")
    public Result<EmotionDiaryVO> getTodayDiary() {
        Long userId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(emotionDiaryService.getTodayDiary(userId));
    }

    /** 当前用户的历史情绪日记分页，支持按日记日期范围筛选。 */
    @GetMapping("/page")
    public Result<Page<EmotionDiaryVO>> userPage(
            @RequestParam(required = false) Long currentPage,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(name = "diaryDateStart", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate diaryDateStart,
            @RequestParam(name = "diaryDateEnd", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate diaryDateEnd) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        long page = currentPage != null ? currentPage : (current != null ? current : (pageNum != null ? pageNum : 1L));
        long pageSizeVal = size != null ? size : (pageSize != null ? pageSize : 10L);
        return Result.ok(emotionDiaryService.adminPage(page, pageSizeVal, userId, null,
                null, diaryDateStart, diaryDateEnd));
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
            @RequestParam(name = "moodScoreRange", required = false) String moodScoreRange,
            /** 按用户名/昵称模糊搜索 */
            @RequestParam(required = false) String username,
            /** 日记日期范围起止，格式 yyyy-MM-dd */
            @RequestParam(name = "diaryDateStart", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate diaryDateStart,
            @RequestParam(name = "diaryDateEnd", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate diaryDateEnd) {
        long page = currentPage != null ? currentPage : (current != null ? current : (pageNum != null ? pageNum : 1L));
        long pageSizeVal = size != null ? size : (pageSize != null ? pageSize : 10L);
        return Result.ok(emotionDiaryService.adminPage(page, pageSizeVal, userId, moodScoreRange,
                username, diaryDateStart, diaryDateEnd));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> deleteDiary(@PathVariable Long id) {
        emotionDiaryService.deleteDiary(id);
        return Result.ok();
    }
}
