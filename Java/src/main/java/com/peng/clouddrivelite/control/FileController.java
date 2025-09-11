package com.peng.clouddrivelite.control;

import com.peng.clouddrivelite.dto.ApiResponse;
import com.peng.clouddrivelite.dto.FileInfoDto;
import com.peng.clouddrivelite.entity.FileObject;
import com.peng.clouddrivelite.service.FileService;
import com.peng.clouddrivelite.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@Validated
public class FileController {

    private final FileService fileService;

    // 文件大小限制：100MB
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    // 允许的文件类型（可选，用于额外验证）
    private static final String[] ALLOWED_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif",
            "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv", "3gp",
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf",
            "zip", "rar", "7z", "tar", "gz", "bz2",
            "java", "js", "css", "html", "htm", "xml", "json", "yaml", "yml", "sql", "py", "cpp", "c", "php",
            "exe", "msi", "dmg", "iso", "apk"
    };

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    private Long requireUser(HttpSession session) {
        Object userId = session.getAttribute(SessionKeys.SESSION_USER_ID);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return Long.valueOf(userId.toString());
    }

    /**
     * 验证上传的文件
     *
     * @param file 上传的文件
     * @throws RuntimeException 如果文件验证失败
     */
    private void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 检查文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // 检查文件扩展名
        String extension = getFileExtension(originalFilename);
        if (extension != null && !isAllowedExtension(extension)) {
            throw new RuntimeException("不支持的文件类型: " + extension);
        }

        System.out.println("文件验证通过: " + originalFilename + " (" + file.getSize() + " bytes)");
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（小写，不包含点号）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 检查文件扩展名是否被允许
     *
     * @param extension 文件扩展名
     * @return 是否允许
     */
    private boolean isAllowedExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "0") Long folderId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 HttpSession session) {
        try {
            Long userId = requireUser(session);
            System.out.println("用户ID " + userId + " 请求文件列表，folderId: " + folderId + ", 页码: " + page + ", 每页大小: " + size);

            Page<FileObject> files = fileService.list(userId, folderId, page - 1, size);
            System.out.println("返回 " + files.getNumberOfElements() + " 个文件，共 " + files.getTotalElements() + " 个文件");

            // 转换为前端友好的DTO
            List<FileInfoDto> fileInfoList = files.getContent().stream()
                    .map(this::convertToFileInfoDto)
                    .collect(Collectors.toList());

            // 构建分页信息
            Map<String, Object> result = Map.of(
                    "items", fileInfoList,
                    "page", page,
                    "size", size,
                    "total", files.getTotalElements()
            );

            return ApiResponse.success("获取文件列表成功", result);
        } catch (Exception e) {
            System.err.println("获取文件列表失败: " + e.getMessage());
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
            FileInfoDto info = convertToFileInfoDto(folder);
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
            return ApiResponse.success("重命名成功", convertToFileInfoDto(updated));
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
            return ApiResponse.success("移动成功", convertToFileInfoDto(updated));
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
            List<FileObject> path = fileService.getFolderPath(userId, folderId);
            List<Map<String, Object>> breadcrumb = new ArrayList<>();
            breadcrumb.add(Map.of("id", 0L, "name", "根目录"));
            for (FileObject f : path) {
                breadcrumb.add(Map.of("id", f.getId(), "name", f.getFileName()));
            }
            return ApiResponse.success("获取路径成功", breadcrumb);
        } catch (Exception e) {
            return ApiResponse.error("获取路径失败: " + e.getMessage());
        }
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileInfoDto> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "0") Long folderId,
            HttpSession session) {
        try {
            // 1. 验证用户登录状态
            Long userId = requireUser(session);
            System.out.println("=== 文件上传开始 ===");
            System.out.println("用户ID: " + userId);

            // 2. 检查文件参数
            if (file == null) {
                System.err.println("错误：没有收到文件参数");
                return ApiResponse.error("请选择要上传的文件");
            }

            System.out.println("收到文件参数: " + file.getOriginalFilename());

            // 3. 验证文件
            validateFile(file);

            // 4. 记录文件信息
            System.out.println("原始文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize() + " bytes (" + formatFileSize(file.getSize()) + ")");
            System.out.println("浏览器Content-Type: " + file.getContentType());

            // 5. 上传文件到服务器（自动检测文件大小和类型）
            FileObject saved = fileService.upload(userId, file, folderId);

            // 5. 记录上传结果
            System.out.println("文件上传成功!");
            System.out.println("数据库ID: " + saved.getId());
            System.out.println("存储文件名: " + saved.getStoredFileName());
            System.out.println("存储路径: " + saved.getFilePath());
            System.out.println("检测到的文件类型: " + saved.getFileType());
            System.out.println("上传时间: " + saved.getUploadedTime());
            System.out.println("=== 文件上传完成 ===");

            // 6. 转换为前端友好的DTO
            FileInfoDto fileInfo = convertToFileInfoDto(saved);

            return ApiResponse.success("文件上传成功", fileInfo);
        } catch (Exception e) {
            System.err.println("=== 文件上传失败 ===");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== 错误详情结束 ===");
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }
    // 兼容旧接口，如不需要可删除
    // @GetMapping("/list") ...
    /**
     * 格式化文件大小显示
     *
     * @param bytes 字节数
     * @return 格式化后的大小字符串
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            System.out.println("用户ID " + userId + " 请求下载文件 ID: " + id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            System.out.println("下载文件: " + fo.getFileName() + " (" + formatFileSize(fo.getFileSize()) + ")");

            Resource resource = fileService.loadAsResource(fo);
            String encoded = URLEncoder.encode(fo.getFileName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.parseMediaType(fo.getFileType()))
                    .body(resource);
        } catch (Exception e) {
            System.err.println("下载文件失败 ID: " + id + ", 错误: " + e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{id}/download-url")
    public ApiResponse<Map<String, String>> getDownloadUrl(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            System.out.println("用户ID " + userId + " 请求文件下载链接 ID: " + id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            String downloadUrl = "/api/files/" + id + "/download";
            Map<String, String> result = Map.of(
                    "downloadUrl", downloadUrl,
                    "fileName", fo.getFileName(),
                    "fileSize", formatFileSize(fo.getFileSize())
            );

            return ApiResponse.success("获取下载链接成功", result);
        } catch (Exception e) {
            System.err.println("获取下载链接失败 ID: " + id + ", 错误: " + e.getMessage());
            return ApiResponse.error("获取下载链接失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            System.out.println("用户ID " + userId + " 请求预览文件 ID: " + id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            if (!fileService.isImage(fo)) {
                throw new RuntimeException("仅支持图片预览，当前文件类型: " + fo.getFileType());
            }

            System.out.println("预览图片: " + fo.getFileName() + " (" + fo.getFileType() + ")");

            Resource resource = fileService.loadAsResource(fo);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fo.getFileType()))
                    .body(resource);
        } catch (Exception e) {
            System.err.println("预览文件失败 ID: " + id + ", 错误: " + e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{id}/preview-url")
    public ApiResponse<Map<String, String>> getPreviewUrl(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = requireUser(session);
            System.out.println("用户ID " + userId + " 请求文件预览链接 ID: " + id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            if (!fileService.isImage(fo)) {
                return ApiResponse.error("仅支持图片预览，当前文件类型: " + fo.getFileType());
            }

            String previewUrl = "/api/files/" + id + "/preview";
            Map<String, String> result = Map.of(
                    "previewUrl", previewUrl,
                    "fileName", fo.getFileName(),
                    "fileType", fo.getFileType()
            );

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
            System.out.println("用户ID " + userId + " 请求删除文件 ID: " + id);

            FileObject fo = fileService.findOwned(userId, id)
                    .orElseThrow(() -> new RuntimeException("文件不存在或无权限"));

            if (recursive || fo.isFile()) {
                fileService.deleteRecursively(fo);
            } else {
                // 非递归时，若是文件夹且不为空，直接提示
                // 简化处理：由deleteRecursively兜底；此处直接返回错误提示可优化
                fileService.delete(fo);
            }

            System.out.println("文件删除成功");
            Map<String, String> result = Map.of(
                    "deletedItem", fo.getFileName(),
                    "type", fo.isFolder() ? "folder" : "file"
            );

            return ApiResponse.success("文件删除成功", result);
        } catch (Exception e) {
            System.err.println("删除文件失败 ID: " + id + ", 错误: " + e.getMessage());
            return ApiResponse.error("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 将FileObject转换为FileInfoDto
     *
     * @param fileObject 文件对象
     * @return 前端友好的文件信息DTO
     */
    private FileInfoDto convertToFileInfoDto(FileObject fileObject) {
        FileInfoDto dto = new FileInfoDto();
        dto.setId(fileObject.getId());
        dto.setParentId(fileObject.getParentId());
        dto.setFileName(fileObject.getFileName());
        dto.setFileType(fileObject.getFileType());
        dto.setFileSize(fileObject.getFileSize());
        dto.setFileSizeFormatted(fileObject.isFolder() ? "-" : formatFileSize(fileObject.getFileSize()));
        dto.setDownloadUrl(fileObject.isFile() ? ("/api/files/" + fileObject.getId() + "/download") : null);
        dto.setPreviewUrl(fileObject.isFile() ? ("/api/files/" + fileObject.getId() + "/preview") : null);
        dto.setIsImage(fileObject.isFile() && fileService.isImage(fileObject));
        dto.setFolder(fileObject.isFolder());
        dto.setUploadedTime(fileObject.getUploadedTime());
        return dto;
    }
}


