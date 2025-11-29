## CloudDriveLite

一个轻量级的个人网盘/文件管理系统，采用前后端分离架构，支持文件的上传、下载、移动、重命名、删除、路径导航以及在线预览。特别支持大文件分片上传和断点续传功能。

### 功能特性

#### 用户认证
- **用户注册**：支持用户名、用户号、手机号、邮箱注册
- **用户登录**：基于 Session 的登录态管理
- **会话管理**：自动会话超时、安全登出

#### 文件管理
- **文件列表**：分页展示、按目录浏览、文件类型图标识别
- **目录操作**：
  - 创建文件夹
  - 双击进入文件夹
  - 返回上一级目录
  - 面包屑导航（支持点击跳转）
- **文件操作**：
  - 普通文件上传（支持拖拽）
  - **大文件分片上传**（支持断点续传、进度显示、多文件队列）
  - 重命名、移动、删除（支持递归删除文件夹）
  - 批量选择删除
  - 文件下载
  - 在线预览（图片、文本等）
- **文件信息**：自动记录文件大小、类型、上传时间

#### 在线预览
- 后端直出预览接口，按 `Content-Type` 返回资源
- 文本内容自动 UTF-8 编码
- 前端一键"预览"按钮，新窗口打开
- 支持图片、文本文件等常见格式预览

#### 可选中间件
- 支持以 RawBox 作为静态文件托管/预览中转（可选，默认不需要）

### 技术栈

#### 后端
- **框架**：Spring Boot 3.5.5
- **数据库**：MySQL + Spring Data JPA
- **认证**：Session 会话管理
- **文件处理**：Spring MultipartFile、文件类型自动识别
- **构建工具**：Maven

#### 前端
- **框架**：Vue 3 + TypeScript
- **构建工具**：Vite 7
- **UI 组件库**：Element Plus 2.11
- **路由**：Vue Router 4
- **状态管理**：Pinia
- **HTTP 客户端**：Axios
- **大文件上传**：simple-uploader.js（分片上传、断点续传）

