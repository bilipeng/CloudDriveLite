# CloudDriveLite 后端实现说明（基于当前代码）

> 本文档面向“从接口一路读到落地”的阅读方式，重点覆盖：
> - 大文件分片上传：分片保存、断点续传探测、合并策略
> - 文件元数据如何入库、返回给前端哪些字段
> - 下载/预览如何实现（鉴权、Content-Type、Resource 返回）

---

## 1. 技术栈与模块划分

- 后端框架：Spring Boot（Controller + Service + Repository）
- 持久化：Spring Data JPA + MySQL
- 文件存储：本地磁盘目录（`storage.base-dir`，默认 `E:/CloudDriveLite/storage`）
- 登录态：HttpSession（Cookie：`JSESSIONID`）
- 返回结构：统一 `ApiResponse<T>`（部分接口直接 `ResponseEntity` 返回二进制流/Map）

关键代码位置：
- 控制层：`Java/src/main/java/com/peng/clouddrivelite/control`
- 业务层：`Java/src/main/java/com/peng/clouddrivelite/service`
- 数据层：`Java/src/main/java/com/peng/clouddrivelite/repository`
- 实体：`Java/src/main/java/com/peng/clouddrivelite/entity`
- 配置：`Java/src/main/resources/application.properties`

---

## 2. 认证与鉴权（Session）

### 2.1 登录

- 接口：`POST /api/auth/login`
- 逻辑：`AuthController.login()`
- 成功后写入 Session：
  - `SessionKeys.SESSION_USER_ID`
  - `SessionKeys.SESSION_USER_NUMBER`

返回：
```json
{
  "message": "登录成功",
  "userId": 1,
  "userName": "xxx"
}
```

### 2.2 文件接口鉴权方式

文件相关接口统一通过 `FileController.requireUser(session)` 获取 `userId`：
- Session 缺失直接抛 `RuntimeException("未登录")`
- 进一步访问文件时，通过 `fileService.findOwned(userId, fileId)` 校验所有权

全局异常：`GlobalExceptionHandler` 会把 `RuntimeException` 转为 `400` 并返回：
```json
{ "message": "xxx" }
```

---

## 3. 文件元数据设计（数据库实体 FileObject）

实体：`com.peng.clouddrivelite.entity.FileObject`

核心字段（与“上传/预览/下载”强相关）：
- `id`：文件记录主键
- `userId`：归属用户
- `parentId`：父文件夹（0=根目录）
- `fileName`：原始文件名（展示名）
- `storedFileName`：落盘文件名（uuid.ext）
- `filePath`：落盘路径（当前实现是绝对路径字符串）
- `fileSize`：字节数
- `fileType`：MIME 类型（或文件夹固定为 `folder`）
- `isFolder`：是否文件夹
- `uploadedTime/createdTime/updatedTime`：时间字段（`@PrePersist` 自动写入）

说明：
- 文件夹记录：`isFolder=true`，并在 `@PrePersist` 中把 `fileType="folder"`、`fileSize=0`、`filePath/storedFileName` 置空字符串。

Repository：`FileRepository`
- `findByIdAndUserId`：用于鉴权
- `findByUserIdAndParentIdOrderByFolderFirst`：列表（文件夹置顶）
- `findByUserIdAndFileNameContainingIgnoreCaseOrderByUploadedTimeDesc`：搜索

---

## 4. API 返回给前端什么（FileInfoDto + ApiResponse）

### 4.1 通用返回结构 ApiResponse

`ApiResponse<T>` 字段：
- `success`：是否成功
- `message`：提示信息
- `data`：数据
- `timestamp`：时间
- `path`：当前代码里未看到统一赋值点（字段存在）

### 4.2 文件列表/上传完成时返回的 DTO：FileInfoDto

DTO：`com.peng.clouddrivelite.dto.FileInfoDto`

