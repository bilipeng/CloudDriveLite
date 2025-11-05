package com.peng.clouddrivelite.control;

import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.service.UserService;
import com.peng.clouddrivelite.util.SessionKeys;
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

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam @NotBlank String username,
                                      @RequestParam @NotBlank String userNumber,
                                      @RequestParam @NotBlank String phoneNumber,
                                      @RequestParam @NotBlank String password,
                                      @RequestParam(required = false) String email) {
        if (userService.existsByUserNumber(userNumber)) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户号码已存在"));
        }
        if (userService.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }
        if (userService.existsByPhoneNumber(phoneNumber)) {
            return ResponseEntity.badRequest().body(Map.of("message", "手机号已存在"));
        }
        if (email != null && !email.isBlank() && userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "邮箱已存在"));
        }
        User user = userService.register(username, userNumber, phoneNumber, password, email);
        return ResponseEntity.ok(Map.of("id", user.getId(), "userNumber", user.getUserNumber()));
    }

    @PostMapping("/login")
public ResponseEntity<?> login(@RequestParam @NotBlank String userNumber,
                               @RequestParam @NotBlank String password,
                               HttpSession session) {
    return userService.findByUserNumber(userNumber)
            .filter(u -> userService.verifyPassword(u, password))
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
            .orElseGet(() -> ResponseEntity.status(401)
                    .body(Map.of("message", "账号或密码错误")));
}

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "已登出"));
    }
}


