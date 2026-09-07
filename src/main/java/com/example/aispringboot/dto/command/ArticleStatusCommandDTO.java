package com.example.aispringboot.dto.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleStatusCommandDTO {
    @NotNull(message="状态不能为空")
    private @NotNull(message="状态不能为空") Integer status;
}

