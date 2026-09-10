package com.example.aispringboot.dto.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户更新个人资料入参。
 * username 不可修改；userType/status 不允许前端自助修改。
 */
@Data
public class UserProfileUpdateDTO {

    @Email(message = "邮箱格式错误")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    private Integer gender;

    private LocalDate birthday;

    @Size(max = 255, message = "头像路径长度不能超过255个字符")
    private String avatar;
}
