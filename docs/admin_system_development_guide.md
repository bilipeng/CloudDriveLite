# 管理员系统开发指南

## 概述

本指南详细说明如何实现管理员系统的四个核心功能：
1. **存储空间管理** - 设置用户最大存储空间
2. **系统监控** - 查看系统状态和统计数据
3. **用户管理** - 查看用户列表、启用/禁用用户
4. **登录日志** - 记录和查看用户登录历史

---

## 一、后端开发（Java/Spring Boot）

### 1.1 实体类（Entity）开发

#### 1.1.1 修改 User 实体类
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/entity/User.java`

**需要做的事情**：
- 添加 `role` 字段（String 类型，默认值 "USER"）
- 添加 `maxStorage` 字段（Long 类型，默认值 10737418240，即 10GB）
- 添加对应的 getter/setter 方法
- 添加 `@Column` 注解指定数据库字段名和注释

**技术要点**：
- 使用 JPA 注解 `@Column` 映射数据库字段
- `role` 字段建议使用枚举或常量类定义（USER/ADMIN）
- `maxStorage` 单位统一使用字节（byte）

---

#### 1.1.2 创建 LoginLog 实体类
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/entity/LoginLog.java`

**需要做的事情**：
- 创建新的实体类，对应 `login_log` 表
- 定义字段：id, userId, userNumber, username, loginTime, ipAddress, userAgent, loginStatus, failureReason
- 使用 JPA 注解：`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`
- 添加 `@PrePersist` 自动设置登录时间

**技术要点**：
- `loginStatus` 建议使用枚举（SUCCESS/FAILED）
- `ipAddress` 字段长度设为 45（支持 IPv6）
- `userAgent` 字段长度设为 500（浏览器信息可能较长）

---

#### 1.1.3 创建 SystemConfig 实体类（可选）
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/entity/SystemConfig.java`

**需要做的事情**：
- 创建实体类对应 `system_config` 表
- 定义字段：id, configKey, configValue, configType, description, createdAt, updatedAt
- 使用 `@PrePersist` 和 `@PreUpdate` 自动更新时间戳

**技术要点**：
- `configKey` 需要唯一索引
- `configType` 建议使用枚举（STRING/NUMBER/BOOLEAN/JSON）

---

### 1.2 Repository 层开发

#### 1.2.1 修改 UserRepository
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/repository/UserRepository.java`

**需要做的事情**：
- 添加按角色查询方法：`List<User> findByRole(String role)`
- 添加按状态和角色查询：`Page<User> findByStatusAndRole(Integer status, String role, Pageable pageable)`
- 添加统计方法：`long countByRole(String role)`

**技术要点**：
- 使用 Spring Data JPA 方法命名规范
- 分页查询使用 `Pageable` 参数

---

#### 1.2.2 创建 LoginLogRepository
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/repository/LoginLogRepository.java`

**需要做的事情**：
- 创建接口继承 `JpaRepository<LoginLog, Long>`
- 添加查询方法：
  - 按用户ID查询：`Page<LoginLog> findByUserIdOrderByLoginTimeDesc(Long userId, Pageable pageable)`
  - 按时间范围查询：`List<LoginLog> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end)`
  - 按状态查询：`long countByLoginStatus(String status)`
  - 今日登录统计：`long countByLoginTimeAfterAndLoginStatus(LocalDateTime date, String status)`

**技术要点**：
- 使用 `Pageable` 实现分页
- 时间查询使用 `LocalDateTime`

---

#### 1.2.3 创建 SystemConfigRepository（可选）
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/repository/SystemConfigRepository.java`

**需要做的事情**：
- 创建接口继承 `JpaRepository<SystemConfig, Long>`
- 添加按 key 查询：`Optional<SystemConfig> findByConfigKey(String key)`

---

### 1.3 Service 层开发

#### 1.3.1 修改 UserService
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/service/UserService.java`

**需要做的事情**：
- 添加方法：`void updateUserStorage(Long userId, Long maxStorage)` - 更新用户存储空间
- 添加方法：`void updateUserStatus(Long userId, Integer status)` - 启用/禁用用户
- 添加方法：`void updateUserRole(Long userId, String role)` - 修改用户角色
- 添加方法：`Page<User> listUsers(Integer status, String role, int page, int size)` - 分页查询用户列表
- 添加方法：`Map<String, Object> getUserStorageStats(Long userId)` - 获取用户存储统计

**技术要点**：
- 更新存储空间前需要验证新值是否合理（不能小于已用空间）
- 禁用用户前可以检查是否有未完成的操作
- 存储统计需要关联 `file_object` 表计算已用空间

---

#### 1.3.2 创建 LoginLogService
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/service/LoginLogService.java`

