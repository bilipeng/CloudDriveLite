package com.peng.clouddrivelite.control;

import com.peng.clouddrivelite.dto.ApiResponse;
import com.peng.clouddrivelite.dto.FileInfoDto;
import com.peng.clouddrivelite.dto.ChunkUploadParams;
import com.peng.clouddrivelite.entity.FileObject;
import com.peng.clouddrivelite.service.FileService;
import com.peng.clouddrivelite.service.FileDtoService;
import com.peng.clouddrivelite.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件管理控制器
 * <p>
 * 处理所有与文件操作相关的HTTP请求，包括：
 * - 文件上传（支持分片上传）
 * - 文件下载和预览
 * - 文件管理（创建、重命名、移动、删除）
 * - 文件列表和搜索
 * </p>
 * 所有操作都需要用户登录，并且只能操作用户自己的文件
 */
@RestController
@RequestMapping("/api/files")
@Validated
public class FileController {

    private final FileService fileService;
    private final FileDtoService fileDtoService;
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    public FileController(FileService fileService, FileDtoService fileDtoService) {
        this.fileService = fileService;
        this.fileDtoService = fileDtoService;
    }

    /**
     * 从 Session 中强制获取当前登录用户。
     * <p>
     * 本项目的鉴权方式是：登录成功后把 userId 写入 HttpSession（见 AuthController.login）。
     * 之后所有需要登录的接口都通过这里读取 userId。
     * </p>
     *
     * <p>
     * 注意：这里仅负责“是否登录”的判断；
     * 对于具体文件是否属于当前用户，还需要在后续通过 {@code fileService.findOwned(userId, fileId)} 校验。
     * </p>
     */
    private Long requireUser(HttpSession session) {
        Object userId = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return Long.valueOf(userId.toString());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "0") Long folderId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String keyword,
                                                 HttpSession session) {
        try {
            Long userId = requireUser(session);
            log.debug("list files userId={}, folderId={}, page={}, size={}, keyword={}", userId, folderId, page, size, keyword);

            Page<FileObject> files;
            // .trim().isEmpty() 防止空指针异常
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 搜索模式
                files = fileService.search(userId, keyword.trim(), page - 1, size);
                log.debug("search mode: {} items of {} total", files.getNumberOfElements(), files.getTotalElements());
            } else {
                // 普通列表模式
                files = fileService.list(userId, folderId, page - 1, size);
                log.debug("list mode: {} items of {} total", files.getNumberOfElements(), files.getTotalElements());
            }

            // 转换为前端友好的DTO
            List<FileInfoDto> fileInfoList = files.getContent().stream()
                    .map(fileDtoService::convertToFileInfoDto)
                    .collect(Collectors.toList());

            // 构建分页信息
            Map<String, Object> result = Map.of(
                    "items", fileInfoList,
                    "page", page,
                    "size", size,
                    "total", files.getTotalElements(),
                    "keyword", keyword != null ? keyword : ""
            );

            return ApiResponse.success("获取文件列表成功", result);
        } catch (Exception e) {
            log.error("list files failed: {}", e.getMessage());
            return ApiResponse.error("获取文件列表失败: " + e.getMessage());
        }
    }

    /** 创建文件夹 */
    @PostMapping("/folder")
    public ApiResponse<FileInfoDto> createFolder(@RequestParam String folderName,
                                                 @RequestParam(defaultValue = "0") Long parentId,
                                                 HttpSession session) {
        try {
            Long userId = requireUser(session);
            FileObject folder = fileService.createFolder(userId, folderName, parentId);
            FileInfoDto info = fileDtoService.convertToFileInfoDto(folder);
            return ApiResponse.success("文件夹创建成功", info);
        } catch (Exception e) {
            return ApiResponse.error("创建文件夹失败: " + e.getMessage());
        }
    }

    /** 重命名 */
    @PutMapping("/{id}/rename")
    public ApiResponse<FileInfoDto> rename(@PathVariable Long id,
                                           @RequestParam String newName,
                                           HttpSession session) {
        try {
            Long userId = requireUser(session);
            FileObject updated = fileService.rename(userId, id, newName);
            return ApiResponse.success("重命名成功", fileDtoService.convertToFileInfoDto(updated));
        } catch (Exception e) {
            return ApiResponse.error("重命名失败: " + e.getMessage());
        }
    }

    /** 移动 */
    @PutMapping("/{id}/move")
    public ApiResponse<FileInfoDto> move(@PathVariable Long id,
                                         @RequestParam Long targetParentId,
                                         HttpSession session) {
        try {
            Long userId = requireUser(session);
            FileObject updated = fileService.move(userId, id, targetParentId);
            return ApiResponse.success("移动成功", fileDtoService.convertToFileInfoDto(updated));
        } catch (Exception e) {
            return ApiResponse.error("移动失败: " + e.getMessage());
        }
    }

    /** 面包屑 */
    @GetMapping("/breadcrumb")
    public ApiResponse<List<Map<String, Object>>> getBreadcrumb(@RequestParam(defaultValue = "0") Long folderId,
                                                                HttpSession session) {
        try {
            Long userId = requireUser(session);
            List<Map<String, Object>> breadcrumb = fileDtoService.buildBreadcrumb(folderId, userId);
            return ApiResponse.success("获取路径成功", breadcrumb);
        } catch (Exception e) {
            return ApiResponse.error("获取路径失败: " + e.getMessage());
        }
    }

    /**
     * 检查分片是否已上传
     * <p>
     * 支持断点续传，前端在上传每个分片前会先调用此接口检查该分片是否已上传。
     * - 返回 200 OK: 分片已存在，无需重复上传
     * - 返回 204 No Content: 分片不存在，需要上传
     * </p>
     *
     * @param identifier 文件唯一标识符
     * @param chunkNumber 分片编号（从1开始）
     * @param altIdentifier 备用标识符参数名
     * @param altChunkNumber 备用分片编号参数名
     * @param session HTTP会话，用于获取当前登录用户
     * @return 200-分片已存在，204-分片不存在
     */
    @GetMapping(path = "/upload")
    public ResponseEntity<Void> checkChunk(
            @RequestParam(name = "resumableIdentifier", required = false) String identifier,
            @RequestParam(name = "resumableChunkNumber", required = false) Integer chunkNumber,

            // 兼容备用命名（某些前端库可能使用不同的参数名）
            @RequestParam(name = "identifier", required = false) String altIdentifier,
            @RequestParam(name = "chunkNumber", required = false) Integer altChunkNumber,
            HttpSession session) {
        Long userId = requireUser(session);
        if (identifier == null && altIdentifier != null) identifier = altIdentifier;
        if (chunkNumber == null && altChunkNumber != null) chunkNumber = altChunkNumber;

        if (identifier == null || chunkNumber == null) {
            return ResponseEntity.badRequest().build();
        }
        log.debug("check chunk userId={}, id={}, no={}", userId, identifier, chunkNumber);

        //服务器会去临时文件夹或数据库里查找：
        // 针对这个用户（userId）、这个文件（identifier），第 chunkNumber 个分片是否已经物理存在。
        boolean exists = fileService.chunkExists(userId, identifier, chunkNumber);
        // 为避免某些前端库将 404 视为错误，这里不存在返回 204 No Content
        return exists ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
    }

    /**
     * 处理文件上传请求
     * 支持普通文件上传和分片上传。对于大文件，前端会将文件分成多个分片上传，
     * 每个分片都会调用此接口，后端负责保存分片并在所有分片上传完成后合并。
     * @param file          上传的文件分片
     * @param folderId      目标文件夹ID，默认为0（根目录）
     * @param chunkNumber   当前分片编号（从1开始）
     * @param totalChunks   总分片数
     * @param chunkSize    分片大小（字节）
     * @param totalSize     文件总大小（字节）
     * @param identifier    文件唯一标识符，用于标识同一个文件的不同分片
     * @param filename      原始文件名
     * @param altChunkNumber    备用分片编号参数名
     * @param altTotalChunks    备用总分片数参数名
     * @param altChunkSize      备用分片大小参数名
     * @param altTotalSize      备用文件总大小参数名
     * @param altIdentifier     备用文件标识符参数名
     * @param altFilename       备用文件名参数名
     * @param session       HTTP会话，用于获取当前登录用户
     * @return 上传结果，包含文件信息或分片上传进度
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileInfoDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") Long folderId,
            // 分片参数（simple-uploader.js 默认）
            @RequestParam(name = "resumableChunkNumber", required = false) Integer chunkNumber,
            @RequestParam(name = "resumableTotalChunks", required = false) Integer totalChunks,
            @RequestParam(name = "resumableChunkSize", required = false) Long chunkSize,
            @RequestParam(name = "resumableTotalSize", required = false) Long totalSize,
            @RequestParam(name = "resumableIdentifier", required = false) String identifier,
            @RequestParam(name = "resumableFilename", required = false) String filename,
            // 兼容其他命名（如 chunkNumber/totalChunks/identifier/filename/totalSize）
            @RequestParam(name = "chunkNumber", required = false) Integer altChunkNumber,
            @RequestParam(name = "totalChunks", required = false) Integer altTotalChunks,
            @RequestParam(name = "chunkSize", required = false) Long altChunkSize,
            @RequestParam(name = "totalSize", required = false) Long altTotalSize,
            @RequestParam(name = "identifier", required = false) String altIdentifier,
            @RequestParam(name = "filename", required = false) String altFilename,
            HttpSession session) {
        try {
            // 1. 验证用户登录状态
            Long userId = requireUser(session);
            log.debug("upload start userId={}", userId);

            // 2. 检查文件参数
            if (file == null) {
                log.warn("upload without file param");
                return ApiResponse.error("请选择要上传的文件");
            }

            log.debug("recv file: {}", file.getOriginalFilename());
            log.debug("chunk params: id={}, no={}, total={}", identifier, chunkNumber, totalChunks);
            log.debug("alt   params: id={}, no={}, total={}", altIdentifier, altChunkNumber, altTotalChunks);

            // 3. 参数回填（若默认命名为空则使用备用命名）
            // 这里处理前端可能使用不同参数名的情况，提高接口兼容性
            if (identifier == null && altIdentifier != null) identifier = altIdentifier;
            if (chunkNumber == null && altChunkNumber != null) chunkNumber = altChunkNumber;
            if (totalChunks == null && altTotalChunks != null) totalChunks = altTotalChunks;
            if (chunkSize == null && altChunkSize != null) chunkSize = altChunkSize;
            if (totalSize == null && altTotalSize != null) totalSize = altTotalSize;
            if ((filename == null || filename.isBlank()) && altFilename != null) filename = altFilename;

            // 4. 规范化上传参数
            // 将分散的参数封装到 ChunkUploadParams 对象中，便于后续处理
            ChunkUploadParams params = new ChunkUploadParams(identifier, chunkNumber, totalChunks,
                    chunkSize, totalSize, filename);
            // 对参数进行校验和规范化处理
            params.normalize(file.getOriginalFilename(), file.getSize(), userId);

            // 5. 保存分片到临时存储
            // 每个分片会以 {userId}/{identifier}/{chunkNumber} 的路径保存
            fileService.saveChunk(userId, params.getIdentifier(), params.getChunkNumber(), file);
            if (log.isDebugEnabled()) {
                log.debug("saved chunk {}/{} id={}", 
                    params.getChunkNumber(), 
                    params.getTotalChunks(), 
                    params.getIdentifier()
                );
            }

            // 6. 如果是最后一个分片，尝试合并所有分片
            // 这里会检查是否所有分片都已上传，如果是则合并，否则返回分片上传成功的响应
            var merged = fileService.tryMergeChunks(
                userId, 
                params.getIdentifier(), 
                params.getTotalChunks(),
                params.getFilename(), 
                params.getTotalSize(), 
                folderId
            );
            
            // 7. 处理合并结果
            if (merged.isPresent()) {
                // 合并成功，将文件信息转换为DTO并返回
                FileInfoDto fileInfo = fileDtoService.convertToFileInfoDto(merged.get());
                log.info("merge completed, fileId={}", merged.get().getId());
                return ApiResponse.success("文件上传成功", fileInfo);
            } else {
                // 非最后一个分片或合并未完成，返回分片上传成功响应
                return ApiResponse.success("分片上传成功", null);
            }
        } catch (Exception e) {
            log.error("upload failed: {}", e.getMessage(), e);
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件（二进制流）。
     * <p>
     * 返回 {@link Resource}：Spring 会把它作为响应体输出给浏览器。
     * 这里通过设置响应头 Content-Disposition=attachment，强制浏览器触发“下载”行为。
     * </p>
     *
     * <p>
     * 鉴权流程：
     * 1) requireUser(session) 得到 userId（确认已登录）
     * 2) fileService.findOwned(userId, id) 确认文件归属当前用户
     * </p>
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            log.debug("download userId={}, fileId={}", userId, id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            log.info("download file: {} ({})", fo.getFileName(), 
                    com.peng.clouddrivelite.util.FileUtils.formatFileSize(fo.getFileSize()));

            Resource resource = fileService.loadAsResource(fo);
            String encoded = URLEncoder.encode(fo.getFileName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.parseMediaType(fo.getFileType()))
                    .body(resource);
        } catch (Exception e) {
            log.error("download failed id={}, err={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取下载链接（JSON）。
     * <p>
     * 与 download 不同：此接口不直接返回文件内容，而是返回前端可用的下载 URL 以及文件名、大小等信息。
     * </p>
     */
    @GetMapping("/{id}/download-url")
    public ApiResponse<Map<String, String>> getDownloadUrl(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            log.debug("download-url userId={}, fileId={}", userId, id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            Map<String, String> result = fileDtoService.buildDownloadUrlInfo(id, fo.getFileName(), fo.getFileSize());

            return ApiResponse.success("获取下载链接成功", result);
        } catch (Exception e) {
            log.error("get download url failed id={}, err={}", id, e.getMessage());
            return ApiResponse.error("获取下载链接失败: " + e.getMessage());
        }
    }

    /**
     * 预览文件（二进制流）。
     * <p>
     * 与 download 的差别：
     * - 不设置 Content-Disposition=attachment，让浏览器自行决定是“直接展示”还是“下载”。
     * - 设置 Content-Type，浏览器会据此决定展示方式（例如 image/* 可直接展示）。
     * </p>
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            log.debug("preview userId={}, fileId={}", userId, id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            log.info("preview file: {} ({})", fo.getFileName(), fo.getFileType());

            Resource resource = fileService.loadAsResource(fo);
            
            // 设置适当的 Content-Type
            MediaType contentType = MediaType.parseMediaType(fo.getFileType());
            
            // 对于文本文件，设置 charset
            if (fo.getFileType().startsWith("text/")) {
                contentType = new MediaType("text", "plain", StandardCharsets.UTF_8);
            }
            
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(resource);
        } catch (Exception e) {
            log.error("preview failed id={}, err={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取预览链接（JSON）。
     * <p>
     * 当前实现主要用于“图片预览”：如果不是图片类型，则直接返回错误。
     * 实际预览内容仍由 /preview 接口提供。
     * </p>
     */
    @GetMapping("/{id}/preview-url")
    public ApiResponse<Map<String, String>> getPreviewUrl(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            log.debug("rawbox preview url userId={}, fileId={}", userId, id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            if (!fileService.isImage(fo)) {
                return ApiResponse.error("仅支持图片预览，当前文件类型: " + fo.getFileType());
            }

            Map<String, String> result = fileDtoService.buildPreviewUrlInfo(id, fo.getFileName(), fo.getFileType());

            return ApiResponse.success("获取预览链接成功", result);
        } catch (Exception e) {
            log.error("get preview url failed id={}, err={}", id, e.getMessage());
            return ApiResponse.error("获取预览链接失败: " + e.getMessage());
        }
    }

    /**
     * （兼容/历史接口）获取 RawBox 预览链接。
     * <p>
     * 代码注释中说明：这里已经不再依赖 RawBox，而是直接返回后端 /preview 接口的链接。
     * </p>
     */
    @GetMapping("/{id}/rawbox-preview")
    public ApiResponse<Map<String, String>> getRawBoxPreviewUrl(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            System.out.println("用户ID " + userId + " 请求文件预览链接 ID: " + id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            // 直接使用我们的后端预览接口，不依赖 RawBox
            Map<String, String> result = fileDtoService.buildPreviewUrlInfo(id, fo.getFileName(), fo.getFileType());

            return ApiResponse.success("获取预览链接成功", result);
        } catch (Exception e) {
            System.err.println("获取预览链接失败 ID: " + id + ", 错误: " + e.getMessage());
            return ApiResponse.error("获取预览链接失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> delete(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "false") boolean recursive,
                                                   HttpSession session) {
        try {
            Long userId = requireUser(session);
            log.debug("delete userId={}, fileId={}", userId, id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            if (recursive || fo.isFile()) {
                fileService.deleteRecursively(fo);
            } else {
                // 非递归时，若是文件夹且不为空，直接提示
                // 简化处理：由deleteRecursively兜底；此处直接返回错误提示可优化
                fileService.delete(fo);
            }

            log.info("delete success id={}", id);
            Map<String, String> result = Map.of(
                    "deletedItem", fo.getFileName(),
                    "type", fo.isFolder() ? "folder" : "file"
            );

            return ApiResponse.success("文件删除成功", result);
        } catch (Exception e) {
            log.error("delete failed id={}, err={}", id, e.getMessage());
            return ApiResponse.error("删除文件失败: " + e.getMessage());
        }
    }

}