package com.example.aispringboot.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeArticleCommandDTO {
    private String id;
    @NotBlank(message="文章标题不能为空")
    @Size(max=200, message="文章标题最多200个字符")
    private @NotBlank(message="文章标题不能为空") @Size(max=200, message="文章标题最多200个字符") String title;
    @NotNull(message="请选择文章分类")
    private @NotNull(message="请选择文章分类") Long categoryId;
    @Size(max=1000, message="摘要最多1000个字符")
    private @Size(max=1000, message="摘要最多1000个字符") String summary;
    @NotBlank(message="文章内容不能为空")
    private @NotBlank(message="文章内容不能为空") String content;
    private String coverImage;
    private String tags;
}