**需要做的事情**：
- 添加方法：`void recordLogin(Long userId, String userNumber, String username, String ipAddress, String userAgent, String status, String failureReason)` - 记录登录日志
- 添加方法：`Page<LoginLog> getLoginLogs(Long userId, int page, int size)` - 分页查询登录日志
- 添加方法：`Map<String, Object> getLoginStatistics(LocalDateTime startDate, LocalDateTime endDate)` - 获取登录统计

**技术要点**：
- 在 `AuthController` 的登录方法中调用 `recordLogin`
- 登录成功和失败都要记录
- 统计方法需要聚合查询（按日期、按状态等）

---

#### 1.3.3 创建 AdminService
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/service/AdminService.java`

**需要做的事情**：
- 添加方法：`Map<String, Object> getSystemOverview()` - 获取系统概览数据
  - 总用户数、活跃用户数、管理员数量
  - 总文件数、总文件夹数
  - 系统总存储使用量
  - 今日登录次数、今日上传文件数
- 添加方法：`List<Map<String, Object>> getUserStorageRanking(int limit)` - 获取用户存储使用排行
- 添加方法：`Map<String, Object> getStorageStatistics()` - 获取存储统计详情
- 添加方法：`void updateSystemConfig(String key, String value)` - 更新系统配置

**技术要点**：
- 系统概览需要聚合多个表的查询
- 存储排行需要按用户分组计算总存储量
- 可以使用 `@Transactional(readOnly = true)` 优化只读查询

---

### 1.4 Controller 层开发

#### 1.4.1 修改 AuthController（记录登录日志）
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/control/AuthController.java`

**需要做的事情**：
- 在 `login` 方法中，登录成功时调用 `LoginLogService.recordLogin` 记录成功日志
- 在 `login` 方法中，登录失败时调用 `LoginLogService.recordLogin` 记录失败日志
- 从 `HttpServletRequest` 获取 IP 地址和 User-Agent
- 注入 `LoginLogService` 依赖

**技术要点**：
- IP 地址获取：`request.getRemoteAddr()` 或从 `X-Forwarded-For` 头获取
- User-Agent 获取：`request.getHeader("User-Agent")`
- 失败原因可以从异常信息中提取

---

#### 1.4.2 创建 AdminController
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/control/AdminController.java`

**需要做的事情**：
- 创建新的 Controller 类，使用 `@RestController` 和 `@RequestMapping("/api/admin")`
- 添加权限检查方法：`requireAdmin(HttpSession session)` - 验证当前用户是否为管理员
- 实现以下接口：

**用户管理接口**：
- `GET /api/admin/users` - 获取用户列表（分页、筛选）
  - 参数：page, size, status, role, keyword（搜索用户名/账号）
  - 返回：用户列表 + 分页信息 + 每个用户的存储使用情况
- `PUT /api/admin/users/{id}/storage` - 更新用户存储空间
  - 参数：maxStorage（请求体）
- `PUT /api/admin/users/{id}/status` - 启用/禁用用户
  - 参数：status（请求体）
- `PUT /api/admin/users/{id}/role` - 修改用户角色
  - 参数：role（请求体）

**系统监控接口**：
- `GET /api/admin/system/overview` - 获取系统概览
  - 返回：用户数、文件数、存储使用、今日统计等
- `GET /api/admin/system/storage/ranking` - 获取存储使用排行
  - 参数：limit（可选，默认10）
  - 返回：TOP N 用户的存储使用情况
- `GET /api/admin/system/storage/statistics` - 获取存储统计详情
  - 返回：总存储、已用存储、各用户存储分布等

**登录日志接口**：
- `GET /api/admin/logs/login` - 获取登录日志列表
  - 参数：page, size, userId（可选），startDate（可选），endDate（可选）
  - 返回：登录日志列表 + 分页信息
- `GET /api/admin/logs/login/statistics` - 获取登录统计
  - 参数：startDate, endDate
  - 返回：按日期统计的登录数据（成功/失败次数、活跃用户数等）

**系统配置接口**（可选）：
- `GET /api/admin/config` - 获取所有系统配置
- `PUT /api/admin/config/{key}` - 更新系统配置
  - 参数：configValue（请求体）

**技术要点**：
- 所有接口都需要先调用 `requireAdmin` 检查权限
- 使用 `@RequestParam` 接收查询参数
- 使用 `@RequestBody` 接收 JSON 请求体
- 返回统一使用 `ApiResponse` 包装
- 分页使用 Spring Data 的 `Page` 对象

---

### 1.5 权限验证

#### 1.5.1 创建权限检查工具类
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/util/AdminUtil.java`（可选）

