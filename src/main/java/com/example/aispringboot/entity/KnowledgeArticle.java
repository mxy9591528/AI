package com.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value="knowledge_article")
public class KnowledgeArticle {
    @TableId(type=IdType.ASSIGN_UUID)
    private String id;
    @TableField(value="category_id")
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    @TableField(value="cover_image")
    private String coverImage;
    private String tags;
    @TableField(value="author_id")
    private Long authorId;
    @TableField(value="read_count")
    private Integer readCount;
    private Integer status;
    @TableField(value="published_at")
    private LocalDateTime publishedAt;
    @TableField(value="created_at", fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value="updated_at", fill=FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

