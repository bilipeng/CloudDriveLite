package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.entity.FileObject;
import com.peng.clouddrivelite.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;

    @Value("${storage.base-dir:E:/CloudDriveLite/storage}")
    private String baseDir;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public Page<FileObject> list(Long userId, Long folderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (folderId == null) folderId = 0L;
        // 文件夹置顶，名称升序
        return fileRepository.findByUserIdAndParentIdOrderByFolderFirst(userId, folderId, pageable);
    }

    public Optional<FileObject> findOwned(Long userId, Long id) {
        return fileRepository.findByIdAndUserId(id, userId);
    }

    @Transactional
    public FileObject upload(Long userId, MultipartFile file, Long folderId) throws IOException {
        // 1. 自动检测文件信息
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        Long fileSize = file.getSize(); // 自动获取文件大小
        
        // 2. 生成存储文件名
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalFilename.substring(dot + 1);
        }
        String stored = UUID.randomUUID().toString().replace("-", "");
        if (!ext.isEmpty()) {
            stored = stored + "." + ext;
        }

        // 3. 创建用户专属目录结构
        LocalDate today = LocalDate.now();
        Path dir = Paths.get(baseDir, "user_" + userId, String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()), String.format("%02d", today.getDayOfMonth()));
        Files.createDirectories(dir);
        Path path = dir.resolve(stored);
        
        // 4. 保存文件到磁盘
        file.transferTo(path.toFile());

        // 5. 自动检测文件类型
        String contentType = getContentTypeFromExtension(ext);

        // 6. 创建文件对象（自动设置所有字段）
        FileObject fo = new FileObject();
        fo.setUserId(userId); // 使用userId关联用户
        fo.setFileName(originalFilename);
        fo.setStoredFileName(stored);
        fo.setFilePath(path.toString());
        fo.setParentId(folderId);
        fo.setFileSize(fileSize); // 自动检测的文件大小
        fo.setFileType(contentType); // 自动检测的文件类型
        // uploadedTime 会在 @PrePersist 中自动设置
        
        return fileRepository.save(fo);
    }

    /**
     * 创建文件夹
     */
    @Transactional
    public FileObject createFolder(Long userId, String folderName, Long parentId) {
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new RuntimeException("文件夹名称不能为空");
        }
        if (folderName.length() > 255) {
            throw new RuntimeException("文件夹名称长度不能超过255个字符");
        }
        // 不能包含 / \ : * ? " < > |
        if (folderName.matches(".*[\\/:*?\"<>|].*")) {
            throw new RuntimeException("文件夹名称包含非法字符");
        }
        if (parentId == null) parentId = 0L;
        if (parentId != 0L) {
            // 目标必须是一个文件夹
            fileRepository.findFolderByUserIdAndId(userId, parentId)
                    .orElseThrow(() -> new RuntimeException("父目录不存在或无权限"));
        }
        if (fileRepository.existsByUserIdAndParentIdAndFileName(userId, parentId, folderName)) {
            throw new RuntimeException("同级目录下已存在同名文件或文件夹");
        }

        FileObject folder = new FileObject();
        folder.setUserId(userId);
        folder.setFileName(folderName);
        folder.setParentId(parentId);
        folder.setIsFolder(true);
        folder.setFileType("folder");
        folder.setFileSize(0L);
        folder.setStoredFileName("");
        folder.setFilePath("");
        return fileRepository.save(folder);
    }

    /** 重命名文件或文件夹 */
    @Transactional
    public FileObject rename(Long userId, Long fileId, String newName) {
        FileObject fo = findOwned(userId, fileId).orElseThrow(() -> new RuntimeException("文件不存在或无权限"));
        if (newName == null || newName.trim().isEmpty()) {
            throw new RuntimeException("名称不能为空");
        }
        if (newName.length() > 255) {
            throw new RuntimeException("名称长度不能超过255个字符");
        }
        if (fileRepository.existsByUserIdAndParentIdAndFileNameAndIdNot(userId, fo.getParentId(), newName, fileId)) {
            throw new RuntimeException("同级目录下已存在同名文件或文件夹");
        }
        fo.setFileName(newName);
        return fileRepository.save(fo);
    }

    /** 移动文件或文件夹 */
    @Transactional
    public FileObject move(Long userId, Long fileId, Long newParentId) {
        FileObject fo = findOwned(userId, fileId).orElseThrow(() -> new RuntimeException("文件不存在或无权限"));
        if (newParentId == null) newParentId = 0L;
        if (newParentId != 0L) {
            FileObject target = fileRepository.findFolderByUserIdAndId(userId, newParentId)
                    .orElseThrow(() -> new RuntimeException("目标目录不存在或无权限"));
            if (fo.isFolder() && isDescendantOf(newParentId, fileId, userId)) {
                throw new RuntimeException("不能将文件夹移动到其子目录下");
            }
        }
        if (fileRepository.existsByUserIdAndParentIdAndFileNameAndIdNot(userId, newParentId, fo.getFileName(), fileId)) {
            throw new RuntimeException("目标目录下已存在同名文件或文件夹");
        }
        fo.setParentId(newParentId);
        return fileRepository.save(fo);
    }

    /** 判断ancestorId是否为descendantId的祖先（用于防循环） */
    private boolean isDescendantOf(Long ancestorId, Long descendantId, Long userId) {
        if (ancestorId.equals(descendantId)) return true;
        Optional<FileObject> ancestor = fileRepository.findFolderByUserIdAndId(userId, ancestorId);
        if (ancestor.isEmpty() || ancestor.get().getParentId() == 0L) return false;
        return isDescendantOf(ancestor.get().getParentId(), descendantId, userId);
    }

    /** 递归删除（文件夹/文件） */
    @Transactional
    public void deleteRecursively(FileObject fo) throws IOException {
        if (fo.isFolder()) {
            List<FileObject> children = fileRepository.findByUserIdAndParentId(fo.getUserId(), fo.getId());
            for (FileObject child : children) {
                deleteRecursively(child);
            }
        } else {
            if (fo.getFilePath() != null && !fo.getFilePath().isEmpty()) {
                Files.deleteIfExists(Paths.get(fo.getFilePath()));
            }
        }
        fileRepository.deleteById(fo.getId());
    }

    /** 获取文件夹路径（面包屑） */
    public List<FileObject> getFolderPath(Long userId, Long folderId) {
        List<FileObject> path = new ArrayList<>();
        Long currentId = folderId;
        while (currentId != null && currentId != 0L) {
            Optional<FileObject> folder = fileRepository.findFolderByUserIdAndId(userId, currentId);
            if (folder.isEmpty()) break;
            path.add(0, folder.get());
            currentId = folder.get().getParentId();
        }
        return path;
    }

    public Resource loadAsResource(FileObject fo) {
        return new FileSystemResource(fo.getFilePath());
    }

    @Transactional
    public void delete(FileObject fo) throws IOException {
        Files.deleteIfExists(Paths.get(fo.getFilePath()));
        fileRepository.deleteById(fo.getId());
    }

    public boolean isImage(FileObject fo) {
        String type = fo.getFileType();
        return type != null && type.toLowerCase().startsWith("image/");
    }

    /**
     * 根据文件扩展名获取MIME类型
     * @param extension 文件扩展名（不包含点号）
     * @return MIME类型
     */
    private String getContentTypeFromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        
        String ext = extension.toLowerCase();
        
        // 图片类型
        if (ext.matches("jpg|jpeg")) return "image/jpeg";
        if (ext.matches("png")) return "image/png";
        if (ext.matches("gif")) return "image/gif";
        if (ext.matches("bmp")) return "image/bmp";
        if (ext.matches("webp")) return "image/webp";
        if (ext.matches("svg")) return "image/svg+xml";
        if (ext.matches("ico")) return "image/x-icon";
        if (ext.matches("tiff|tif")) return "image/tiff";
        
        // 视频类型
        if (ext.matches("mp4")) return "video/mp4";
        if (ext.matches("avi")) return "video/x-msvideo";
        if (ext.matches("mov")) return "video/quicktime";
        if (ext.matches("wmv")) return "video/x-ms-wmv";
        if (ext.matches("flv")) return "video/x-flv";
        if (ext.matches("webm")) return "video/webm";
        if (ext.matches("mkv")) return "video/x-matroska";
        if (ext.matches("3gp")) return "video/3gpp";
        
        // 音频类型
        if (ext.matches("mp3")) return "audio/mpeg";
        if (ext.matches("wav")) return "audio/wav";
        if (ext.matches("flac")) return "audio/flac";
        if (ext.matches("aac")) return "audio/aac";
        if (ext.matches("ogg")) return "audio/ogg";
        if (ext.matches("wma")) return "audio/x-ms-wma";
        if (ext.matches("m4a")) return "audio/mp4";
        
        // 文档类型
        if (ext.matches("pdf")) return "application/pdf";
        if (ext.matches("doc")) return "application/msword";
        if (ext.matches("docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (ext.matches("xls")) return "application/vnd.ms-excel";
        if (ext.matches("xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (ext.matches("ppt")) return "application/vnd.ms-powerpoint";
        if (ext.matches("pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (ext.matches("txt")) return "text/plain";
        if (ext.matches("rtf")) return "application/rtf";
        
        // 压缩文件类型
        if (ext.matches("zip")) return "application/zip";
        if (ext.matches("rar")) return "application/vnd.rar";
        if (ext.matches("7z")) return "application/x-7z-compressed";
        if (ext.matches("tar")) return "application/x-tar";
        if (ext.matches("gz")) return "application/gzip";
        if (ext.matches("bz2")) return "application/x-bzip2";
        
        // 代码文件类型
        if (ext.matches("java")) return "text/x-java-source";
        if (ext.matches("js")) return "application/javascript";
        if (ext.matches("css")) return "text/css";
        if (ext.matches("html|htm")) return "text/html";
        if (ext.matches("xml")) return "application/xml";
        if (ext.matches("json")) return "application/json";
        if (ext.matches("yaml|yml")) return "application/x-yaml";
        if (ext.matches("sql")) return "application/sql";
        if (ext.matches("py")) return "text/x-python";
        if (ext.matches("cpp|c\\+\\+")) return "text/x-c++";
        if (ext.matches("c")) return "text/x-c";
        if (ext.matches("php")) return "application/x-httpd-php";
        
        // 其他常见类型
        if (ext.matches("exe")) return "application/x-msdownload";
        if (ext.matches("msi")) return "application/x-msdownload";
        if (ext.matches("dmg")) return "application/x-apple-diskimage";
        if (ext.matches("iso")) return "application/x-iso9660-image";
        if (ext.matches("apk")) return "application/vnd.android.package-archive";
        
        // 默认返回二进制流
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}