#### 可选预览中间件
- RawBox（Go 实现的轻量文件托管服务，参考 [`dongshull/rawbox`](https://github.com/dongshull/rawbox)）

### 目录结构

```
CloudDriveLite/
├── Java/                          # 后端 Spring Boot 工程
│   ├── src/main/java/com/peng/clouddrivelite/
│   │   ├── control/              # 控制器层
│   │   │   ├── AuthController.java      # 用户认证（登录、注册、登出）
│   │   │   ├── FileController.java      # 文件 CRUD、预览、上传接口
│   │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── service/              # 业务逻辑层
│   │   │   ├── FileService.java         # 文件存储、类型识别、递归删除
│   │   │   └── UserService.java         # 用户管理、密码加密
│   │   ├── repository/           # 数据访问层
│   │   │   ├── FileRepository.java
│   │   │   └── UserRepository.java
│   │   ├── entity/               # 实体类
│   │   │   ├── FileObject.java          # 文件实体（支持 parentId 目录结构）
│   │   │   └── User.java
│   │   ├── dto/                  # 数据传输对象
│   │   │   ├── ApiResponse.java
│   │   │   └── FileInfoDto.java
│   │   ├── config/              # 配置类
│   │   │   └── CorsConfig.java         # 跨域配置
│   │   └── util/                # 工具类
│   │       ├── PasswordUtil.java
│   │       └── SessionKeys.java
│   └── src/main/resources/
│       └── application.properties       # 后端配置（数据库、文件上传限制等）
│
├── CloudDriveLite-web/           # 前端工程（Vite + Vue 3）
│   ├── src/
│   │   ├── views/                # 页面组件
│   │   │   ├── Login.vue                # 登录页
│   │   │   ├── Register.vue             # 注册页
│   │   │   └── FileManager.vue          # 文件管理主界面
│   │   ├── components/          # 公共组件
│   │   │   ├── ChunkUploader.vue        # 大文件分片上传组件
│   │   │   ├── Upfilebutton.vue         # 普通文件上传按钮
│   │   │   ├── FloatNav.vue             # 顶部导航栏
│   │   │   ├── Back.vue                 # 返回上一级按钮
│   │   │   └── Breadcrumb.vue           # 面包屑导航
│   │   ├── api/                  # API 封装
│   │   │   ├── auth.ts                  # 认证相关 API
│   │   │   ├── files.ts                 # 文件相关 API
│   │   │   └── request.ts               # HTTP 请求封装
│   │   ├── router/               # 路由配置
│   │   │   └── index.ts
│   │   ├── stores/               # Pinia 状态管理
│   │   │   └── fileSystem.ts
│   │   └── layout/               # 布局组件
│   │       └── NavbarLayout.vue
│   └── vite.config.ts            # Vite 配置（含代理配置）
│
└── README.md                      # 项目说明文档
```

### 环境准备

#### 必需环境
- **JDK 17+**（项目使用 Java 17）
- **Maven 3.8+**（项目已包含 `mvnw`，可直接使用）
- **Node.js 20.19+ 或 22.12+**（前端要求，见 `package.json`）
- **npm 9+**（或 pnpm/yarn）
- **MySQL 8.0+**（数据库）

#### 可选环境
- **Docker**（如需启用 RawBox 预览中间件）

### 本地快速启动

#### 1. 数据库准备
创建 MySQL 数据库：
```sql
CREATE DATABASE CloudDriveLite CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `Java/src/main/resources/application.properties` 中的数据库连接信息：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/CloudDriveLite?serverTimezone=UTC
spring.datasource.username=你的用户名
spring.datasource.password=你的密码
```

#### 2. 启动后端（Spring Boot）
```bash
cd Java
# Windows
mvnw.cmd spring-boot:run
# Linux/Mac
./mvnw spring-boot:run
```

**后端配置说明**：
- 默认端口：`8080`（可在 `application.properties` 中修改 `server.port`）
- 文件存储路径：`E:/CloudDriveLite/storage`（可在 `FileService` 或配置文件中修改）
- 文件存储规则：`{baseDir}/user_{userId}/yyyy/MM/dd/{uuid}.{ext}`
- 文件上传限制：单文件最大 100MB（可在 `application.properties` 中修改）

#### 3. 启动前端（Vite）
```bash
cd CloudDriveLite-web
npm install
npm run dev
```

**前端配置说明**：
- 默认开发地址：`http://localhost:5173`（以控制台输出为准）
- 代理配置：`/api` 请求会自动代理到 `http://localhost:8080`（见 `vite.config.ts`）

#### 4. 使用说明
1. **注册账号**：访问 `http://localhost:5173/register` 注册新用户
2. **登录系统**：使用注册的账号登录
3. **文件管理**：
   - 上传文件：点击"上传文件"按钮（普通上传）或"上传文件"按钮（大文件分片上传）
   - 创建文件夹：点击"新建文件夹"按钮
   - 进入目录：双击文件夹
   - 返回上级：点击"返回上一级"按钮或使用面包屑导航
   - 文件操作：重命名、移动、删除、下载、预览
   - 批量删除：勾选多个文件后点击"删除所选"

### 核心接口（后端）

> 所有接口均要求已登录会话（通过 `HttpSession` 校验 `SESSION_USER_ID`）。返回数据统一包装为 `ApiResponse` 格式。

#### 用户认证接口（`/api/auth`）
- **注册**：`POST /api/auth/register`
  - 参数：`username`, `userNumber`, `phoneNumber`, `password`, `email`(可选)
- **登录**：`POST /api/auth/login`
  - 参数：`userNumber`, `password`
  - 返回：`{ message, userId }`
- **登出**：`POST /api/auth/logout`

#### 文件管理接口（`/api/files`）
- **文件列表**：`GET /api/files?folderId=0&page=1&size=20`
  - 参数：`folderId`(父目录ID，0为根目录), `page`(页码，从1开始), `size`(每页数量)
  - 返回：`{ items: FileInfoDto[], page, size, total }`
  
- **普通上传**：`POST /api/files/upload`
  - Content-Type: `multipart/form-data`
  - 参数：`file`(文件), `folderId`(可选，默认0)
  - 限制：单文件最大 100MB
  
- **分片上传**：`POST /api/files/upload/chunk`
  - 用于大文件分片上传和断点续传
  - 参数：`chunk`(分片文件), `chunkIndex`(分片索引), `totalChunks`(总分片数), `fileName`(文件名), `fileSize`(文件大小), `folderId`(可选)
  
- **创建文件夹**：`POST /api/files/folder`
  - 参数：`folderName`, `parentId`(可选，默认0)
  
- **重命名**：`PUT /api/files/{id}/rename`
  - 参数：`newName`
  
- **移动**：`PUT /api/files/{id}/move`
  - 参数：`targetParentId`
  
- **删除**：`DELETE /api/files/{id}?recursive=true|false`
  - 参数：`recursive`(是否递归删除，文件夹必填)
  
- **下载**：`GET /api/files/{id}/download`
  - 返回：文件流，自动设置 `Content-Disposition`
  
- **预览**：`GET /api/files/{id}/preview`
  - 返回：文件流，按文件类型设置 `Content-Type`
  
- **面包屑导航**：`GET /api/files/breadcrumb?folderId=0`
  - 返回：从根目录到指定目录的路径数组

#### 返回数据格式
```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... }
}
```

### 在线预览说明

#### 预览方式
1. **后端直出预览**（默认方式）
   - 接口：`GET /api/files/{id}/preview`
   - 特点：
     - 按文件类型自动设置 `Content-Type`
     - 文本文件（`text/*`）自动使用 UTF-8 编码
     - 图片文件直接返回二进制流
     - 前端新窗口打开预览

2. **RawBox 预览**（可选）
   - 需要先启动 RawBox 容器
   - 接口：`GET /api/files/{id}/rawbox-preview`
   - 适用于需要额外限流、访问控制的场景

#### 支持的预览格式
- **图片**：jpg, png, gif, bmp, webp, svg 等
- **文本**：txt, json, xml, html, css, js, java, py 等
- **其他**：pdf（需浏览器支持）、视频/音频（需浏览器支持）

### 可选：接入 RawBox 作为静态预览（不推荐默认启用）
> 项目已内置更稳妥的后端直出预览，RawBox 为可选方案。若需要额外的静态托管/限流能力，可按下述步骤启用。

1) 启动 RawBox 容器并挂载数据目录
```
docker run -d --name rawbox \
  -p 8082:8080 \
  -v E:\IDEAproject\CloudDriveLite\rawbox-data:/data \
  -e API_TOKENS="rawbox-token-123" \
  dongshull/rawbox
```
2) 通过 RawBox 访问（公开文件示例）
```
http://localhost:8082/your-file-name
```
3) 私有文件示例（需 token）
```
http://localhost:8082/your-file-name?api=rawbox-token-123
```
4) 更多配置（UA 白名单、限流、中间件等）参考 RawBox 文档：`https://github.com/dongshull/rawbox`