由 `FileDtoService.convertToFileInfoDto(FileObject)` 构造：
- `id`
- `parentId`
- `fileName`
- `fileType`
- `fileSize`
- `fileSizeFormatted`：文件夹为 `-`，文件为格式化后的大小字符串
- `downloadUrl`：文件才有：`/api/files/{id}/download`
- `previewUrl`：文件才有：`/api/files/{id}/preview`
- `isImage`：是否图片（后端判断 `fileType` 是否 `image/*`）
- `folder`：是否文件夹
- `uploadedTime`

注意：
- 上传接口在“分片未合并完成”时返回 `ApiResponse.success("分片上传成功", null)`，即 data 为 `null`。

---

## 5. 上传：普通上传 vs 分片上传（核心）

后端同时支持：
- 普通上传：`FileService.upload(userId, file, folderId)`（当前 `FileController` 的上传接口走的是“分片逻辑 + 兼容单分片”。当 `totalChunks` 规范化为 1 时，本质上也会走分片保存+合并。）
- 分片上传：`FileController.upload()` → `FileService.saveChunk()` → `FileService.tryMergeChunks()`

### 5.1 前端分片上传交互（基于 simple-uploader/flow.js 约定）

后端提供两个与分片相关的 endpoint（同一路径不同方法）：

1) **分片探测**：`GET /api/files/upload`
- 参数（支持两套命名）：
  - `resumableIdentifier` / `identifier`
  - `resumableChunkNumber` / `chunkNumber`
- 返回：
  - `200 OK`：分片已存在
  - `204 No Content`：分片不存在（为了避免某些库把 404 当错误）

2) **分片上传**：`POST /api/files/upload` (multipart/form-data)
- 参数（支持两套命名）：
  - 文件字段：`file`
  - 目标目录：`folderId`（默认 0）
  - 分片：
    - `resumableChunkNumber` / `chunkNumber`
    - `resumableTotalChunks` / `totalChunks`
    - `resumableChunkSize` / `chunkSize`
    - `resumableTotalSize` / `totalSize`
    - `resumableIdentifier` / `identifier`
    - `resumableFilename` / `filename`

### 5.2 后端参数规范化：ChunkUploadParams

类：`com.peng.clouddrivelite.dto.ChunkUploadParams`

`normalize(originalFilename, fileSize, userId)` 规则：
- `filename` 为空：使用 `originalFilename`
- `totalSize` 为空或 <=0：使用当前 `MultipartFile.size`
- `totalChunks` 为空或 <=0：置为 1
- `chunkNumber` 为空或 <=0：置为 1
- `identifier` 为空：用 `(filename + "_" + totalSize + "_" + userId)` 生成，再把非 `[a-zA-Z0-9_-]` 字符替换掉

这意味着：
- 即便前端不传分片参数，后端也会把它当作 `totalChunks=1` 的“单分片上传”。

---

## 6. 分片落盘与断点续传

### 6.1 分片存储目录结构

配置：`FileService.baseDir`
- 注入：`@Value("${storage.base-dir:E:/CloudDriveLite/storage}")`

分片目录：
```
{baseDir}/chunks/user_{userId}/{identifier}/
```

单个分片文件名：直接用分片序号（字符串）：
```
{chunkNumber}
```

举例：
```
E:/CloudDriveLite/storage/chunks/user_1/abc123/1
E:/CloudDriveLite/storage/chunks/user_1/abc123/2
...
```

### 6.2 分片是否存在：chunkExists

实现：`FileService.chunkExists(userId, identifier, chunkNumber)`
- 直接 `Files.exists(chunkPath)`

### 6.3 保存分片：saveChunk

实现：`FileService.saveChunk(userId, identifier, chunkNumber, MultipartFile file)`
- `getChunkDir()` 确保目录存在
- 通过 `InputStream -> OutputStream` 写入指定 chunk 文件

关键点：
- 分片保存没有额外 hash 校验，主要依赖前端的重试 + 后端探测。

---

## 7. 合并分片：tryMergeChunks（拼接过程细节）

实现：`FileService.tryMergeChunks(userId, identifier, totalChunks, originalFilename, totalSize, folderId)`
返回：`Optional<FileObject>`
- **若分片未齐**：返回 `Optional.empty()`
- **若合并完成**：返回入库后的 `FileObject`

