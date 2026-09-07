package com.example.aispringboot.service.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aispringboot.dto.command.UserLoginCommandDTO;
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
}