备注：本项目当前默认预览路径指向自身后端接口，不强依赖 RawBox。若切换到 RawBox 托管，可自行在后端生成 RawBox 链接或前端拼接相应 URL。

### 配置说明

#### 后端配置（`Java/src/main/resources/application.properties`）
```properties
# 服务端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/CloudDriveLite?serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=你的密码

# JPA 配置
spring.jpa.hibernate.ddl-auto=update  # 自动更新表结构
spring.jpa.show-sql=true              # 显示 SQL 语句

# 文件上传配置
spring.servlet.multipart.max-file-size=100MB      # 单文件最大大小
spring.servlet.multipart.max-request-size=100MB  # 请求最大大小
spring.servlet.multipart.enabled=true

# Session 配置
server.servlet.session.timeout=30m    # 会话超时时间
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
```

**注意**：文件存储路径在 `FileService` 中硬编码为 `E:/CloudDriveLite/storage`，如需修改请编辑 `FileService.java` 中的 `baseDir` 字段或通过配置文件注入。

#### 前端配置（`CloudDriveLite-web/vite.config.ts`）
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true,
    }
  }
}
```

### 开发建议

#### 代码规范
- 统一使用 **UTF-8** 编码
- 前端使用 **TypeScript** 严格模式
- 后端遵循 **Spring Boot** 最佳实践

#### 性能优化
- ✅ 已实现大文件分片上传和断点续传
- ✅ 文件列表支持分页，避免一次性加载大量数据
- ⚠️ 大文件下载建议使用流式传输，避免内存溢出
- ⚠️ 可考虑添加文件缓存机制，减少重复下载

#### 安全建议
- ✅ 已实现基于 Session 的登录鉴权
- ✅ 密码使用 BCrypt 加密存储
- ⚠️ 建议添加 CSRF 防护（Spring Security）
- ⚠️ 建议添加文件类型白名单验证（已部分实现）
- ⚠️ 建议添加文件大小限制（已配置 100MB）
- ⚠️ 建议添加请求频率限制（防止恶意上传）

#### 功能扩展建议
- [ ] 文件分享功能（生成分享链接）
- [ ] 文件搜索功能（按文件名、类型搜索）
- [ ] 文件版本管理
- [ ] 回收站功能（软删除）
- [ ] 文件标签/分类
- [ ] 存储空间统计
- [ ] 文件预览支持更多格式（PDF、Office 等）

### 项目信息

- **GitHub 仓库**：https://github.com/bilipeng/CloudDriveLite
- **后端技术**：Spring Boot 3.5.5 + MySQL + JPA
- **前端技术**：Vue 3 + TypeScript + Element Plus + Vite
- **开发状态**：持续开发中

### 许可证

未显式声明，默认以项目实际 LICENSE 为准。如需开源，请在根目录添加 LICENSE 文件。

---

**贡献者**：欢迎提交 Issue 和 Pull Request！

**更新日志**：
- ✅ 支持大文件分片上传和断点续传
- ✅ 完善文件管理功能（批量删除、面包屑导航）
- ✅ 优化用户体验（拖拽上传、进度显示）