### 7.1 合并前：检查是否所有分片都到齐

```java
for (int i = 1; i <= totalChunks; i++) {
    if (!Files.exists(dir.resolve(String.valueOf(i)))) {
        return Optional.empty();
    }
}
```

含义：
- 只要缺任意一个分片，就不会触发合并；上传接口会返回“分片上传成功”。

### 7.2 合并前：存储空间校验

- 从 `User.maxStorage` 读取配额
- `calculateUserStorage(userId)` 计算已用（当前实现是 `fileRepository.findAll()` 再 filter，属于 O(N) 粗实现）
- `used + totalSize > maxStorage` 则抛异常

### 7.3 确定最终文件落盘路径（与普通上传一致）

最终文件保存目录：按用户 + 日期分层：
```
{baseDir}/user_{userId}/YYYY/MM/DD/{storedFileName}
```

`storedFileName` 生成逻辑：
- `UUID.randomUUID().toString().replace("-", "")`
- 如果原文件名有扩展名，则拼上 `.ext`

### 7.4 真正的“拼接”实现

核心写法：**按分片序号从 1..N 依次读取并顺序写入同一个输出流**。

```java
try (OutputStream out = Files.newOutputStream(target)) {
    for (int i = 1; i <= totalChunks; i++) {
        Path chunkPath = dir.resolve(String.valueOf(i));
        try (InputStream in = Files.newInputStream(chunkPath)) {
            in.transferTo(out);
        }
    }
}
```

这就是后端如何把“小文件”拼成“大文件”的：
- 分片文件本身是二进制片段
- 顺序拼接即可恢复原文件内容

### 7.5 合并后校验与清理

- 校验：读取 `Files.size(target)`，与 `totalSize` 不一致只 `warn`，不回滚。
- 清理：删除每个 chunk 文件，然后删除分片目录 `identifier` 目录。

### 7.6 合并后：入库 FileObject

```java
FileObject fo = new FileObject();
fo.setUserId(userId);
fo.setFileName(originalFilename);
fo.setStoredFileName(stored);
fo.setFilePath(target.toString());
fo.setParentId(folderId != null ? folderId : 0L);
fo.setFileSize(realSize);
fo.setFileType(contentType);
FileObject saved = fileRepository.save(fo);
```

然后做一次“并发保护”的二次配额检查：
- 若超额：删除数据库记录 + 删除物理文件，再抛异常

---

## 8. 上传接口最终返回什么

入口：`FileController.upload()`

- 保存分片成功后会调用 `tryMergeChunks(...)`
- 若 `merged.isPresent()`：
  - 将 `FileObject` 转 `FileInfoDto`
  - 返回：`ApiResponse.success("文件上传成功", fileInfoDto)`

- 若未合并完成（缺分片）：
  - 返回：`ApiResponse.success("分片上传成功", null)`

因此前端通常需要：
- 逐片上传
- 最后一片上传后等待服务端返回 `data != null` 或通过列表刷新看到新文件

---

## 9. 下载实现（/download）

接口：`GET /api/files/{id}/download`

流程：
1) 从 Session 获取 `userId`
2) `fileService.findOwned(userId, id)` 校验文件归属
3) `fileService.loadAsResource(fo)` 返回 `FileSystemResource(fo.getFilePath())`
4) 设置响应头：
   - `Content-Disposition: attachment; filename*=UTF-8''{urlencodedFileName}`
   - `Content-Type: fo.getFileType()`

返回体：二进制流（Resource）

---

## 10. 预览实现（/preview + preview-url）

### 10.1 直接预览接口：GET /api/files/{id}/preview

流程与下载类似，但关键区别：
- 不设置 `Content-Disposition: attachment`，浏览器会尝试直接展示
- `Content-Type` 取 `fo.getFileType()`
- 对文本类型做了 charset 处理：
  - 若 `fileType.startsWith("text/")`：强制 `text/plain; charset=UTF-8`

返回体：二进制流（Resource）

### 10.2 获取预览 URL：GET /api/files/{id}/preview-url

