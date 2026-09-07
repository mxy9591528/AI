package com.example.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.command.ArticleStatusCommandDTO;
import com.example.aispringboot.dto.command.KnowledgeArticleCommandDTO;
import com.example.aispringboot.dto.response.KnowledgeArticleVO;
import com.example.aispringboot.entity.KnowledgeCategory;
import com.example.aispringboot.enums.ArticleStatus;
import com.example.aispringboot.service.knowledge.KnowledgeService;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库：分类树 + 文章 CRUD。发布/下线等管理操作仅管理员可用。
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    /** 普通用户只能浏览已发布文章。 */
    private static final Integer PUBLISHED_STATUS = ArticleStatus.PUBLISHED.getCode();

    @Resource
    private KnowledgeService knowledgeService;

    @GetMapping("/category/tree")
    @PreAuthorize("hasRole('2')")
    public Result<List<KnowledgeCategory>> categoryTree() {
        return Result.ok(knowledgeService.categoryTree());
    }

    @GetMapping("/article/page")
    public Result<Page<KnowledgeArticleVO>> articlePage(
            @RequestParam(defaultValue = "1") long currentPage,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        Integer queryStatus = status;
        Integer roleType = JwtTokenUtil.getCurrentRoleType();
        if (queryStatus == null && (roleType == null || roleType != 2)) {
            queryStatus = PUBLISHED_STATUS;
        }
        return Result.ok(knowledgeService.articlePage(currentPage, size, title, categoryId, queryStatus, sortField, sortDirection));
    }

    @PostMapping("/article")
    @PreAuthorize("hasRole('2')")
    public Result<KnowledgeArticleVO> createArticle(@Valid @RequestBody KnowledgeArticleCommandDTO command) {
        return Result.ok(knowledgeService.createArticle(command, JwtTokenUtil.getCurrentUserId()));
    }

    @GetMapping("/article/{id}")
    public Result<KnowledgeArticleVO> articleDetail(@PathVariable String id) {
        return Result.ok(knowledgeService.getArticleDetail(id));
    }

    @PutMapping("/article/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<KnowledgeArticleVO> updateArticle(@PathVariable String id,
                                                    @Valid @RequestBody KnowledgeArticleCommandDTO command) {
        return Result.ok(knowledgeService.updateArticle(id, command));
    }

    @PutMapping("/article/{id}/status")
    @PreAuthorize("hasRole('2')")
    public Result<Void> updateStatus(@PathVariable String id, @Valid @RequestBody ArticleStatusCommandDTO command) {
        knowledgeService.updateStatus(id, command.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/article/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> deleteArticle(@PathVariable String id) {
        knowledgeService.deleteArticle(id);
        return Result.ok();
    }
}
