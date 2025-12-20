package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.entity.FileObject;
import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.repository.FileRepository;
import com.peng.clouddrivelite.repository.LoginLogRepository;
import com.peng.clouddrivelite.repository.UserRepository;
import com.peng.clouddrivelite.util.SessionKeys;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final LoginLogRepository loginLogRepository;

    public AdminService(UserRepository userRepository, 
                       FileRepository fileRepository,
                       LoginLogRepository loginLogRepository) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.loginLogRepository = loginLogRepository;
    }

    /**
     * 从 Session 中获取当前用户 ID
     */
    public Long requireUserId(HttpSession session) {
        Object userIdObj = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userIdObj == null) {
            throw new RuntimeException("未登录");
        }
        return Long.valueOf(userIdObj.toString());
    }

    /**
     * 检查当前 Session 是否已激活管理员会话且用户为管理员
     */
    public void requireAdmin(HttpSession session) {
        Object adminActivated = session.getAttribute(SessionKeys.SESSION_ADMIN_ACTIVATED);
        if (adminActivated == null || !Boolean.TRUE.equals(adminActivated)) {
            throw new RuntimeException("管理员 Session 未激活，请先激活管理员 Session");
        }
        Long userId = requireUserId(session);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("需要管理员权限");
        }
    }

    /**
     * 获取系统概览数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 用户统计
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatusAndRole(1, "USER");
        long adminCount = userRepository.countByRole("ADMIN");
        
        // 文件统计
        long totalFiles = fileRepository.count();
        Specification<FileObject> folderSpec = (root, query, cb) -> 
            cb.equal(root.get("isFolder"), true);
        long totalFolders = fileRepository.count(folderSpec);
        
        // 存储统计（使用 Specification 查询非文件夹文件）
        Specification<FileObject> fileSpec = (root, query, cb) -> 
            cb.equal(root.get("isFolder"), false);
        List<FileObject> allFiles = fileRepository.findAll(fileSpec);
        Long totalStorage = allFiles.stream()
                .mapToLong(FileObject::getFileSize)
                .sum();
        
        // 今日统计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayLogins = loginLogRepository.countByLoginTimeAfterAndLoginStatus(todayStart, "SUCCESS");
        
        Specification<FileObject> todayUploadSpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("uploadedTime"), todayStart));
            predicates.add(cb.equal(root.get("isFolder"), false));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        long todayUploads = fileRepository.count(todayUploadSpec);
        
        overview.put("totalUsers", totalUsers);
        overview.put("activeUsers", activeUsers);
        overview.put("adminCount", adminCount);
        overview.put("totalFiles", totalFiles);
        overview.put("totalFolders", totalFolders);
        overview.put("totalStorage", totalStorage != null ? totalStorage : 0L);
        overview.put("todayLogins", todayLogins);
        overview.put("todayUploads", todayUploads);
        
        return overview;
    }

    /**
     * 获取用户存储使用排行
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserStorageRanking(int limit) {
        List<User> users = userRepository.findAll();
        Specification<FileObject> fileSpec = (root, query, cb) -> 
            cb.equal(root.get("isFolder"), false);
        List<FileObject> allFiles = fileRepository.findAll(fileSpec);
        
        return users.stream()
                .map(user -> {
                    Long usedStorage = allFiles.stream()
                            .filter(f -> f.getUserId().equals(user.getId()))
                            .mapToLong(FileObject::getFileSize)
                            .sum();
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("userId", user.getId());
                    item.put("username", user.getUsername());
                    item.put("userNumber", user.getUserNumber());
                    item.put("usedStorage", usedStorage);
                    item.put("maxStorage", user.getMaxStorage());
                    item.put("usagePercent", user.getMaxStorage() > 0 
                            ? (usedStorage * 100.0 / user.getMaxStorage()) 
                            : 0.0);
                    return item;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("usedStorage"), (Long) a.get("usedStorage")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取存储统计详情
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 查询所有非文件夹文件
        Specification<FileObject> fileSpec = (root, query, cb) -> 
            cb.equal(root.get("isFolder"), false);
        List<FileObject> allFiles = fileRepository.findAll(fileSpec);
        
        // 总存储使用
        Long totalStorage = allFiles.stream()
                .mapToLong(FileObject::getFileSize)
                .sum();
        
        // 按文件类型统计
        Map<String, Long> typeStats = allFiles.stream()
                .collect(Collectors.groupingBy(
                        FileObject::getFileType,
                        Collectors.summingLong(FileObject::getFileSize)
                ));
        
        // 用户存储分布统计
        long usersOver80 = 0, users50to80 = 0, users20to50 = 0, usersUnder20 = 0;
        
        for (User user : userRepository.findAll()) {
            Long usedStorage = allFiles.stream()
                    .filter(f -> f.getUserId().equals(user.getId()))
                    .mapToLong(FileObject::getFileSize)
                    .sum();
            
            double usagePercent = user.getMaxStorage() > 0 
                    ? (usedStorage * 100.0 / user.getMaxStorage()) 
                    : 0.0;
            
            if (usagePercent >= 80) usersOver80++;
            else if (usagePercent >= 50) users50to80++;
            else if (usagePercent >= 20) users20to50++;
            else usersUnder20++;
        }
        
        stats.put("totalStorage", totalStorage != null ? totalStorage : 0L);
        stats.put("typeStatistics", typeStats);
        stats.put("usersOver80", usersOver80);
        stats.put("users50to80", users50to80);
        stats.put("users20to50", users20to50);
        stats.put("usersUnder20", usersUnder20);
        
        return stats;
    }

    /**
     * 更新系统配置
     */
    @Transactional
    public void updateSystemConfig(String key, String value) {
        // 这里可以扩展 SystemConfigService
        // 暂时留空，后续实现
    }
}

