package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.repository.UserRepository;
import com.peng.clouddrivelite.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    // 根据用户编号查找用户（可能为空）
    public Optional<User> findByUserNumber(String userNumber) {
        return userRepository.findByUserNumber(userNumber);
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
}


