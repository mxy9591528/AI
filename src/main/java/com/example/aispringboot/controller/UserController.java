package com.example.aispringboot.controller;

import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.command.UserLoginCommandDTO;
import com.example.aispringboot.dto.command.UserPasswordUpdateDTO;
import com.example.aispringboot.dto.command.UserProfileUpdateDTO;
import com.example.aispringboot.dto.command.UserRegisterCommandDTO;
import com.example.aispringboot.dto.response.UserLoginResponseDTO;
import com.example.aispringboot.service.system.UserService;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户：注册 / 登录 / 登出 / 当前用户信息。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {
        return Result.ok(userService.login(commandDTO));
    }

    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO commandDTO) {
        return Result.ok(userService.register(commandDTO));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // 无状态 JWT：登出由前端清除本地 token 即可
        return Result.ok();
    }

    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser() {
        Long userId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(userService.getUserById(userId));
    }

    /** 更新当前登录用户的个人资料（邮箱/昵称/手机号/性别/生日/头像）。 */
    @PutMapping
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(userService.updateProfile(userId, dto));
    }

    /** 修改当前登录用户的密码：需校验原密码。 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateDTO dto) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        userService.updatePassword(userId, dto);
        return Result.ok();
    }
}
