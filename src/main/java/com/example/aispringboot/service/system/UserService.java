package com.example.aispringboot.service.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aispringboot.dto.command.UserLoginCommandDTO;
import com.example.aispringboot.dto.command.UserPasswordUpdateDTO;
import com.example.aispringboot.dto.command.UserProfileUpdateDTO;
import com.example.aispringboot.dto.command.UserRegisterCommandDTO;
import com.example.aispringboot.dto.response.UserLoginResponseDTO;
import com.example.aispringboot.entity.User;
import com.example.aispringboot.enums.UserType;
import com.example.aispringboot.exception.BusinessException;
import com.example.aispringboot.mapper.UserMapper;
import com.example.aispringboot.service.convert.UserConvert;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Resource
    private UserMapper userMapper;

    public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
        String account = commandDTO.getUsername().trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, account).or().eq(User::getEmail, account)));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PASSWORD_ENCODER.matches(commandDTO.getPassword().trim(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return UserConvert.entityToLoginResponse(token, UserConvert.entityToDetailResponse(user));
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
        if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }
        if (!UserType.isValidCode(commandDTO.getUserType())) {
            throw new BusinessException("无效的用户类型");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, commandDTO.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, commandDTO.getEmail())) > 0) {
            throw new BusinessException("邮箱已存在");
        }
        // 可选字段空白归一为 null，避免空串入库
        if (commandDTO.getPhone() != null && commandDTO.getPhone().isBlank()) {
            commandDTO.setPhone(null);
        }
        if (commandDTO.getNickname() != null && commandDTO.getNickname().isBlank()) {
            commandDTO.setNickname(null);
        }
        String encodedPassword = PASSWORD_ENCODER.encode(commandDTO.getPassword().trim());
        User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);
        userMapper.insert(user);
        log.info("新用户注册: userId={}, username={}", user.getId(), user.getUsername());
        return UserConvert.entityToDetailResponse(user);
    }

    /**
     * 供 JWT 过滤器逐请求调用，走 Redis 缓存（30 分钟 TTL）减轻数据库压力。
     */
    @Cacheable(cacheNames = "user:info", key = "#userId", unless = "#result == null")
    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserConvert.entityToDetailResponse(user);
    }

    /**
     * 用户自助更新个人资料。username/userType/status 不允许通过此接口修改。
     */
    @CacheEvict(cacheNames = "user:info", key = "#userId")
    public UserLoginResponseDTO.UserDetailResponseDTO updateProfile(Long userId, UserProfileUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 邮箱唯一校验（排除自己）
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, dto.getEmail().trim())
                    .ne(User::getId, userId));
            if (count != null && count > 0) {
                throw new BusinessException("邮箱已被其他账号使用");
            }
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname().isBlank() ? null : dto.getNickname().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            user.setBirthday(dto.getBirthday());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar().isBlank() ? null : dto.getAvatar().trim());
        }
        userMapper.updateById(user);
        log.info("用户更新资料: userId={}", userId);
        return UserConvert.entityToDetailResponse(user);
    }

    /**
     * 用户修改密码：校验原密码，两次新密码一致后更新。
     */
    @CacheEvict(cacheNames = "user:info", key = "#userId")
    public void updatePassword(Long userId, UserPasswordUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PASSWORD_ENCODER.matches(dto.getOldPassword().trim(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(PASSWORD_ENCODER.encode(dto.getNewPassword().trim()));
        userMapper.updateById(user);
        log.info("用户修改密码: userId={}", userId);
    }
}
