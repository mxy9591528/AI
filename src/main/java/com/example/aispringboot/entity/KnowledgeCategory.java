package com.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value="knowledge_category")
public class KnowledgeCategory {
    @TableId(type=IdType.AUTO)
    private Long id;
    @TableField(value="parent_id")
    private Long parentId;
    @TableField(value="category_name")
    private String categoryName;
    @TableField(value="category_code")
    private String categoryCode;
    private String description;
    @TableField(value="sort_order")
    private Integer sortOrder;
    private Integer status;
    @TableField(value="created_at", fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value="updated_at", fill=FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