- 先鉴权
- 只允许图片：`fileService.isImage(fo)`（判断 `image/*`）
- 返回：
```json
{
  "success": true,
  "message": "获取预览链接成功",
  "data": {
    "previewUrl": "/api/files/{id}/preview",
    "fileName": "xxx.png",
    "fileType": "image/png"
  }
}
```

### 10.3 rawbox-preview（当前实现已不依赖 RawBox）

`GET /api/files/{id}/rawbox-preview`
- 注释写明：直接使用后端预览接口，不依赖 RawBox
- 实际返回内容和 `preview-url` 类似

### 10.4 关于 RawBox copyToRawBox

`FileService.copyToRawBox(FileObject fo)` 会把文件复制到：
- `rawbox-data/public`（相对路径）
并生成一个 `file_{id}_{timestamp}.ext` 文件名。

但从 `FileController` 的当前路由来看，预览链接最终还是指向 `/api/files/{id}/preview`；RawBox 相关更像遗留/备用方案。

---

## 11. 其他相关配置与限制

`application.properties` 中与上传有关：
- `spring.servlet.multipart.max-file-size=100MB`
- `spring.servlet.multipart.max-request-size=100MB`

注意：
- 分片上传能“绕过单次请求很大”的问题，但每个分片仍受 `max-file-size` 限制。

---

## 12. 你关心的“端到端”总结（大文件上传全链路）

### 12.1 大文件上传端到端（一步不跳）

1) 前端选择大文件
2) 前端生成：
   - `identifier`（文件唯一标识）
   - `totalChunks`、`chunkNumber`、`chunkSize`、`totalSize`、`filename`
3) 对每个分片 i：
   1. 调用 `GET /api/files/upload?resumableIdentifier=...&resumableChunkNumber=i`
   2. 若返回 200：跳过该分片（断点续传）
   3. 若返回 204：执行上传
   4. 调用 `POST /api/files/upload`（multipart）上传 `file` 分片与参数
   5. 后端保存分片到 `{baseDir}/chunks/user_{userId}/{identifier}/{i}`
   6. 后端检查 1..N 是否都存在：
      - 否：返回 `ApiResponse.success("分片上传成功", null)`
      - 是：开始合并
4) 合并时：
   - 生成最终落盘路径 `{baseDir}/user_{userId}/YYYY/MM/DD/{uuid.ext}`
   - 依次读取 1..N 分片顺序写入同一个输出流
   - 删除分片目录
   - 入库 `FileObject`（保存 `filePath/fileType/fileSize/...`）
   - 返回 `ApiResponse.success("文件上传成功", FileInfoDto)`

### 12.2 文件信息如何保存、返还什么

- 保存：`FileObject` 写入 MySQL（JPA `file_object` 表）
- 返回：上传合并完成后返回 `FileInfoDto`（含 download/preview URL）

---

## 13. 已知实现特点/可改进点（读代码时的注意项）

- `calculateUserStorage()` 当前实现会 `findAll()` 再 filter，数据量大时性能差；更理想是写 SQL 聚合（sum）或维护已用空间字段。
- 分片合并只校验 size（且 mismatch 仅 warn），没有 hash 校验。
- 分片目录清理：只删除了 `identifier` 目录；如果出现异常中断，可能遗留分片文件。
- `storage.base-dir` 未在 `application.properties` 显式配置，使用的是默认值；部署到非 Windows 盘符需要配置。

---

## 14. 关联源码索引（方便你继续跳转阅读）

- 上传/下载/预览接口：
  - `com.peng.clouddrivelite.control.FileController`
- 分片保存/合并/落盘/入库：
  - `com.peng.clouddrivelite.service.FileService`
- DTO 组装（downloadUrl/previewUrl/isImage）：
  - `com.peng.clouddrivelite.service.FileDtoService`
- 元数据实体：
  - `com.peng.clouddrivelite.entity.FileObject`
- 数据访问：
  - `com.peng.clouddrivelite.repository.FileRepository`
- 登录与 Session 写入：
  - `com.peng.clouddrivelite.control.AuthController`
  - `com.peng.clouddrivelite.util.SessionKeys`

