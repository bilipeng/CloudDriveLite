package com.peng.clouddrivelite.repository;

import com.peng.clouddrivelite.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 根据用户编号查找用户（可能为空）
    Optional<User> findByUserNumber(String userNumber);

    // 判断用户编号是否已存在
    boolean existsByUserNumber(String userNumber);

    // 判断用户名是否已存在
    boolean existsByUsername(String username);

    // 判断手机号是否已存在
    boolean existsByPhoneNumber(String phoneNumber);

    // 判断邮箱是否已存在
    boolean existsByEmail(String email);
}
