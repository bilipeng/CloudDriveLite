package com.peng.clouddrivelite.repository;

import com.peng.clouddrivelite.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // 按角色查询用户列表
    List<User> findByRole(String role);

    // 按状态和角色分页查询
    Page<User> findByStatusAndRole(Integer status, String role, Pageable pageable);

    // 按角色统计数量
    long countByRole(String role);

    // 按状态和角色统计数量
    long countByStatusAndRole(Integer status, String role);

    // 按用户名或用户号搜索（分页）
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR u.username LIKE %:keyword% OR u.userNumber LIKE %:keyword%) " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:role IS NULL OR :role = '' OR u.role = :role)")
    Page<User> searchUsers(@Param("keyword") String keyword, 
                          @Param("status") Integer status, 
                          @Param("role") String role, 
                          Pageable pageable);

    // 根据用户号和手机号查找用户（用于找回密码验证）
    Optional<User> findByUserNumberAndPhoneNumber(String userNumber, String phoneNumber);

    // 根据用户号和邮箱查找用户（用于找回密码验证）
    Optional<User> findByUserNumberAndEmail(String userNumber, String email);
}
