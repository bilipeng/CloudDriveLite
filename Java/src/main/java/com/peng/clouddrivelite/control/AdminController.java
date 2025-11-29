package com.peng.clouddrivelite.control;

import com.peng.clouddrivelite.dto.ApiResponse;
import com.peng.clouddrivelite.entity.LoginLog;
import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.service.AdminService;
import com.peng.clouddrivelite.service.FileService;
import com.peng.clouddrivelite.service.LoginLogService;
import com.peng.clouddrivelite.service.UserService;
import com.peng.clouddrivelite.util.SessionKeys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;
    private final LoginLogService loginLogService;
    private final FileService fileService;

    public AdminController(UserService userService, AdminService adminService, 
                          LoginLogService loginLogService, FileService fileService) {
        this.userService = userService;
        this.adminService = adminService;
        this.loginLogService = loginLogService;
        this.fileService = fileService;
    }

    /**
     * 检查当前用户是否为管理员
     */
    private void requireAdmin(HttpSession session) {
        // 首先检查管理员 Session 是否已激活
        Object adminActivated = session.getAttribute(SessionKeys.SESSION_ADMIN_ACTIVATED);
        if (adminActivated == null || !Boolean.TRUE.equals(adminActivated)) {
            throw new RuntimeException("管理员 Session 未激活，请先激活管理员 Session");
        }
        
        // 验证用户 ID 和权限
        Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userIdObj == null) {
            throw new RuntimeException("未登录");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        if (!userService.isAdmin(userId)) {
            throw new RuntimeException("需要管理员权限");
        }
    }

    /**
     * 激活管理员 Session
     * 从客户端 Session 验证用户是否为管理员，如果是，则激活管理员 Session
     */
    @PostMapping("/activate")
    public ApiResponse<Map<String, Object>> activateAdminSession(HttpSession session) {
        try {
            // 从客户端 Session 获取用户 ID
            Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
            if (userIdObj == null) {
                return ApiResponse.error("未登录");
            }
            
            Long userId = Long.valueOf(userIdObj.toString());
            
            // 验证用户是否为管理员
            if (!userService.isAdmin(userId)) {
                return ApiResponse.error("您不是管理员，无法激活管理员 Session");
            }
            
            // 激活管理员 Session
            session.setAttribute(SessionKeys.SESSION_ADMIN_ACTIVATED, true);
            
            Map<String, Object> result = new HashMap<>();
            result.put("activated", true);
            result.put("message", "管理员 Session 已激活");
            
            return ApiResponse.success("管理员 Session 激活成功", result);
        } catch (Exception e) {
            return ApiResponse.error("激活失败: " + e.getMessage());
        }
    }

    // ==================== 用户管理接口 ====================

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        try {
            requireAdmin(session);
            Page<User> users = userService.listUsers(keyword, status, role, page - 1, size);
            
            // 为每个用户计算存储使用情况
            List<Map<String, Object>> userList = users.getContent().stream().map(user -> {
                long usedStorage = fileService.calculateUserStorage(user.getId());
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("userNumber", user.getUserNumber());
                userMap.put("email", user.getEmail());
                userMap.put("phoneNumber", user.getPhoneNumber());
                userMap.put("role", user.getRole());
                userMap.put("status", user.getStatus());
                userMap.put("maxStorage", user.getMaxStorage());
                userMap.put("usedStorage", usedStorage);
                userMap.put("usagePercent", user.getMaxStorage() > 0 
                        ? (usedStorage * 100.0 / user.getMaxStorage()) 
                        : 0.0);
                userMap.put("createdAt", user.getCreatedAt());
                return userMap;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", userList);
            result.put("page", page);
            result.put("size", size);
            result.put("total", users.getTotalElements());
            
            return ApiResponse.success("获取用户列表成功", result);
        } catch (Exception e) {
            return ApiResponse.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户存储空间
     */
    @PutMapping("/users/{id}/storage")
    public ApiResponse<Map<String, Object>> updateUserStorage(
            @PathVariable Long id,
            @RequestParam Long maxStorage,
            HttpSession session) {
        try {
            requireAdmin(session);
            
            long usedStorage = fileService.calculateUserStorage(id);
            if (maxStorage < usedStorage) {
                return ApiResponse.error("存储空间不能小于已用空间: " + 
                        String.format("%.2f", usedStorage / 1024.0 / 1024.0 / 1024.0) + " GB");
            }
            
            userService.updateUserStorage(id, maxStorage);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", id);
            result.put("maxStorage", maxStorage);
            result.put("usedStorage", usedStorage);
            
            return ApiResponse.success("更新存储空间成功", result);
        } catch (Exception e) {
            return ApiResponse.error("更新存储空间失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @PutMapping("/users/{id}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            HttpSession session) {
        try {
            requireAdmin(session);
            userService.updateUserStatus(id, status);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", id);
            result.put("status", status);
            
            return ApiResponse.success(status == 1 ? "用户已启用" : "用户已禁用", result);
        } catch (Exception e) {
            return ApiResponse.error("更新用户状态失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    public ApiResponse<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            HttpSession session) {
        try {
            requireAdmin(session);
            userService.updateUserRole(id, role);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", id);
            result.put("role", role);
            
            return ApiResponse.success("更新用户角色成功", result);
        } catch (Exception e) {
            return ApiResponse.error("更新用户角色失败: " + e.getMessage());
        }
    }

    // ==================== 系统监控接口 ====================

    /**
     * 获取系统概览
     */
    @GetMapping("/system/overview")
    public ApiResponse<Map<String, Object>> getSystemOverview(HttpSession session) {
        try {
            requireAdmin(session);
            Map<String, Object> overview = adminService.getSystemOverview();
            return ApiResponse.success("获取系统概览成功", overview);
        } catch (Exception e) {
            return ApiResponse.error("获取系统概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取存储使用排行
     */
    @GetMapping("/system/storage/ranking")
    public ApiResponse<List<Map<String, Object>>> getStorageRanking(
            @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {
        try {
            requireAdmin(session);
            List<Map<String, Object>> ranking = adminService.getUserStorageRanking(limit);
            return ApiResponse.success("获取存储排行成功", ranking);
        } catch (Exception e) {
            return ApiResponse.error("获取存储排行失败: " + e.getMessage());
        }
    }

    /**
     * 获取存储统计详情
     */
    @GetMapping("/system/storage/statistics")
    public ApiResponse<Map<String, Object>> getStorageStatistics(HttpSession session) {
        try {
            requireAdmin(session);
            Map<String, Object> stats = adminService.getStorageStatistics();
            return ApiResponse.success("获取存储统计成功", stats);
        } catch (Exception e) {
            return ApiResponse.error("获取存储统计失败: " + e.getMessage());
        }
    }

    // ==================== 登录日志接口 ====================

    /**
     * 获取登录日志列表
     */
    @GetMapping("/logs/login")
    public ApiResponse<Map<String, Object>> getLoginLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String loginStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        try {
            requireAdmin(session);
            Page<LoginLog> logs = loginLogService.getLoginLogs(userId, startDate, endDate, loginStatus, page - 1, size);
            
            Map<String, Object> result = new HashMap<>();
            result.put("items", logs.getContent());
            result.put("page", page);
            result.put("size", size);
            result.put("total", logs.getTotalElements());
            
            return ApiResponse.success("获取登录日志成功", result);
        } catch (Exception e) {
            return ApiResponse.error("获取登录日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取登录统计
     */
    @GetMapping("/logs/login/statistics")
    public ApiResponse<Map<String, Object>> getLoginStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpSession session) {
        try {
            requireAdmin(session);
            if (startDate == null) {
                startDate = LocalDateTime.now().minusDays(7); // 默认最近7天
            }
            if (endDate == null) {
                endDate = LocalDateTime.now();
            }
            Map<String, Object> stats = loginLogService.getLoginStatistics(startDate, endDate);
            return ApiResponse.success("获取登录统计成功", stats);
        } catch (Exception e) {
            return ApiResponse.error("获取登录统计失败: " + e.getMessage());
        }
    }
}

