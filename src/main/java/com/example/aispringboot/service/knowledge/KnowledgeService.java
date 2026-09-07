package com.example.aispringboot.service.knowledge;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispringboot.dto.command.KnowledgeArticleCommandDTO;
import com.example.aispringboot.dto.response.KnowledgeArticleVO;
import com.example.aispringboot.entity.KnowledgeArticle;
import com.example.aispringboot.entity.KnowledgeCategory;
import com.example.aispringboot.entity.User;
import com.example.aispringboot.enums.ArticleStatus;
import com.example.aispringboot.exception.BusinessException;
import com.example.aispringboot.mapper.KnowledgeArticleMapper;
import com.example.aispringboot.mapper.KnowledgeCategoryMapper;
import com.example.aispringboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("publishedAt", "readCount", "updatedAt", "createdAt");

    @Resource
    private KnowledgeArticleMapper articleMapper;
    @Resource
    private KnowledgeCategoryMapper categoryMapper;
    @Resource
    private UserMapper userMapper;

    public List<KnowledgeCategory> categoryTree() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1)
                .orderByAsc(KnowledgeCategory::getSortOrder)
                .orderByAsc(KnowledgeCategory::getId);
        return categoryMapper.selectList(wrapper);
    }

    public Page<KnowledgeArticleVO> articlePage(long current, long size, String title, Long categoryId, Integer status, String sortField, String sortDirection) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(title), KnowledgeArticle::getTitle, title)
                .eq(categoryId != null, KnowledgeArticle::getCategoryId, categoryId)
                .eq(status != null, KnowledgeArticle::getStatus, status);
        if (StrUtil.isNotBlank(sortField) && SORTABLE_FIELDS.contains(sortField)) {
            boolean asc = "asc".equalsIgnoreCase(sortDirection);
            switch (sortField) {
                case "publishedAt" -> wrapper.orderBy(true, asc, KnowledgeArticle::getPublishedAt);
                case "readCount" -> wrapper.orderBy(true, asc, KnowledgeArticle::getReadCount);
                case "createdAt" -> wrapper.orderBy(true, asc, KnowledgeArticle::getCreatedAt);
                default -> wrapper.orderBy(true, asc, KnowledgeArticle::getUpdatedAt);
            }
        } else {
            wrapper.orderByDesc(KnowledgeArticle::getUpdatedAt);
        }
        Page<KnowledgeArticle> page = articleMapper.selectPage(new Page<>(current, size), wrapper);
        Page<KnowledgeArticleVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<KnowledgeArticleVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        fillExtraInfo(voList);
        resultPage.setRecords(voList);
        return resultPage;
    }

    public KnowledgeArticleVO createArticle(KnowledgeArticleCommandDTO command, Long authorId) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(StrUtil.isNotBlank(command.getId()) ? command.getId() : null);
        article.setTitle(command.getTitle());
        article.setCategoryId(command.getCategoryId());
        article.setSummary(command.getSummary());
        article.setContent(command.getContent());
        article.setCoverImage(command.getCoverImage());
        article.setTags(command.getTags());
        article.setAuthorId(authorId);
        article.setReadCount(0);
        article.setStatus(ArticleStatus.DRAFT.getCode());
        article.setPublishedAt(null);
        articleMapper.insert(article);
        return toVO(article);
    }

    public KnowledgeArticleVO getArticleDetail(String id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setReadCount((article.getReadCount() == null ? 0 : article.getReadCount()) + 1);
        articleMapper.updateById(article);
        KnowledgeArticleVO vo = toVO(article);
        fillExtraInfo(List.of(vo));
        return vo;
    }

    public KnowledgeArticleVO updateArticle(String id, KnowledgeArticleCommandDTO command) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setTitle(command.getTitle());
        article.setCategoryId(command.getCategoryId());
        article.setSummary(command.getSummary());
        article.setContent(command.getContent());
        article.setCoverImage(command.getCoverImage());
        article.setTags(command.getTags());
        articleMapper.updateById(article);
        return toVO(article);
    }

    public void updateStatus(String id, Integer status) {
        if (!ArticleStatus.isValidCode(status)) {
            throw new BusinessException("无效的文章状态: " + status);
        }
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setStatus(status);
        if (ArticleStatus.PUBLISHED.getCode().equals(status) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(article);
    }

    @CacheEvict(cacheNames = "knowledge:article", key = "#id")
    public void deleteArticle(String id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        articleMapper.deleteById(id);
    }

    private KnowledgeArticleVO toVO(KnowledgeArticle article) {
        KnowledgeArticleVO vo = new KnowledgeArticleVO();
        vo.setId(article.getId());
        vo.setCategoryId(article.getCategoryId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCoverImage(article.getCoverImage());
        vo.setTags(article.getTags());
        if (StrUtil.isNotBlank(article.getTags())) {
            vo.setTagArray(Arrays.stream(article.getTags().split(","))
                    .map(String::trim)
                    .filter(CharSequenceUtil::isNotBlank)
                    .collect(Collectors.toList()));
        }
        vo.setAuthorId(article.getAuthorId());
        vo.setReadCount(article.getReadCount());
        vo.setStatus(article.getStatus());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }

    private void fillExtraInfo(List<KnowledgeArticleVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        Set<Long> categoryIds = voList.stream().map(KnowledgeArticleVO::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = Map.of();
        if (!categoryIds.isEmpty()) {
            categoryNameMap = categoryMapper.selectList(new LambdaQueryWrapper<KnowledgeCategory>().in(KnowledgeCategory::getId,categoryIds)).stream()
                    .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getCategoryName));
        }
        Set<Long> authorIds = voList.stream().map(KnowledgeArticleVO::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = Map.of();
        if (!authorIds.isEmpty()) {
            userMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId,authorIds)).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
        }
        for (KnowledgeArticleVO vo : voList) {
            vo.setCategoryName(categoryNameMap.get(vo.getCategoryId()));
            User author = userMap.get(vo.getAuthorId());
            if (author == null) {
                continue;
            }
            vo.setAuthorName(StrUtil.isNotBlank(author.getNickname()) ? author.getNickname() : author.getUsername());
        }
    }
}
