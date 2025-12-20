package com.peng.clouddrivelite.control;

import com.peng.clouddrivelite.dto.ApiResponse;
import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.repository.UserRepository;
import com.peng.clouddrivelite.service.UserService;
import com.peng.clouddrivelite.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
     * 从 Session 中获取当前用户 ID
     */
    private Long requireUserId(HttpSession session) {
        Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userIdObj == null) {
        throw new RuntimeException("未登录");
        }
        return Long.valueOf(userIdObj.toString());
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getCurrentUserInfo(HttpSession session) {
        try {
            Long userId = requireUserId(session);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            Map<String, Object> userInfo = userService.buildUserInfo(user);
            return ApiResponse.success("获取用户信息成功", userInfo);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
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
        try {
            Long userId = requireUserId(session);
            Map<String, Object> result = userService.updateUserProfile(userId, username, phoneNumber, email);
            return ApiResponse.success("更新成功", result);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ApiResponse<Map<String, Object>> changePassword(
            @RequestParam @NotBlank String oldPassword,
            @RequestParam @NotBlank String newPassword,
            HttpSession session) {
        try {
            Long userId = requireUserId(session);
            userService.changePassword(userId, oldPassword, newPassword);
            return ApiResponse.success("密码修改成功，请重新登录", Map.of());
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户的存储信息
     */
    @GetMapping("/storage")
    public ApiResponse<Map<String, Object>> getCurrentUserStorage(HttpSession session) {
        try {
            Long userId = requireUserId(session);
            Map<String, Object> stats = userService.getUserStorageStats(userId);
            return ApiResponse.success("获取存储信息成功", stats);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

