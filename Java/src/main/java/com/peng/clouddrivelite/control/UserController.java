package com.peng.clouddrivelite.control;

import com.peng.clouddrivelite.dto.ApiResponse;
import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.repository.UserRepository;
import com.peng.clouddrivelite.service.UserService;
import com.peng.clouddrivelite.util.PasswordUtil;
import com.peng.clouddrivelite.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getCurrentUserInfo(HttpSession session) {
        Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userIdObj == null) {
            return ApiResponse.error("未登录");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("userNumber", user.getUserNumber());
        userInfo.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        userInfo.put("email", user.getEmail() != null ? user.getEmail() : "");
        userInfo.put("role", user.getRole());
        userInfo.put("status", user.getStatus());
        // 格式化日期时间
        if (user.getCreatedAt() != null) {
            userInfo.put("createdAt", user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            userInfo.put("createdAt", "");
        }
        
        return ApiResponse.success("获取用户信息成功", userInfo);
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/info")
    public ApiResponse<Map<String, Object>> updateCurrentUserInfo(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email,
            HttpSession session) {
        Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userIdObj == null) {
            return ApiResponse.error("未登录");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 更新用户名
        if (username != null && !username.trim().isEmpty()) {
            if (userRepository.existsByUsername(username) && !username.equals(user.getUsername())) {
                return ApiResponse.error("用户名已存在");
            }
            user.setUsername(username);
        }
        
        // 更新手机号
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            if (userRepository.existsByPhoneNumber(phoneNumber) && !phoneNumber.equals(user.getPhoneNumber())) {
                return ApiResponse.error("手机号已存在");
            }
            if (!phoneNumber.matches("^1[3-9]\\d{9}$")) {
                return ApiResponse.error("手机号格式不正确");
            }
            user.setPhoneNumber(phoneNumber);
        }
        
        // 更新邮箱
        if (email != null) {
            if (!email.trim().isEmpty() && userRepository.existsByEmail(email) && !email.equals(user.getEmail())) {
                return ApiResponse.error("邮箱已存在");
            }
            user.setEmail(email.trim().isEmpty() ? null : email);
        }
        
        userRepository.save(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("phoneNumber", user.getPhoneNumber());
        result.put("email", user.getEmail());
        
        return ApiResponse.success("更新成功", result);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ApiResponse<Map<String, Object>> changePassword(
            @RequestParam @NotBlank String oldPassword,
            @RequestParam @NotBlank String newPassword,
            HttpSession session) {
        Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userIdObj == null) {
            return ApiResponse.error("未登录");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证旧密码
        if (!userService.verifyPassword(user, oldPassword)) {
            return ApiResponse.error("当前密码错误");
        }
        
        // 验证新密码长度
        if (newPassword.length() < 6) {
            return ApiResponse.error("新密码长度至少6位");
        }
        
        // 更新密码
        user.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(user);
        
        return ApiResponse.success("密码修改成功，请重新登录", Map.of());
    }

    /**
     * 获取当前用户的存储信息
     */
    @GetMapping("/storage")
    public ApiResponse<Map<String, Object>> getCurrentUserStorage(HttpSession session) {
        Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userIdObj == null) {
            return ApiResponse.error("未登录");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        Map<String, Object> stats = userService.getUserStorageStats(userId);
        return ApiResponse.success("获取存储信息成功", stats);
    }
}

