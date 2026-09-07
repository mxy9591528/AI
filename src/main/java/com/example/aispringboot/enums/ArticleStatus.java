package com.example.aispringboot.enums;

import lombok.Generated;

/*
 * Exception performing whole class analysis ignored.
 */
public enum ArticleStatus {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    private final Integer code;
    private final String description;

    ArticleStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValidCode(Integer code) {
        for (ArticleStatus status : ArticleStatus.values()) {
            if (!status.getCode().equals(code)) continue;
            return true;
        }
        return false;
    }

    @Generated
    public Integer getCode() {
        return this.code;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }
}

