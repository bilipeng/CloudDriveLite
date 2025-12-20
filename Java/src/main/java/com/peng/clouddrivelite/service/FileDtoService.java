package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.dto.FileInfoDto;
import com.peng.clouddrivelite.entity.FileObject;
import com.peng.clouddrivelite.util.FileUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件 DTO 转换服务
 * <p>
 * 该类的职责是把数据库实体 {@link com.peng.clouddrivelite.entity.FileObject} 转换为前端更易用的 DTO。
 * 其中包含：
 * - 前端展示需要的格式化字段（例如 fileSizeFormatted）
 * - 后端接口约定的下载/预览 URL（downloadUrl/previewUrl）
 * - 图片类型判断（isImage），用于前端决定是否展示“图片预览”组件
 * </p>
 */
@Service
public class FileDtoService {

    private final FileService fileService;

    public FileDtoService(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 将FileObject转换为FileInfoDto
     *
     * @param fileObject 文件对象
     * @return 前端友好的文件信息DTO
     */
    public FileInfoDto convertToFileInfoDto(FileObject fileObject) {
        FileInfoDto dto = new FileInfoDto();
        dto.setId(fileObject.getId());
        dto.setParentId(fileObject.getParentId());
        dto.setFileName(fileObject.getFileName());
        dto.setFileType(fileObject.getFileType());
        dto.setFileSize(fileObject.getFileSize());
        dto.setFileSizeFormatted(fileObject.isFolder() ? "-" : FileUtils.formatFileSize(fileObject.getFileSize()));
        // URL 构造约定：前端拿到该 URL 后直接请求即可。
        // 注意：这里只返回路径（相对 URL），由前端决定拼接域名/端口。
        dto.setDownloadUrl(fileObject.isFile() ? ("/api/files/" + fileObject.getId() + "/download") : null);
        dto.setPreviewUrl(fileObject.isFile() ? ("/api/files/" + fileObject.getId() + "/preview") : null);
        dto.setIsImage(fileObject.isFile() && fileService.isImage(fileObject));
        dto.setFolder(fileObject.isFolder());
        dto.setUploadedTime(fileObject.getUploadedTime());
        return dto;
    }

    /**
     * 构建面包屑列表
     *
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @return 面包屑列表
     */
    public List<Map<String, Object>> buildBreadcrumb(Long folderId, Long userId) {
        List<FileObject> path = fileService.getFolderPath(userId, folderId);
        List<Map<String, Object>> breadcrumb = new ArrayList<>();
        breadcrumb.add(Map.of("id", 0L, "name", "根目录"));
        for (FileObject f : path) {
            breadcrumb.add(Map.of("id", f.getId(), "name", f.getFileName()));
        }
        return breadcrumb;
    }

    /**
     * 构建下载URL信息
     *
     * @param fileId 文件ID
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return URL信息Map
     */
    public Map<String, String> buildDownloadUrlInfo(Long fileId, String fileName, Long fileSize) {
        return Map.of(
                "downloadUrl", "/api/files/" + fileId + "/download",
                "fileName", fileName,
                "fileSize", FileUtils.formatFileSize(fileSize)
        );
    }

    /**
     * 构建预览URL信息
     *
     * @param fileId 文件ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @return URL信息Map
     */
    public Map<String, String> buildPreviewUrlInfo(Long fileId, String fileName, String fileType) {
        return Map.of(
                "previewUrl", "/api/files/" + fileId + "/preview",
                "fileName", fileName,
                "fileType", fileType
        );
    }
}



