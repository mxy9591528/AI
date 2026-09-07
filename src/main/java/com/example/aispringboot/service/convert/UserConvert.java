package com.example.aispringboot.service.convert;

import com.example.aispringboot.dto.command.UserRegisterCommandDTO;
import com.example.aispringboot.dto.response.UserLoginResponseDTO;
import com.example.aispringboot.entity.User;
import com.example.aispringboot.enums.UserStatus;
import java.time.LocalDateTime;

/*
 * Exception performing whole class analysis ignored.
 */
public class UserConvert {
    public static UserLoginResponseDTO.UserDetailResponseDTO entityToDetailResponse(User user) {
        return UserLoginResponseDTO.UserDetailResponseDTO.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail()).nickname(user.getNickname()).avatar(user.getAvatar()).phone(user.getPhone()).gender(user.getGender()).genderDisplayName(UserConvert.getGenderDisplayName((Integer)user.getGender())).birthday(user.getBirthday()).userType(user.getUserType()).userTypeDisplayName(user.getUserTypeDisplayName()).status(user.getStatus()).statusDisplayName(user.getStatusDisplayName()).displayName(user.getDisplayName()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }

    public static UserLoginResponseDTO entityToLoginResponse(String token, UserLoginResponseDTO.UserDetailResponseDTO userInfo) {
        return UserLoginResponseDTO.builder().userInfo(userInfo).token(token).roleType(userInfo.getUserType().toString()).build();
    }

    public static User registerCommandToEntity(UserRegisterCommandDTO commandDTO, String encodedPassword) {
        return User.builder().username(commandDTO.getUsername()).email(commandDTO.getEmail()).password(encodedPassword).nickname(commandDTO.getNickname()).phone(commandDTO.getPhone()).gender(commandDTO.getGender()).birthday(commandDTO.getBirthday()).userType(commandDTO.getUserType()).status(UserStatus.NORMAL.getCode()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private static String getGenderDisplayName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }
}

