package com.peng.clouddrivelite.repository;

import com.peng.clouddrivelite.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    /**
     * 按用户ID分页查询登录日志（按时间倒序）
     */
    Page<LoginLog> findByUserIdOrderByLoginTimeDesc(Long userId, Pageable pageable);

    /**
     * 按时间范围查询登录日志
     */
    List<LoginLog> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 按登录状态统计数量
     */
    long countByLoginStatus(String loginStatus);

    /**
     * 统计指定时间之后的登录次数（按状态）
     */
    long countByLoginTimeAfterAndLoginStatus(LocalDateTime date, String status);

    /**
     * 按用户ID和时间范围查询
     */
    Page<LoginLog> findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(
            Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 按登录状态和时间范围查询
     */
    Page<LoginLog> findByLoginStatusAndLoginTimeBetweenOrderByLoginTimeDesc(
            String loginStatus, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 按用户ID、登录状态和时间范围查询
     */
    Page<LoginLog> findByUserIdAndLoginStatusAndLoginTimeBetweenOrderByLoginTimeDesc(
            Long userId, String loginStatus, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 统计今日登录次数（成功）
     */
    @Query("SELECT COUNT(l) FROM LoginLog l WHERE DATE(l.loginTime) = CURRENT_DATE AND l.loginStatus = 'SUCCESS'")
    long countTodaySuccessfulLogins();

    /**
     * 统计今日登录次数（失败）
     */
    @Query("SELECT COUNT(l) FROM LoginLog l WHERE DATE(l.loginTime) = CURRENT_DATE AND l.loginStatus = 'FAILED'")
    long countTodayFailedLogins();

    /**
     * 统计指定日期范围内的活跃用户数（去重）
     */
    @Query("SELECT COUNT(DISTINCT l.userId) FROM LoginLog l WHERE l.loginTime BETWEEN :start AND :end AND l.loginStatus = 'SUCCESS'")
    long countDistinctUsersByLoginTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}