**需要做的事情**：
- 创建静态方法 `boolean isAdmin(HttpSession session)` - 检查当前用户是否为管理员
- 从 Session 获取用户ID，查询用户角色
- 或者直接在 Controller 中实现 `requireAdmin` 方法

**技术要点**：
- 可以复用现有的 `requireUser` 方法逻辑
- 查询用户角色后判断是否为 "ADMIN"
- 如果不是管理员，抛出异常或返回 403

---

### 1.6 存储空间检查（修改 FileService）

#### 1.6.1 修改 FileService.upload 方法
**文件位置**：`Java/src/main/java/com/peng/clouddrivelite/service/FileService.java`

**需要做的事情**：
- 在上传文件前，检查用户已用存储空间 + 新文件大小是否超过 `maxStorage`
- 如果超过，抛出异常，阻止上传
- 添加方法：`long calculateUserStorage(Long userId)` - 计算用户已用存储空间
  - 查询该用户所有非文件夹的文件，SUM(file_size)

**技术要点**：
- 使用 SQL 聚合函数 `SUM` 计算总存储
- 异常信息要清晰，告知用户已用空间和限制
- 可以在上传前和上传后都检查（防止并发问题）

---

## 二、前端开发（Vue 3 + Element Plus）

### 2.1 路由配置

#### 2.1.1 修改路由文件
**文件位置**：`CloudDriveLite-web/src/router/index.ts`

**需要做的事情**：
- 添加管理员路由（需要权限守卫）
- 路由结构：
  - `/admin` - 管理员入口（重定向到 dashboard）
  - `/admin/dashboard` - 系统概览
  - `/admin/users` - 用户管理
  - `/admin/storage` - 存储管理
  - `/admin/logs` - 登录日志

**技术要点**：
- 使用路由守卫 `beforeEach` 检查用户角色
- 从 localStorage 或 Session 获取用户信息
- 如果不是管理员，重定向到登录页或文件管理页
- 可以使用 `meta: { requiresAdmin: true }` 标记需要管理员权限的路由

---

### 2.2 API 封装

#### 2.2.1 创建管理员 API 文件
**文件位置**：`CloudDriveLite-web/src/api/admin.ts`

**需要做的事情**：
- 封装所有管理员相关的 API 调用
- 使用 `axios` 或 `fetch` 发送请求
- 定义 TypeScript 类型接口

**需要封装的 API**：
- `getSystemOverview()` - 获取系统概览
- `getUserList(params)` - 获取用户列表
- `updateUserStorage(userId, maxStorage)` - 更新用户存储空间
- `updateUserStatus(userId, status)` - 启用/禁用用户
- `updateUserRole(userId, role)` - 修改用户角色
- `getStorageRanking(limit)` - 获取存储排行
- `getStorageStatistics()` - 获取存储统计
- `getLoginLogs(params)` - 获取登录日志
- `getLoginStatistics(startDate, endDate)` - 获取登录统计

**技术要点**：
- 所有请求都需要携带 credentials（Cookie）
- 使用 TypeScript 定义返回数据类型
- 统一错误处理

---

### 2.3 页面组件开发

#### 2.3.1 管理员布局组件
**文件位置**：`CloudDriveLite-web/src/layout/AdminLayout.vue`

**需要做的事情**：
- 创建管理员专用布局
- 包含：顶部导航栏、侧边栏菜单、主内容区
- 侧边栏菜单项：系统概览、用户管理、存储管理、登录日志
- 显示当前登录的管理员信息
- 提供退出登录功能

**技术要点**：
- 使用 Element Plus 的 `el-container`, `el-aside`, `el-main`
- 使用 `el-menu` 实现侧边栏导航
- 响应式设计（移动端可折叠侧边栏）

---

#### 2.3.2 系统概览页面
**文件位置**：`CloudDriveLite-web/src/views/admin/Dashboard.vue`

**需要做的事情**：
- 显示系统概览数据（使用卡片布局）
- 数据卡片：
  - 总用户数、活跃用户数、管理员数量
  - 总文件数、总文件夹数
  - 系统总存储使用量（GB）
  - 今日登录次数、今日上传文件数
- 显示存储使用趋势图（可选，使用 ECharts）
- 显示最近登录日志（表格，最近10条）

