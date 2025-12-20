package com.peng.clouddrivelite.control;

import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.service.LoginLogService;
import com.peng.clouddrivelite.service.UserService;
import com.peng.clouddrivelite.util.RequestUtil;
import com.peng.clouddrivelite.util.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserService userService;
    private final LoginLogService loginLogService;

    public AuthController(UserService userService, LoginLogService loginLogService) {
        this.userService = userService;
        this.loginLogService = loginLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam @NotBlank String username,
                                      @RequestParam @NotBlank String userNumber,
                                      @RequestParam @NotBlank String phoneNumber,
                                      @RequestParam @NotBlank String password,
                                      @RequestParam(required = false) String email) {
        try {
            User user = userService.register(username, userNumber, phoneNumber, password, email);
            return ResponseEntity.ok(Map.of("id", user.getId(), "userNumber", user.getUserNumber()));
        } catch (RuntimeException e) {
            // 业务异常统一映射为 400
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam @NotBlank String userNumber,
                                   @RequestParam @NotBlank String password,
                                   HttpSession session,
                                   HttpServletRequest request) {
        String ipAddress = RequestUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
    return userService.findByUserNumber(userNumber)
                .filter(u -> {
                    boolean success = userService.verifyPassword(u, password);
                    // 记录登录日志
                    if (success) {
                        loginLogService.recordLogin(
                                u.getId(),
                                u.getUserNumber(),
                                u.getUsername(),
                                ipAddress,
                                userAgent,
                                "SUCCESS",
                                null
                        );
                    } else {
                        loginLogService.recordLogin(
                                null,
                                userNumber,
                                null,
                                ipAddress,
                                userAgent,
                                "FAILED",
                                "密码错误"
                        );
                    }
                    return success;
                })
                .<ResponseEntity<?>>map(u -> {
                session.setAttribute(SessionKeys.SESSION_USER_ID, u.getId());
                session.setAttribute(SessionKeys.SESSION_USER_NUMBER, u.getUserNumber());
                // 把 userName 带回去
                return ResponseEntity.ok(Map.of(
                        "message", "登录成功",
                        "userId", u.getId(),
                        "userName", u.getUsername()
                ));
            })
                .orElseGet(() -> {
                    // 用户不存在的情况也记录日志
                    loginLogService.recordLogin(
                            null,
                            userNumber,
                            null,
                            ipAddress,
                            userAgent,
                            "FAILED",
                            "用户不存在"
                    );
                    return ResponseEntity.status(401)
                            .body(Map.of("message", "账号或密码错误"));
                });
}

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "已登出"));
    }

    /**
     * 找回密码（简化版：直接通过用户号+手机号/邮箱验证身份后重置密码）
     * @param userNumber 用户号
     * @param phoneNumber 手机号（可选，与邮箱二选一）
     * @param email 邮箱（可选，与手机号二选一）
     * @param newPassword 新密码
     * @return 响应结果
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam @NotBlank String userNumber,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email,
            @RequestParam @NotBlank String newPassword) {
        try {
            userService.resetPassword(userNumber, phoneNumber, email, newPassword);
            return ResponseEntity.ok(Map.of("message", "密码重置成功，请使用新密码登录"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}


