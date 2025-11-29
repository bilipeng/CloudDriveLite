package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.entity.LoginLog;
import com.peng.clouddrivelite.repository.LoginLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    /**
     * 记录登录日志
     */
    @Transactional
    public void recordLogin(Long userId, String userNumber, String username, 
                           String ipAddress, String userAgent, 
                           String loginStatus, String failureReason) {
        LoginLog log = new LoginLog(userId, userNumber, username, ipAddress, 
                                   userAgent, loginStatus, failureReason);
        loginLogRepository.save(log);
    }

    /**
     * 分页查询登录日志
     */
    public Page<LoginLog> getLoginLogs(Long userId, LocalDateTime startDate, 
                                      LocalDateTime endDate, String loginStatus, 
                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        if (userId != null && startDate != null && endDate != null && loginStatus != null) {
            return loginLogRepository.findByUserIdAndLoginStatusAndLoginTimeBetweenOrderByLoginTimeDesc(
                    userId, loginStatus, startDate, endDate, pageable);
        } else if (userId != null && startDate != null && endDate != null) {
            return loginLogRepository.findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(
                    userId, startDate, endDate, pageable);
        } else if (startDate != null && endDate != null && loginStatus != null) {
            return loginLogRepository.findByLoginStatusAndLoginTimeBetweenOrderByLoginTimeDesc(
                    loginStatus, startDate, endDate, pageable);
        } else if (userId != null) {
            return loginLogRepository.findByUserIdOrderByLoginTimeDesc(userId, pageable);
        } else {
            // 查询所有，按时间倒序
            return loginLogRepository.findAll(pageable);
        }
    }

    /**
     * 获取登录统计
     */
    public Map<String, Object> getLoginStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalSuccess = loginLogRepository.countByLoginTimeAfterAndLoginStatus(
                startDate != null ? startDate : LocalDateTime.of(2000, 1, 1, 0, 0), "SUCCESS");
        long totalFailed = loginLogRepository.countByLoginTimeAfterAndLoginStatus(
                startDate != null ? startDate : LocalDateTime.of(2000, 1, 1, 0, 0), "FAILED");
        
        long todaySuccess = loginLogRepository.countTodaySuccessfulLogins();
        long todayFailed = loginLogRepository.countTodayFailedLogins();
        
        long activeUsers = 0;
        if (startDate != null && endDate != null) {
            activeUsers = loginLogRepository.countDistinctUsersByLoginTimeBetween(startDate, endDate);
        }
        
        stats.put("totalSuccess", totalSuccess);
        stats.put("totalFailed", totalFailed);
        stats.put("todaySuccess", todaySuccess);
        stats.put("todayFailed", todayFailed);
        stats.put("activeUsers", activeUsers);
        stats.put("totalLogins", totalSuccess + totalFailed);
        
        return stats;
    }

    /**
     * 获取最近的登录日志
     */
    public List<LoginLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return loginLogRepository.findAll(pageable).getContent();
    }
}