**技术要点**：
- 使用 Element Plus 的 `el-card`, `el-statistic` 组件
- 使用 `el-row`, `el-col` 实现响应式布局
- 数据刷新：页面加载时获取，可添加刷新按钮
- 数字格式化：存储大小转换为 GB/MB，添加千分位

---

#### 2.3.3 用户管理页面
**文件位置**：`CloudDriveLite-web/src/views/admin/UserManagement.vue`

**需要做的事情**：
- 用户列表表格（Element Plus Table）
  - 列：ID、用户名、用户号、邮箱、角色、状态、已用存储/最大存储、注册时间、操作
- 搜索功能：按用户名、用户号搜索
- 筛选功能：按角色（全部/用户/管理员）、按状态（全部/正常/禁用）筛选
- 分页功能
- 操作按钮：
  - 查看详情（可选）
  - 修改存储空间（弹窗编辑）
  - 启用/禁用用户
  - 修改角色（普通用户/管理员）

**技术要点**：
- 使用 `el-table` 显示列表
- 使用 `el-dialog` 实现编辑弹窗
- 使用 `el-select` 实现筛选
- 使用 `el-pagination` 实现分页
- 存储大小显示：已用/最大，百分比进度条
- 状态显示：使用 `el-tag` 显示正常/禁用状态

---

#### 2.3.4 存储管理页面
**文件位置**：`CloudDriveLite-web/src/views/admin/StorageManagement.vue`

**需要做的事情**：
- 系统存储总览卡片
  - 总存储空间、已用存储、剩余存储
  - 使用率百分比、进度条
- 用户存储使用排行表格
  - 列：排名、用户名、用户号、已用存储、最大存储、使用率、操作
  - 支持按使用率排序
  - 支持设置用户存储上限（弹窗编辑）
- 存储统计图表（可选）
  - 使用 ECharts 显示存储分布饼图
  - 显示各用户存储占比

**技术要点**：
- 存储大小单位转换（字节 → GB/MB）
- 使用 `el-progress` 显示使用率
- 使用 `el-table` 的排序功能
- 弹窗编辑存储上限时，需要验证输入（不能小于已用空间）

---

#### 2.3.5 登录日志页面
**文件位置**：`CloudDriveLite-web/src/views/admin/LoginLogs.vue`

**需要做的事情**：
- 登录日志表格
  - 列：时间、用户名、用户号、IP地址、浏览器、状态、失败原因
- 筛选功能：
  - 按用户筛选（下拉选择或搜索）
  - 按时间范围筛选（日期选择器）
  - 按登录状态筛选（成功/失败）
- 分页功能
- 导出功能（可选）：导出为 CSV/Excel

**技术要点**：
- 使用 `el-date-picker` 实现日期范围选择
- 使用 `el-select` 实现用户和状态筛选
- 状态显示：成功用绿色标签，失败用红色标签
- IP 地址和浏览器信息可以显示完整（使用 tooltip）
- 时间格式化：显示相对时间（如"2小时前"）或绝对时间

---

### 2.4 权限守卫

#### 2.4.1 创建管理员权限守卫
**文件位置**：`CloudDriveLite-web/src/router/guards.ts`（新建）或修改 `router/index.ts`

**需要做的事情**：
- 创建路由守卫函数 `adminGuard`
- 检查用户是否登录
- 检查用户角色是否为管理员
- 如果不是管理员，重定向到登录页或文件管理页
- 显示提示信息（"需要管理员权限"）

**技术要点**：
- 从 localStorage 或 API 获取用户信息
- 可以在登录时保存用户角色到 localStorage
- 或者每次访问管理员页面时调用 API 验证权限

---

### 2.5 用户信息存储

#### 2.5.1 修改登录逻辑
**文件位置**：`CloudDriveLite-web/src/views/Login.vue` 和 `src/api/auth.ts`

**需要做的事情**：
- 登录成功后，保存用户角色到 localStorage
- 修改登录 API 返回，包含用户角色信息
- 或者在登录后调用用户信息接口获取角色

**技术要点**：
- localStorage 存储：`localStorage.setItem('userRole', role)`
- 退出登录时清除：`localStorage.removeItem('userRole')`

---

## 三、开发顺序建议

### 第一阶段：基础功能（1-2天）
1. **后端**：
   - 修改 User 实体类，添加 role 和 maxStorage 字段
   - 创建 LoginLog 实体类和 Repository
   - 修改 AuthController，记录登录日志
   - 创建 AdminController 基础框架和权限检查

2. **前端**：
   - 创建管理员路由和权限守卫
   - 创建 AdminLayout 布局组件
   - 创建 API 封装文件

