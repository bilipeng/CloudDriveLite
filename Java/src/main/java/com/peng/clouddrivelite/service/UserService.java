package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.repository.UserRepository;
import com.peng.clouddrivelite.util.PasswordUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}


