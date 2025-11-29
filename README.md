# CloudDriveLite - 轻量级个人网盘系统

一个基于前后端分离架构的轻量级个人网盘/文件管理系统，支持文件的上传、下载、管理、在线预览等功能。**核心特色是支持大文件分片上传和断点续传**，让用户能够稳定可靠地上传任意大小的文件。

---

## 📋 目录

- [系统概述](#系统概述)
- [技术栈](#技术栈)
- [核心功能](#核心功能)
- [核心实现：文件上传系统](#核心实现文件上传系统)
  - [1. 普通文件上传](#1-普通文件上传)
  - [2. 大文件分片上传](#2-大文件分片上传)
  - [3. 断点续传机制](#3-断点续传机制)
- [系统架构设计](#系统架构设计)
- [快速开始](#快速开始)
- [API 接口文档](#api-接口文档)
- [项目结构](#项目结构)

---

## 系统概述

CloudDriveLite 是一个完整的文件管理系统，采用 **前后端分离** 架构设计：

- **后端**：Spring Boot + MySQL，提供 RESTful API
- **前端**：Vue 3 + TypeScript + Element Plus，提供现代化用户界面
- **核心能力**：文件存储、目录管理、在线预览、用户认证、存储空间管理

### 主要特性

✅ **双模式文件上传**：普通上传（小文件） + 分片上传（大文件）  
✅ **断点续传**：网络中断后可从断点继续上传  
✅ **存储空间管理**：用户存储空间统计和限制  
✅ **在线预览**：支持图片、文本等多种格式在线预览  
✅ **目录管理**：完整的文件夹创建、移动、删除功能  
✅ **用户系统**：注册、登录、找回密码、个人中心  
✅ **管理员系统**：用户管理、存储统计、登录日志

---

## 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.5.5 | Web 框架，提供 RESTful API |
| **Spring Data JPA** | - | 数据库 ORM，简化数据访问 |
| **MySQL** | 8.0+ | 关系型数据库，存储文件元数据和用户信息 |
| **Maven** | 3.8+ | 项目构建和依赖管理 |
| **Java** | 17+ | 编程语言 |

**关键技术点**：
- `MultipartFile`：Spring 提供的文件上传接口
- `HttpSession`：基于 Session 的用户认证
- `@Transactional`：事务管理，确保数据一致性

### 前端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.x | 前端框架，使用 Composition API |
| **TypeScript** | 5.x | 类型安全的 JavaScript |
| **Element Plus** | 2.11 | UI 组件库 |
| **Vite** | 7.x | 前端构建工具，提供快速开发体验 |
| **Vue Router** | 4.x | 前端路由管理 |
| **Pinia** | - | 状态管理 |
| **Axios** | - | HTTP 请求库 |
| **simple-uploader.js** | - | 大文件分片上传库 |

**关键技术点**：
- `simple-uploader.js`：实现分片上传和断点续传的核心库
- `FormData`：用于文件上传的数据格式
- `File API`：浏览器文件操作 API

---

## 核心功能

### 1. 用户认证系统

- **用户注册**：支持用户名、用户号、手机号、邮箱注册
- **用户登录**：基于 Session 的登录态管理
- **找回密码**：通过手机号或邮箱验证重置密码
- **个人中心**：查看个人信息、存储空间、修改密码

### 2. 文件管理系统

- **文件列表**：分页展示、按目录浏览、文件类型图标识别
- **目录操作**：创建文件夹、进入目录、返回上级、面包屑导航
- **文件操作**：上传、下载、重命名、移动、删除、预览
- **批量操作**：批量选择删除
- **搜索功能**：按文件名模糊搜索

### 3. 存储空间管理

- **空间统计**：实时显示已用空间和剩余空间
- **空间限制**：每个用户有独立的存储空间上限
- **空间检查**：上传前检查剩余空间是否足够

### 4. 管理员系统

- **用户管理**：查看用户列表、修改用户信息、设置存储空间
- **存储统计**：查看所有用户的存储使用情况
- **登录日志**：记录用户登录历史
- **系统概览**：系统整体数据统计

---

## 核心实现：文件上传系统

这是整个系统最核心的部分。系统实现了**两种上传方式**，以适应不同场景的需求。

### 1. 普通文件上传

**适用场景**：小文件（通常 < 100MB）

#### 实现原理

普通上传采用**一次性上传**的方式，整个文件作为一个 HTTP 请求发送到服务器。

**前端实现**（`Upfilebutton.vue`）：

```typescript
// 使用 Element Plus 的 el-upload 组件
<el-upload
  action="/api/files/upload"           // 上传接口地址
  :data="uploadData"                   // 额外参数（如 folderId）
  :with-credentials="true"             // 携带 Cookie（用于 Session 认证）
  @on-success="handleSuccess"           // 上传成功回调
>
  <el-button>上传文件</el-button>
</el-upload>
```

**工作流程**：

1. **用户选择文件** → 触发 `el-upload` 的文件选择
2. **自动上传** → `el-upload` 自动将文件封装为 `FormData`，发送 POST 请求到 `/api/files/upload`
3. **后端接收** → Spring Boot 的 `FileController` 接收 `MultipartFile`
4. **文件验证** → 检查文件大小、类型、存储空间
5. **保存文件** → 生成唯一文件名，保存到磁盘
6. **数据库记录** → 在 MySQL 中创建文件记录
7. **返回结果** → 返回文件信息给前端

**后端实现**（`FileService.upload()`）：

```java
@Transactional
public FileObject upload(Long userId, MultipartFile file, Long folderId) {
    // 1. 检查存储空间
    long usedStorage = calculateUserStorage(userId);
    if (usedStorage + file.getSize() > user.getMaxStorage()) {
        throw new RuntimeException("存储空间不足");
    }
    
    // 2. 生成唯一存储文件名（UUID）
    String stored = UUID.randomUUID().toString() + "." + ext;
    
    // 3. 创建用户专属目录：user_{userId}/yyyy/MM/dd/
    Path dir = Paths.get(baseDir, "user_" + userId, year, month, day);
    Files.createDirectories(dir);
    
    // 4. 保存文件到磁盘
    file.transferTo(path.toFile());
    
    // 5. 创建数据库记录
    FileObject fo = new FileObject();
    fo.setUserId(userId);
    fo.setFileName(originalFilename);
    fo.setFilePath(path.toString());
    // ... 设置其他字段
    
    return fileRepository.save(fo);
}
```

**优点**：
- ✅ 实现简单，代码量少
- ✅ 适合小文件，上传速度快
- ✅ 无需额外的前端库

**缺点**：
- ❌ 大文件容易超时
- ❌ 网络中断需要重新上传
- ❌ 无法显示详细的上传进度

---

### 2. 大文件分片上传

**适用场景**：大文件（> 100MB 或需要断点续传的文件）

#### 实现原理

分片上传将大文件**切分成多个小块**（chunk），逐个上传，最后在服务器端合并。

**核心思想**：
```
一个大文件（比如 1GB）
  ↓ 切分
[块1: 2MB] [块2: 2MB] [块3: 2MB] ... [块500: 2MB]
  ↓ 逐个上传
服务器接收 → 临时存储 → 检查完整性 → 合并 → 保存
```

#### 前端实现（`ChunkUploader.vue`）

使用 `simple-uploader.js` 库实现分片上传：

```javascript
import Uploader from 'simple-uploader.js'

// 创建上传器实例
const uploader = new Uploader({
  target: '/api/files/upload',        // 上传接口
  chunkSize: 2 * 1024 * 1024,         // 每个分片 2MB
  simultaneousUploads: 3,              // 同时上传 3 个分片
  testChunks: true,                    // 启用断点续传检测
  query: () => ({
    folderId: props.folderId || 0     // 动态获取当前目录
  })
})

// 监听上传进度
uploader.on('fileProgress', (rootFile, file, chunk) => {
  // 更新进度条
  file.progress = file.progress()
})

// 监听上传成功
uploader.on('fileSuccess', (rootFile, file, response) => {
  // 文件上传完成
})
```

**工作流程**：

1. **文件选择** → 用户选择文件，`simple-uploader.js` 自动计算需要多少个分片
2. **分片检测** → 对每个分片，先发送 GET 请求检查是否已上传（断点续传）
3. **分片上传** → 逐个上传未完成的分片（可并发上传多个）
4. **进度更新** → 实时更新上传进度
5. **合并请求** → 所有分片上传完成后，服务器自动合并
6. **完成通知** → 返回文件信息

**关键代码**：

```javascript
// 文件添加到上传队列
uploader.addFile(file)

// 开始上传
uploader.upload()

// 暂停上传
uploader.pause()

// 继续上传
uploader.resume()
```

#### 后端实现

后端需要处理两个接口：

**① 分片检测接口**（`GET /api/files/upload`）：

```java
@GetMapping("/upload")
public ResponseEntity<Void> checkChunk(
    @RequestParam String identifier,      // 文件唯一标识
    @RequestParam Integer chunkNumber,     // 分片编号
    HttpSession session) {
    
    Long userId = requireUser(session);
    
    // 检查分片文件是否存在
    boolean exists = fileService.chunkExists(userId, identifier, chunkNumber);
    
    // 存在返回 200，不存在返回 204
    return exists ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
}
```

**② 分片上传接口**（`POST /api/files/upload`）：

```java
@PostMapping("/upload")
public ApiResponse<FileInfoDto> upload(
    @RequestParam("file") MultipartFile file,           // 分片文件
    @RequestParam String identifier,                     // 文件唯一标识
    @RequestParam Integer chunkNumber,                   // 当前分片编号
    @RequestParam Integer totalChunks,                   // 总分片数
    @RequestParam String filename,                       // 原始文件名
    @RequestParam Long totalSize,                        // 文件总大小
    @RequestParam(defaultValue = "0") Long folderId,     // 目标目录
    HttpSession session) {
    
    Long userId = requireUser(session);
    
    // 1. 保存分片到临时目录
    fileService.saveChunk(userId, identifier, chunkNumber, file);
    
    // 2. 尝试合并（检查是否所有分片都已上传）
    Optional<FileObject> merged = fileService.tryMergeChunks(
        userId, identifier, totalChunks, filename, totalSize, folderId
    );
    
    // 3. 如果合并成功，返回文件信息；否则返回分片上传成功
    if (merged.isPresent()) {
        return ApiResponse.success("文件上传成功", convertToFileInfoDto(merged.get()));
    } else {
        return ApiResponse.success("分片上传成功", null);
    }
}
```

**分片存储逻辑**（`FileService.saveChunk()`）：

```java
public void saveChunk(Long userId, String identifier, int chunkNumber, MultipartFile file) {
    // 创建分片存储目录：storage/chunks/user_{userId}/{identifier}/
    Path dir = Paths.get(baseDir, "chunks", "user_" + userId, identifier);
    Files.createDirectories(dir);
    
    // 保存分片文件，文件名就是分片编号
    Path chunkPath = dir.resolve(String.valueOf(chunkNumber));
    file.transferTo(chunkPath.toFile());
}
```

**分片合并逻辑**（`FileService.tryMergeChunks()`）：

```java
public Optional<FileObject> tryMergeChunks(...) {
    Path dir = Paths.get(baseDir, "chunks", "user_" + userId, identifier);
    
    // 1. 检查所有分片是否都存在
    for (int i = 1; i <= totalChunks; i++) {
        if (!Files.exists(dir.resolve(String.valueOf(i)))) {
            return Optional.empty();  // 还有分片未上传，不合并
        }
    }
    
    // 2. 检查存储空间
    if (usedStorage + totalSize > user.getMaxStorage()) {
        throw new RuntimeException("存储空间不足");
    }
    
    // 3. 创建目标文件路径
    Path target = Paths.get(baseDir, "user_" + userId, year, month, day, storedFileName);
    
    // 4. 按顺序合并所有分片
    try (OutputStream out = Files.newOutputStream(target)) {
        for (int i = 1; i <= totalChunks; i++) {
            Path chunkPath = dir.resolve(String.valueOf(i));
            try (InputStream in = Files.newInputStream(chunkPath)) {
                in.transferTo(out);  // 将分片内容写入目标文件
            }
        }
    }
    
    // 5. 清理临时分片文件
    for (int i = 1; i <= totalChunks; i++) {
        Files.deleteIfExists(dir.resolve(String.valueOf(i)));
    }
    Files.deleteIfExists(dir);
    
    // 6. 创建数据库记录
    FileObject fo = new FileObject();
    // ... 设置文件信息
    return Optional.of(fileRepository.save(fo));
}
```

**优点**：
- ✅ 支持大文件上传（不受单次请求大小限制）
- ✅ 可以断点续传
- ✅ 可以显示详细的上传进度
- ✅ 可以暂停/继续上传
- ✅ 网络中断后可以继续

**缺点**：
- ❌ 实现相对复杂
- ❌ 需要额外的前端库
- ❌ 服务器需要临时存储分片

---

### 3. 断点续传机制

断点续传是分片上传的一个重要特性，让用户在网络中断后可以**从上次中断的地方继续上传**，而不需要重新上传整个文件。

#### 实现原理

**核心思路**：在上传每个分片之前，先检查这个分片是否已经上传过。

**工作流程**：

1. **文件标识**：每个文件有一个唯一标识 `identifier`（通常由文件名+大小+用户ID生成）
2. **分片检测**：上传前，对每个分片发送 GET 请求检查是否已存在
   ```
   GET /api/files/upload?identifier=xxx&chunkNumber=1
   → 200 OK：分片已存在，跳过
   → 204 No Content：分片不存在，需要上传
   ```
3. **跳过已上传**：如果分片已存在，直接跳过，不重复上传
4. **继续上传**：只上传缺失的分片

**前端实现**：

```javascript
const uploader = new Uploader({
  testChunks: true,  // 启用分片检测
  // ...
})

// simple-uploader.js 会自动在上传前检测每个分片
// 如果分片已存在，会跳过该分片
```

**后端实现**：

```java
// 检测分片是否存在
public boolean chunkExists(Long userId, String identifier, int chunkNumber) {
    Path chunkPath = Paths.get(
        baseDir, "chunks", "user_" + userId, identifier, String.valueOf(chunkNumber)
    );
    return Files.exists(chunkPath);
}
```

**实际场景**：

```
用户上传 1GB 文件，分成 500 个分片
  ↓
上传到第 200 个分片时，网络中断
  ↓
用户重新打开页面，选择同一个文件
  ↓
系统检测：分片 1-200 已存在，分片 201-500 不存在
  ↓
只上传分片 201-500，节省时间和流量
```

---

## 系统架构设计

### 整体架构

```
┌─────────────────┐
│   用户浏览器     │
│  (Vue 3 前端)    │
└────────┬────────┘
         │ HTTP 请求
         ↓
┌─────────────────┐
│  Vite 开发服务器 │  (代理 /api → 后端)
│  localhost:5173  │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Spring Boot     │
│  localhost:8080  │
└────────┬────────┘
         │
    ┌────┴────┐
    ↓         ↓
┌────────┐ ┌──────────┐
│  MySQL │ │  文件系统 │
│  数据库  │ │  (磁盘)  │
└────────┘ └──────────┘
```

### 数据流向

**文件上传流程**：

```
前端选择文件
  ↓
[普通上传] → 直接 POST /api/files/upload
[分片上传] → 检测分片 → 上传分片 → 服务器合并
  ↓
后端接收文件
  ↓
验证（大小、类型、空间）
  ↓
保存到磁盘：storage/user_{userId}/yyyy/MM/dd/{uuid}.{ext}
  ↓
保存到数据库：file_object 表
  ↓
返回文件信息给前端
```

**文件下载流程**：

```
前端请求下载
  ↓
GET /api/files/{id}/download
  ↓
后端验证权限
  ↓
从数据库读取文件路径
  ↓
从磁盘读取文件
  ↓
返回文件流给前端
```

### 数据库设计

**核心表结构**：

```sql
-- 用户表
user (
  id, user_number, username, password, 
  phone_number, email, role, max_storage, ...
)

-- 文件表（核心）
file_object (
  id, user_id, file_name,           -- 文件名
  stored_file_name, file_path,     -- 存储路径
  parent_id,                       -- 父目录ID（0=根目录）
  file_type, file_size,            -- 文件类型和大小
  is_folder, uploaded_time, ...    -- 是否文件夹、上传时间
)

-- 登录日志表
login_log (
  id, user_id, login_time, ip_address, user_agent, ...
)
```

**目录结构设计**：

使用 `parent_id` 字段实现树形结构：

```
根目录 (parent_id = 0)
  ├── 文件夹A (parent_id = 0)
  │   ├── 文件1 (parent_id = A的ID)
  │   └── 文件夹B (parent_id = A的ID)
  │       └── 文件2 (parent_id = B的ID)
  └── 文件3 (parent_id = 0)
```

---

## 快速开始

### 环境要求

- **JDK 17+**
- **Maven 3.8+**
- **Node.js 20.19+ 或 22.12+**
- **MySQL 8.0+**

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/bilipeng/CloudDriveLite.git
cd CloudDriveLite
```

#### 2. 数据库配置

创建数据库：

```sql
CREATE DATABASE CloudDriveLite CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `Java/src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/CloudDriveLite?serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=你的密码
```

#### 3. 启动后端

```bash
cd Java
# Windows
mvnw.cmd spring-boot:run
# Linux/Mac
./mvnw spring-boot:run
```

后端启动在 `http://localhost:8080`

#### 4. 启动前端

```bash
cd CloudDriveLite-web
npm install
npm run dev
```

前端启动在 `http://localhost:5173`

#### 5. 访问系统

1. 访问 `http://localhost:5173/register` 注册账号
2. 登录后即可使用文件管理功能

---

## API 接口文档

### 用户认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/logout` | 用户登出 |
| POST | `/api/auth/forgot-password` | 找回密码 |

### 文件管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/files?folderId=0&page=1&size=20` | 获取文件列表 |
| POST | `/api/files/upload` | 文件上传（普通/分片） |
| GET | `/api/files/upload` | 分片检测（断点续传） |
| POST | `/api/files/folder` | 创建文件夹 |
| PUT | `/api/files/{id}/rename` | 重命名 |
| PUT | `/api/files/{id}/move` | 移动文件 |
| DELETE | `/api/files/{id}?recursive=true` | 删除文件 |
| GET | `/api/files/{id}/download` | 下载文件 |
| GET | `/api/files/{id}/preview` | 预览文件 |
| GET | `/api/files/breadcrumb?folderId=0` | 获取面包屑路径 |

### 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/info` | 获取用户信息 |
| PUT | `/api/user/info` | 更新用户信息 |
| PUT | `/api/user/password` | 修改密码 |
| GET | `/api/user/storage` | 获取存储空间信息 |

### 管理员接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/activate` | 激活管理员 Session |
| GET | `/api/admin/dashboard` | 系统概览 |
| GET | `/api/admin/users` | 用户列表 |
| GET | `/api/admin/storage/stats` | 存储统计 |
| GET | `/api/admin/logs` | 登录日志 |

---

## 项目结构

```
CloudDriveLite/
├── Java/                                    # 后端 Spring Boot 工程
│   ├── src/main/java/com/peng/clouddrivelite/
│   │   ├── control/                         # 控制器层（API 接口）
│   │   │   ├── AuthController.java          # 用户认证
│   │   │   ├── FileController.java          # 文件管理（核心）
│   │   │   ├── UserController.java          # 用户信息
│   │   │   └── AdminController.java         # 管理员功能
│   │   ├── service/                         # 业务逻辑层
│   │   │   ├── FileService.java             # 文件服务（核心：上传、下载、合并）
│   │   │   ├── UserService.java             # 用户服务
│   │   │   └── AdminService.java            # 管理员服务
│   │   ├── repository/                      # 数据访问层（JPA）
│   │   │   ├── FileRepository.java
│   │   │   ├── UserRepository.java
│   │   │   └── LoginLogRepository.java
│   │   ├── entity/                          # 实体类（数据库表映射）
│   │   │   ├── FileObject.java              # 文件实体
│   │   │   ├── User.java                    # 用户实体
│   │   │   └── LoginLog.java                # 登录日志实体
│   │   ├── dto/                             # 数据传输对象
│   │   │   ├── ApiResponse.java             # 统一响应格式
│   │   │   └── FileInfoDto.java             # 文件信息 DTO
│   │   └── util/                            # 工具类
│   │       ├── PasswordUtil.java            # 密码加密（SHA-256）
│   │       └── SessionKeys.java             # Session 键常量
│   └── src/main/resources/
│       └── application.properties           # 配置文件
│
├── CloudDriveLite-web/                      # 前端 Vue 3 工程
│   ├── src/
│   │   ├── views/                           # 页面组件
│   │   │   ├── Login.vue                    # 登录页
│   │   │   ├── Register.vue                 # 注册页
│   │   │   ├── Profile.vue                  # 个人中心
│   │   │   ├── ForgotPassword.vue           # 找回密码
│   │   │   └── admin/                       # 管理员页面
│   │   │       ├── Dashboard.vue
│   │   │       ├── UserManagement.vue
│   │   │       └── StorageManagement.vue
│   │   ├── components/                     # 公共组件
│   │   │   ├── ChunkUploader.vue            # 大文件分片上传（核心）
│   │   │   ├── Upfilebutton.vue             # 普通文件上传
│   │   │   ├── FloatNav.vue                 # 顶部导航
│   │   │   └── ...
│   │   ├── api/                             # API 封装
│   │   │   ├── auth.ts                      # 认证 API
│   │   │   ├── files.ts                     # 文件 API
│   │   │   ├── user.ts                      # 用户 API
│   │   │   ├── admin.ts                     # 管理员 API
│   │   │   └── request.ts                   # Axios 封装
│   │   ├── router/                          # 路由配置
│   │   │   └── index.ts
│   │   ├── stores/                          # Pinia 状态管理
│   │   │   └── fileSystem.ts
│   │   └── layout/                          # 布局组件
│   │       ├── NavbarLayout.vue
│   │       └── AdminLayout.vue
│   └── vite.config.ts                       # Vite 配置
│
└── README.md                                 # 项目文档
```

---

## 总结

CloudDriveLite 是一个功能完整的文件管理系统，**核心亮点是实现了双模式文件上传**：

1. **普通上传**：适合小文件，实现简单，速度快
2. **分片上传**：适合大文件，支持断点续传，用户体验好

通过合理的技术选型和架构设计，系统既保证了功能的完整性，又确保了代码的可维护性和扩展性。

---

## 项目信息

- **GitHub 仓库**：https://github.com/bilipeng/CloudDriveLite
- **开发状态**：持续开发中
- **许可证**：未显式声明

**贡献者**：欢迎提交 Issue 和 Pull Request！

---

## 更新日志

- ✅ 支持大文件分片上传和断点续传
- ✅ 完善文件管理功能（批量删除、面包屑导航）
- ✅ 优化用户体验（拖拽上传、进度显示）
- ✅ 实现用户个人中心和管理员系统
- ✅ 添加找回密码功能
- ✅ 实现存储空间管理和统计
