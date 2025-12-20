package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.repository.UserRepository;
import com.peng.clouddrivelite.util.PasswordUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FileService fileService;

    public UserService(UserRepository userRepository, FileService fileService) {
        this.userRepository = userRepository;
        this.fileService = fileService;
    }
    // 根据用户编号查找用户（可能为空）
    public Optional<User> findByUserNumber(String userNumber) {
        return userRepository.findByUserNumber(userNumber);
    }

    // 根据用户号和手机号查找用户（用于找回密码验证）
    public Optional<User> findByUserNumberAndPhoneNumber(String userNumber, String phoneNumber) {
        return userRepository.findByUserNumberAndPhoneNumber(userNumber, phoneNumber);
    }

    // 根据用户号和邮箱查找用户（用于找回密码验证）
    public Optional<User> findByUserNumberAndEmail(String userNumber, String email) {
        return userRepository.findByUserNumberAndEmail(userNumber, email);
    }

    // 保存用户（用于更新密码等操作）
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    // 判断用户编号是否已存在
    public boolean existsByUserNumber(String userNumber) {
        return userRepository.existsByUserNumber(userNumber);
    }
    // 判断用户名是否已存在
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    // 判断手机号是否已存在
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }
    // 判断邮箱是否已存在
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User register(String username,
                         String userNumber,
                         String phoneNumber,
                         String rawPassword,
                         String email) {
        User user = new User(username, userNumber, phoneNumber,
                PasswordUtil.hash(rawPassword), email);
        return userRepository.save(user);
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return PasswordUtil.matches(rawPassword, user.getPassword());
    }

    /**
     * 更新用户存储空间
     */
    @Transactional
    public void updateUserStorage(Long userId, Long maxStorage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (maxStorage < 0) {
            throw new RuntimeException("存储空间不能为负数");
        }
        user.setMaxStorage(maxStorage);
        userRepository.save(user);
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (status != 0 && status != 1) {
            throw new RuntimeException("状态值无效，只能为0（禁用）或1（正常）");
        }
        user.setStatus(status);
        userRepository.save(user);
    }

    /**
     * 更新用户角色
     */
    @Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new RuntimeException("角色值无效，只能为USER或ADMIN");
        }
        user.setRole(role);
        userRepository.save(user);
    }

    /**
     * 分页查询用户列表
     */
    public Page<User> listUsers(String keyword, Integer status, String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.searchUsers(keyword, status, role, pageable);
    }

    /**
     * 获取用户存储统计
     */
    public Map<String, Object> getUserStorageStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        long usedStorage = fileService.calculateUserStorage(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("username", user.getUsername());
        stats.put("userNumber", user.getUserNumber());
        stats.put("maxStorage", user.getMaxStorage());
        stats.put("usedStorage", usedStorage);
        stats.put("usagePercent", user.getMaxStorage() > 0 
                ? (usedStorage * 100.0 / user.getMaxStorage()) 
                : 0.0);
        
        return stats;
    }

    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> "ADMIN".equals(user.getRole()))
                .orElse(false);
    }

    /**
     * 构建当前用户信息 Map（用于前端展示）
     */
    public Map<String, Object> buildUserInfo(User user) {
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
            userInfo.put("createdAt", user.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            userInfo.put("createdAt", "");
        }
        return userInfo;
    }

    /**
     * 更新当前用户的基础信息（用户名、手机号、邮箱）
     */
    @Transactional
    public Map<String, Object> updateUserProfile(Long userId,
                                                 String username,
                                                 String phoneNumber,
                                                 String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新用户名
        if (username != null && !username.trim().isEmpty()) {
            if (userRepository.existsByUsername(username) && !username.equals(user.getUsername())) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(username);
        }

        // 更新手机号
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            if (userRepository.existsByPhoneNumber(phoneNumber)
                    && !phoneNumber.equals(user.getPhoneNumber())) {
                throw new RuntimeException("手机号已存在");
            }
            if (!phoneNumber.matches("^1[3-9]\\d{9}$")) {
                throw new RuntimeException("手机号格式不正确");
            }
            user.setPhoneNumber(phoneNumber);
        }

        // 更新邮箱
        if (email != null) {
            if (!email.trim().isEmpty()
                    && userRepository.existsByEmail(email)
                    && !email.equals(user.getEmail())) {
                throw new RuntimeException("邮箱已存在");
            }
            user.setEmail(email.trim().isEmpty() ? null : email);
        }

        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("phoneNumber", user.getPhoneNumber());
        result.put("email", user.getEmail());
        return result;
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId,
                               String oldPassword,
                               String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证旧密码
        if (!verifyPassword(user, oldPassword)) {
            throw new RuntimeException("当前密码错误");
        }

        // 验证新密码长度
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码长度至少6位");
        }

        // 更新密码
        user.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(user);
    }

    /**
     * 找回密码：通过用户号 + 手机号/邮箱验证后重置密码
     */
    @Transactional
    public void resetPassword(String userNumber,
                              String phoneNumber,
                              String email,
                              String newPassword) {
        if ((phoneNumber == null || phoneNumber.isBlank()) &&
            (email == null || email.isBlank())) {
            throw new RuntimeException("请提供手机号或邮箱");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("密码长度至少6位");
        }

        Optional<User> userOpt;
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            userOpt = findByUserNumberAndPhoneNumber(userNumber, phoneNumber);
        } else {
            userOpt = findByUserNumberAndEmail(userNumber, email);
        }

        User user = userOpt.orElseThrow(() -> new RuntimeException("用户不存在或信息不匹配"));

        user.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(user);
    }
}