### 第二阶段：用户管理（2-3天）
3. **后端**：
   - 实现 AdminController 的用户管理接口
   - 实现 UserService 的用户管理方法

4. **前端**：
   - 创建用户管理页面
   - 实现用户列表、搜索、筛选、分页
   - 实现修改存储空间、启用/禁用、修改角色功能

### 第三阶段：系统监控（2-3天）
5. **后端**：
   - 实现 AdminService 的系统概览和统计方法
   - 实现存储排行和统计接口

6. **前端**：
   - 创建系统概览页面
   - 创建存储管理页面
   - 实现数据展示和图表（可选）

### 第四阶段：登录日志（1-2天）
7. **后端**：
   - 完善 LoginLogService
   - 实现登录日志查询和统计接口

8. **前端**：
   - 创建登录日志页面
   - 实现筛选、分页、导出功能

### 第五阶段：存储空间限制（1天）
9. **后端**：
   - 修改 FileService，添加上传前存储空间检查

10. **前端**：
    - 在上传组件中显示用户存储使用情况
    - 存储空间不足时提示用户

---

## 四、技术要点总结

### 4.1 后端技术要点
- **权限验证**：所有管理员接口都需要先检查用户角色
- **数据聚合**：系统统计需要多表关联查询，使用 SQL 聚合函数
- **存储计算**：用户已用存储 = SUM(file_size) WHERE user_id = ? AND is_folder = 0
- **分页查询**：使用 Spring Data JPA 的 `Pageable` 和 `Page`
- **异常处理**：存储空间超限、权限不足等场景需要明确的异常信息

### 4.2 前端技术要点
- **权限守卫**：路由级别和组件级别的权限检查
- **数据格式化**：存储大小、时间、百分比等需要格式化显示
- **响应式设计**：管理员页面需要适配不同屏幕尺寸
- **用户体验**：加载状态、错误提示、操作确认（删除、禁用等）
- **数据刷新**：列表数据支持手动刷新和自动刷新（可选）

### 4.3 数据库查询优化
- **索引使用**：login_log 表的时间、用户ID、状态字段已建立索引
- **聚合查询**：存储统计使用 GROUP BY 和 SUM，注意性能
- **分页查询**：大数据量时使用 LIMIT 和 OFFSET
- **定期清理**：登录日志表建议定期清理旧数据（如保留6个月）

---

## 五、测试建议

### 5.1 功能测试
- 管理员登录后能访问所有管理员页面
- 普通用户无法访问管理员页面（重定向）
- 用户管理：列表、搜索、筛选、分页、编辑、启用/禁用
- 存储管理：查看统计、设置用户存储上限
- 登录日志：查看、筛选、分页
- 存储空间限制：上传文件时检查是否超限

### 5.2 边界测试
- 存储空间设置为 0 或负数
- 存储空间设置为小于已用空间
- 大量用户和日志数据下的性能测试
- 并发上传时的存储空间检查

### 5.3 安全测试
- 普通用户尝试直接访问管理员 API
- 普通用户尝试修改其他用户的存储空间
- SQL 注入测试（参数化查询）
- XSS 测试（用户输入过滤）

---

## 六、后续扩展建议

### 6.1 功能扩展
- 操作日志：记录管理员的所有操作
- 文件审核：管理员审核用户上传的文件
- 批量操作：批量设置用户存储空间、批量启用/禁用
- 数据导出：导出用户列表、登录日志为 Excel

### 6.2 性能优化
- 系统统计数据缓存（Redis）
- 登录日志表分区（按时间）
- 存储统计异步计算

### 6.3 用户体验
- 实时通知：存储空间不足提醒
- 数据可视化：更多图表展示（ECharts）
- 移动端适配：响应式设计优化

---

## 七、开发注意事项

1. **数据安全**：管理员操作涉及敏感数据，需要记录操作日志
2. **性能考虑**：系统统计查询可能较慢，考虑添加缓存
3. **用户体验**：操作确认（删除、禁用等）使用确认对话框
4. **错误处理**：所有 API 调用都需要错误处理，显示友好提示
5. **代码规范**：遵循项目现有的代码风格和命名规范
6. **测试覆盖**：关键功能需要单元测试和集成测试

---

## 八、参考资料

- Spring Data JPA 文档：https://spring.io/projects/spring-data-jpa
- Element Plus 文档：https://element-plus.org/
- Vue Router 文档：https://router.vuejs.org/
- ECharts 文档（如使用图表）：https://echarts.apache.org/

---

**开发完成后，记得更新 README.md，添加管理员系统的使用说明！**


