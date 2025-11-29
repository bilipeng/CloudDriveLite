# 管理员系统数据库设计说明

## 概述

为了支持管理员系统的以下功能：
1. **存储空间管理** - 设置用户最大存储空间
2. **系统监控** - 查看系统状态和统计数据
3. **用户管理** - 查看用户列表、启用/禁用用户
4. **登录日志** - 记录用户登录历史

## 数据库变更

### 1. users 表扩展

#### 新增字段

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `role` | varchar(20) | 'USER' | 用户角色：USER-普通用户，ADMIN-管理员 |
| `max_storage` | bigint | 10737418240 | 最大存储空间（字节），默认10GB |

#### 索引
- `idx_users_role` - 用于按角色快速查询

#### 使用场景
- **角色判断**：登录时检查用户角色，决定是否有管理员权限
- **存储限制**：上传文件前检查用户已用空间是否超过 `max_storage`
- **管理员查询**：快速筛选所有管理员账号

---

### 2. login_log 表（新建）

#### 表结构

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户ID（外键关联 users.id） |
| `user_number` | varchar(20) | 用户账号（冗余，便于查询） |
| `username` | varchar(20) | 用户名（冗余） |
| `login_time` | datetime(6) | 登录时间 |
| `ip_address` | varchar(45) | IP地址（支持IPv6） |
| `user_agent` | varchar(500) | 浏览器信息 |
| `login_status` | varchar(20) | 登录状态：SUCCESS/FAILED |
| `failure_reason` | varchar(255) | 失败原因（仅失败时） |

#### 索引
- `idx_login_log_user_id` - 按用户查询登录历史
- `idx_login_log_login_time` - 按时间排序
- `idx_login_log_status` - 按状态筛选
- `idx_login_log_user_time` - 复合索引（用户+时间）

#### 使用场景
- **登录历史查询**：管理员查看用户登录记录
- **安全审计**：分析异常登录行为
- **活跃度统计**：统计用户登录频率
- **失败登录监控**：识别可能的暴力破解尝试

#### 数据量预估
- 假设每天100个用户登录，每次登录记录1条
- 每天约100条记录，一年约3.6万条
- 建议：定期清理6个月前的日志（可选）

---

### 3. system_config 表（新建，可选）

#### 表结构

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | bigint | 主键 |
| `config_key` | varchar(100) | 配置键（唯一） |
| `config_value` | text | 配置值 |
| `config_type` | varchar(20) | 配置类型：STRING/NUMBER/BOOLEAN/JSON |
| `description` | varchar(255) | 配置说明 |
| `created_at` | datetime(6) | 创建时间 |
| `updated_at` | datetime(6) | 更新时间 |

#### 默认配置项

| config_key | 默认值 | 说明 |
|------------|--------|------|
| `default_user_storage` | 10737418240 | 新用户默认存储空间（10GB） |
| `max_file_size` | 104857600 | 单文件最大上传大小（100MB） |
| `system_total_storage` | 0 | 系统总存储空间（0=不限制） |
| `enable_user_registration` | true | 是否允许用户注册 |

#### 使用场景
- **系统配置管理**：管理员通过界面修改系统配置
- **动态配置**：无需重启服务即可更新配置
- **配置审计**：记录配置变更历史（可扩展）

---

## 数据查询示例

### 1. 用户存储使用统计

```sql
-- 查询每个用户的存储使用情况
SELECT 
    u.id,
    u.username,
    u.user_number,
    u.role,
    u.max_storage / 1024 / 1024 / 1024 as max_storage_gb,
    COALESCE(SUM(f.file_size), 0) as used_storage,
    COALESCE(SUM(f.file_size), 0) / 1024 / 1024 / 1024 as used_storage_gb,
    (u.max_storage - COALESCE(SUM(f.file_size), 0)) / 1024 / 1024 / 1024 as remaining_storage_gb,
    CASE 
        WHEN COALESCE(SUM(f.file_size), 0) > u.max_storage THEN 'EXCEEDED'
        WHEN COALESCE(SUM(f.file_size), 0) / u.max_storage > 0.9 THEN 'WARNING'
        ELSE 'NORMAL'
    END as storage_status
FROM users u
LEFT JOIN file_object f ON u.id = f.user_id AND f.is_folder = 0
GROUP BY u.id, u.username, u.user_number, u.role, u.max_storage;
```

