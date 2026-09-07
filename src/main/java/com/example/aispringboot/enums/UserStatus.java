package com.example.aispringboot.enums;

import lombok.Generated;

/*
 * Exception performing whole class analysis ignored.
 */
public enum UserStatus {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final Integer code;
    private final String description;

    UserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserStatus fromCode(Integer code) {
        for (UserStatus status : UserStatus.values()) {
            if (!status.getCode().equals(code)) continue;
            return status;
        }
        throw new IllegalArgumentException("未知的用户状态代码: " + code);
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

