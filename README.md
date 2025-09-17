## CloudDriveLite

一个轻量级的个人网盘/文件管理系统，包含前后端与可选文件预览中间件，支持文件的上传、下载、移动、重命名、删除、路径导航以及在线预览。

### 功能特性
- **用户会话**：基于后端会话的登录态校验（接口内校验 `SESSION_USER_ID`）。
- **文件管理**：
  - 列表/分页、按目录浏览
  - 新建文件夹、重命名、移动、删除（支持递归删除）
  - 上传（自动记录大小、类型、时间）、下载
  - 面包屑导航
- **在线预览**：
  - 后端直出预览接口，按 `Content-Type` 返回资源
  - 文本内容自动 UTF-8（`text/*`）
  - 前端一键“预览”按钮
- **可选中间件**：支持以 RawBox 作为静态文件托管/预览中转（可选，默认不需要）

### 技术栈
- 后端：Java、Spring Boot、Spring Data JPA
- 前端：Vue 3、Vite、TypeScript、Element Plus
- 构建/依赖：Maven、npm
- 可选预览中间件：RawBox（Go 实现的轻量文件托管服务，参考 `dongshull/rawbox` 仓库说明 [`https://github.com/dongshull/rawbox`]）

### 目录结构（简要）
- `Java/`：后端 Spring Boot 工程
  - `src/main/java/com/peng/clouddrivelite/...`
    - `control/FileController.java`：文件 CRUD 与预览接口
    - `service/FileService.java`：文件存储、类型识别、递归删除等
  - `src/main/resources/application.properties`：后端配置
- `CloudDriveLite-web/`：前端工程（Vite）
  - `src/views/FileManager.vue`：主文件管理界面（上传、列表、预览等）
  - `src/api/files.ts`：文件相关 API 封装
- `rawbox-data/`（可选）：RawBox 数据目录（容器挂载点）

### 环境准备
- JDK 17+（建议）
- Maven 3.8+
- Node.js 18+、npm 9+（或 pnpm/yarn）
- 可选：Docker（如需启用 RawBox）

### 本地快速启动
1) 启动后端（Spring Boot）
```
cd Java
./mvnw spring-boot:run   # Windows 可用 mvnw.cmd spring-boot:run
```
默认端口（若未改动）通常为 `8080`。后端会按如下规则保存文件：
- 存储根目录：`storage.base-dir`（在 `FileService` 中有默认值 `E:/CloudDriveLite/storage`，可在 `application.properties` 配置）
- 存储路径：`{baseDir}/user_{userId}/yyyy/MM/dd/{uuid}.{ext}`

2) 启动前端（Vite）
```
cd CloudDriveLite-web
npm install
npm run dev
```
默认开发地址：`http://localhost:5173`（以控制台输出为准）。

3) 登录与使用
- 登录后进入文件管理页：上传、创建文件夹、重命名、移动、删除、下载、预览等
- 双击文件夹进入下一级；通过面包屑快速回退

### 核心接口（后端）
> 仅列出和前端对接的主要接口，实际以代码为准（`FileController.java`）。接口均要求已登录会话。

- 列表：`GET /api/files?folderId=0&page=1&size=20`
- 上传：`POST /api/files/upload`（`multipart/form-data`，字段名 `file`，可选 `folderId`）
- 创建文件夹：`POST /api/files/folder`（`folderName`，可选 `parentId`）
- 重命名：`PUT /api/files/{id}/rename`（`newName`）
- 移动：`PUT /api/files/{id}/move`（`targetParentId`）
- 面包屑：`GET /api/files/breadcrumb?folderId=...`
- 删除：`DELETE /api/files/{id}?recursive=true|false`
- 下载：`GET /api/files/{id}/download`
- 预览：`GET /api/files/{id}/preview`

返回数据通常包装为：
```
{
  success: boolean,
  message: string,
  data: ...
}
```

### 在线预览说明
- 直接使用 `GET /api/files/{id}/preview` 返回资源：
  - `Content-Type` 依据文件类型设置
  - `text/*` 返回 `UTF-8` 编码
- 前端在 `FileManager.vue` 中提供“预览”按钮，自动打开新窗口进行查看

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

### 配置说明（后端示例）
- `application.properties` 常用项：
  - `server.port`：后端端口
  - `storage.base-dir`：文件物理存储根路径（覆盖 `FileService` 默认值）

### 开发建议
- 统一使用 UTF-8
- 避免在大文件上执行同步 I/O，可按需扩展断点续传/大文件分片
- 完善登录鉴权与 CSRF/同源策略（当前接口基于会话）

### 许可证
未显式声明，默认以项目实际 LICENSE 为准。如需开源，请在根目录添加 LICENSE 文件。