### 2. 系统总体统计

```sql
-- 系统总览数据
SELECT 
    (SELECT COUNT(*) FROM users WHERE status = 1) as active_users,
    (SELECT COUNT(*) FROM users WHERE role = 'ADMIN') as admin_count,
    (SELECT COUNT(*) FROM file_object WHERE is_folder = 0) as total_files,
    (SELECT COUNT(*) FROM file_object WHERE is_folder = 1) as total_folders,
    (SELECT COALESCE(SUM(file_size), 0) FROM file_object WHERE is_folder = 0) as total_storage_used,
    (SELECT COUNT(*) FROM login_log WHERE DATE(login_time) = CURDATE()) as today_logins;
```

### 3. 登录日志查询

```sql
-- 最近7天的登录统计
SELECT 
    DATE(login_time) as login_date,
    COUNT(*) as login_count,
    COUNT(DISTINCT user_id) as unique_users,
    SUM(CASE WHEN login_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count
FROM login_log
WHERE login_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(login_time)
ORDER BY login_date DESC;
```

### 4. 存储空间排行

```sql
-- 用户存储使用排行（TOP 10）
SELECT 
    u.id,
    u.username,
    u.user_number,
    COALESCE(SUM(f.file_size), 0) / 1024 / 1024 / 1024 as used_storage_gb,
    u.max_storage / 1024 / 1024 / 1024 as max_storage_gb,
    ROUND(COALESCE(SUM(f.file_size), 0) * 100.0 / u.max_storage, 2) as usage_percent
FROM users u
LEFT JOIN file_object f ON u.id = f.user_id AND f.is_folder = 0
WHERE u.status = 1
GROUP BY u.id, u.username, u.user_number, u.max_storage
ORDER BY used_storage_gb DESC
LIMIT 10;
```

---

## 实施步骤

### 1. 执行迁移脚本
```bash
mysql -u root -p CloudDriveLite < docs/admin_system_migration.sql
```

### 2. 验证数据
- 检查 users 表是否添加了新字段
- 检查 login_log 表是否创建成功
- 检查 system_config 表是否有默认数据

### 3. 设置第一个管理员
```sql
-- 将用户ID为1的用户设置为管理员
UPDATE users SET role = 'ADMIN' WHERE id = 1;
```

### 4. 后端代码修改
- 修改 `User.java` 实体类，添加 `role` 和 `max_storage` 字段
- 创建 `LoginLog.java` 实体类
- 创建 `SystemConfig.java` 实体类（可选）
- 修改登录逻辑，记录登录日志
- 修改上传逻辑，检查存储空间限制

### 5. 前端代码修改
- 创建管理员路由和页面
- 添加权限守卫（检查用户角色）
- 实现管理员功能界面

---

## 注意事项

1. **数据迁移安全**：执行迁移前请备份数据库
2. **存储空间单位**：统一使用字节（byte），前端显示时转换为 GB/MB
3. **登录日志清理**：建议定期清理旧日志，避免表过大
4. **性能优化**：登录日志表数据量大时，考虑分区表或归档策略
5. **索引维护**：定期检查索引使用情况，优化查询性能

---

## 扩展建议

### 未来可添加的表

1. **操作日志表**（operation_log）
   - 记录用户的所有操作（上传、删除、重命名等）
   - 用于审计和安全分析

2. **系统监控表**（system_monitor）
   - 定期记录系统指标（CPU、内存、磁盘使用率）
   - 用于性能监控和告警

3. **存储配额历史表**（storage_quota_history）
   - 记录用户存储空间变更历史
   - 用于追踪配额调整记录

