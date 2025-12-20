package com.peng.clouddrivelite.service;

import com.peng.clouddrivelite.entity.FileObject;
import com.peng.clouddrivelite.entity.User;
import com.peng.clouddrivelite.repository.FileRepository;
import com.peng.clouddrivelite.repository.UserRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.peng.clouddrivelite.util.FileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
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
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    /**
     * 文件存储根目录。
     * <p>
     * 默认值：{@code E:/CloudDriveLite/storage}（可通过配置项 {@code storage.base-dir} 覆盖）。
     * </p>
     *
     * <h3>目录结构约定</h3>
     * <pre>
     * {baseDir}/user_{userId}/YYYY/MM/DD/{storedFileName}    # 最终文件落盘位置（普通上传/分片合并后）
     * {baseDir}/chunks/user_{userId}/{identifier}/{chunkNo}  # 分片临时目录（断点续传与合并使用）
     * </pre>
     */
    @Value("${storage.base-dir:E:/CloudDriveLite/storage}")
    private String baseDir;

    public FileService(FileRepository fileRepository, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    public Page<FileObject> list(Long userId, Long folderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (folderId == null) folderId = 0L;
        // 文件夹置顶，名称升序
        return fileRepository.findByUserIdAndParentIdOrderByFolderFirst(userId, folderId, pageable);
    }

    /**
     * 搜索文件（按文件名模糊匹配）
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果分页
     */
    public Page<FileObject> search(Long userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fileRepository.findByUserIdAndFileNameContainingIgnoreCaseOrderByUploadedTimeDesc(userId, keyword, pageable);
    }

    public Optional<FileObject> findOwned(Long userId, Long id) {
        return fileRepository.findByIdAndUserId(id, userId);
    }

    /**
     * 验证上传的文件
     *
     * @param file 上传的文件
     * @throws RuntimeException 如果文件验证失败
     */
    public void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 检查文件名
        String originalFilename = file.getOriginalFilename();
        FileUtils.validateFilename(originalFilename);

        // 检查文件扩展名
        String extension = FileUtils.getFileExtension(originalFilename);
        FileUtils.validateExtension(extension);
    }

    /**
     * 计算用户已用存储空间
     */
    public long calculateUserStorage(Long userId) {
        return fileRepository.findAll().stream()
                .filter(f -> f.getUserId().equals(userId) && !f.getIsFolder())
                .mapToLong(FileObject::getFileSize)
                .sum();
    }

    /**
     * 普通文件上传（非分片场景）。
     * <p>
     * 该方法直接把 MultipartFile 落盘到最终目录，并写入一条 {@link FileObject} 记录。
     * </p>
     *
     * <h3>落盘路径</h3>
     * <pre>
     * {baseDir}/user_{userId}/YYYY/MM/DD/{uuid.ext}
     * </pre>
     *
     * <h3>入库字段</h3>
     * - fileName: 原始文件名（展示用）
     * - storedFileName: uuid 重命名后的文件名（避免冲突）
     * - filePath: 实际磁盘路径（当前实现保存的是绝对路径字符串）
     * - fileType: 由扩展名推断的 MIME
     * - fileSize: 文件大小（字节）
     */
    @Transactional
    public FileObject upload(Long userId, MultipartFile file, Long folderId) throws IOException {
        // 0. 检查存储空间限制
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        long usedStorage = calculateUserStorage(userId);
        long fileSize = file.getSize();
        if (usedStorage + fileSize > user.getMaxStorage()) {
            long remaining = user.getMaxStorage() - usedStorage;
            throw new RuntimeException(String.format(
                    "存储空间不足！已用：%.2f GB，限制：%.2f GB，剩余：%.2f GB，文件大小：%.2f GB",
                    usedStorage / 1024.0 / 1024.0 / 1024.0,
                    user.getMaxStorage() / 1024.0 / 1024.0 / 1024.0,
                    remaining / 1024.0 / 1024.0 / 1024.0,
                    fileSize / 1024.0 / 1024.0 / 1024.0
            ));
        }

        // 1. 自动检测文件信息
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        
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
        
        FileObject saved = fileRepository.save(fo);
        
        // 再次检查存储空间（防止并发问题）
        long newUsedStorage = calculateUserStorage(userId);
        if (newUsedStorage > user.getMaxStorage()) {
            // 回滚：删除已保存的文件记录和物理文件
            fileRepository.deleteById(saved.getId());
            Files.deleteIfExists(Paths.get(saved.getFilePath()));
            throw new RuntimeException("存储空间检查失败，上传已取消");
        }
        
        return saved;
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

    /**
     * 将已上传文件包装为 Spring 的 {@link Resource}。
     * <p>
     * Controller 的下载/预览接口会直接返回这个 Resource 给浏览器。
     * </p>
     *
     * <p>
     * 注意：这里不做权限校验，权限校验应在 Controller/调用方通过 {@code findOwned(...)} 完成。
     * </p>
     */
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

    /**
     * 将文件复制到 RawBox 数据目录
     * @param fo 文件对象
     * @return RawBox 中的文件名
     * @throws IOException 复制失败时抛出异常
     */
    public String copyToRawBox(FileObject fo) throws IOException {
        if (fo.isFolder()) {
            throw new IllegalArgumentException("文件夹不能复制到 RawBox");
        }
        
        // RawBox 数据目录路径 - 使用相对路径
        String rawboxDir = "rawbox-data/public";
        Path rawboxPath = Paths.get(rawboxDir);
        
        // 确保目录存在
        Files.createDirectories(rawboxPath);
        
        // 生成唯一的文件名，避免冲突
        String originalName = fo.getFileName();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }
        
        String rawboxFileName = "file_" + fo.getId() + "_" + System.currentTimeMillis() + extension;
        Path targetPath = rawboxPath.resolve(rawboxFileName);
        
        // 复制文件
        Files.copy(Paths.get(fo.getFilePath()), targetPath);
        log.info("copied to RawBox: {}", targetPath);
        return rawboxFileName;
    }

    // -------------- 分片上传支持 --------------

    /**
     * 获取某个文件（identifier）的分片临时目录。
     * <p>
     * 分片临时目录用于断点续传与合并。
     * </p>
     *
     * <pre>
     * {baseDir}/chunks/user_{userId}/{identifier}/
     * </pre>
     */
    private Path getChunkDir(Long userId, String identifier) throws IOException {
        Path dir = Paths.get(baseDir, "chunks", "user_" + userId, identifier);
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * 分片探测：判断某个分片文件是否已存在。
     * <p>
     * 配合 Controller 的 GET /api/files/upload 使用，让前端能够在重试/续传时跳过已上传的分片。
     * </p>
     */
    public boolean chunkExists(Long userId, String identifier, int chunkNumber) {
        try {
            Path chunkPath = Paths.get(baseDir, "chunks", "user_" + userId, identifier, String.valueOf(chunkNumber));
            return Files.exists(chunkPath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 保存单个分片到临时目录。
     * <p>
     * 分片文件名直接使用分片序号（例如：1、2、3...），合并时按序号顺序拼接。
     * </p>
     */
    public void saveChunk(Long userId, String identifier, int chunkNumber, MultipartFile file) throws IOException {
        Path dir = getChunkDir(userId, identifier);
        Path chunkPath = dir.resolve(String.valueOf(chunkNumber));
        try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(chunkPath)) {
            in.transferTo(out);
        }
    }

    /**
     * 尝试合并某个 identifier 的所有分片。
     * <p>
     * 该方法会：
     * 1) 检查 1..totalChunks 是否都已上传；若缺少则返回 empty
     * 2) 存储空间配额检查
     * 3) 创建最终落盘文件（与普通上传一致的 user/date 目录）
     * 4) 按分片序号从小到大顺序读取，并写入同一个 OutputStream，实现“拼接”
     * 5) 清理分片目录
     * 6) 写入 FileObject 元数据并返回
     * </p>
     *
     * <p>
     * 这里返回 Optional 的原因是：上传过程中大多数请求只是上传某一片，
     * 只有当所有分片都齐时才会真正产生最终文件与数据库记录。
     * </p>
     */
    @Transactional
    public Optional<FileObject> tryMergeChunks(Long userId,
                                               String identifier,
                                               int totalChunks,
                                               String originalFilename,
                                               long totalSize,
                                               Long folderId) throws IOException {
        // 分片临时目录：baseDir/chunks/user_{userId}/{identifier}
        // identifier 一般是文件内容哈希或前端生成的唯一标识，用于把同一文件的分片归到同一目录。
        Path dir = Paths.get(baseDir, "chunks", "user_" + userId, identifier);
        // 如果分片目录都不存在，说明分片还没开始上传/已被清理，直接返回 empty。
        if (!Files.exists(dir)) return Optional.empty();

        // 检查是否所有切片已就绪：要求 1..totalChunks 每个分片文件都存在。
        for (int i = 1; i <= totalChunks; i++) {
            // 每个分片的文件名就是分片序号（saveChunk 里也是这么保存的）
            if (!Files.exists(dir.resolve(String.valueOf(i)))) {
                // 只要缺任何一片，就暂时不能合并：返回 empty，让调用方继续上传剩余分片。
                return Optional.empty();
            }
        }

        // 检查存储空间限制：合并之前先做一次配额校验，避免浪费 IO 合并出文件后再失败。
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 计算用户已使用的存储（通常来自数据库记录/遍历文件记录求和）
        long usedStorage = calculateUserStorage(userId);
        // usedStorage + 当前文件总大小 不能超过用户配额
        if (usedStorage + totalSize > user.getMaxStorage()) {
            // 还剩余多少空间
            long remaining = user.getMaxStorage() - usedStorage;
            // 抛出异常：由于方法标记 @Transactional，抛异常会让数据库操作回滚（如后续有入库）。
            throw new RuntimeException(String.format(
                    "存储空间不足！已用：%.2f GB，限制：%.2f GB，剩余：%.2f GB，文件大小：%.2f GB",
                    usedStorage / 1024.0 / 1024.0 / 1024.0,
                    user.getMaxStorage() / 1024.0 / 1024.0 / 1024.0,
                    remaining / 1024.0 / 1024.0 / 1024.0,
                    totalSize / 1024.0 / 1024.0 / 1024.0
            ));
        }

        // 目标存储路径（与普通上传一致的用户/日期目录）：最终合并的文件会存到正式目录中。
        // 下面开始生成“最终落盘文件名 stored”，避免与原始文件名冲突。
        String ext = "";
        // 从原始文件名提取扩展名（用于 contentType 推断 / 方便下载时保留类型）
        int dot = originalFilename.lastIndexOf('.');
        // 若存在后缀，则 ext=后缀（不带点）
        if (dot >= 0) ext = originalFilename.substring(dot + 1);
        // 生成随机文件名（不带 -），防止同名覆盖
        String stored = UUID.randomUUID().toString().replace("-", "");
        // 若原文件有扩展名，则拼回扩展名，便于后续识别类型/下载
        if (!ext.isEmpty()) stored = stored + "." + ext;

        // 以日期分目录：year/MM/dd
        LocalDate today = LocalDate.now();
        // 最终目录：baseDir/user_{userId}/yyyy/MM/dd
        Path targetDir = Paths.get(baseDir, "user_" + userId, String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()), String.format("%02d", today.getDayOfMonth()));
        // 确保目标目录存在
        Files.createDirectories(targetDir);
        // 最终文件完整路径
        Path target = targetDir.resolve(stored);

        // 合并写入：核心逻辑就是“按顺序把每个分片追加写入同一个目标文件”
        // 这里使用 try-with-resources，确保输出流最终会被关闭，避免文件句柄泄露。
        try (OutputStream out = Files.newOutputStream(target)) {
            // 按分片序号从小到大写入，确保与前端切片顺序一致
            for (int i = 1; i <= totalChunks; i++) {
                // 当前分片路径：dir/{i}
                Path chunkPath = dir.resolve(String.valueOf(i));
                // 打开该分片输入流
                try (InputStream in = Files.newInputStream(chunkPath)) {
                    // Java 9+：把输入流内容直接传输到输出流，相当于循环读写 buffer
                    in.transferTo(out);
                }
            }
        }

        // 校验总大小（可选）
        // 合并后实际落盘文件大小
        long realSize = Files.size(target);
        // 若前端/调用方提供了 totalSize，则可用于做一致性校验
        if (totalSize > 0 && realSize != totalSize) {
            // 出于安全考虑，大小不一致也允许，但记录；实际可抛异常并回滚
            log.warn("merged size mismatch, real={}, reported={}", realSize, totalSize);
        }

        // 清理分片目录：合并完成后，分片临时文件就没有继续保留的价值了。
        // 注意：如果这里删除失败（例如被占用/权限问题），可能会遗留垃圾分片；
        // 但不会影响已经合并出的最终文件。
        for (int i = 1; i <= totalChunks; i++) {
            // 删除单个分片文件（忽略不存在的情况）
            Files.deleteIfExists(dir.resolve(String.valueOf(i)));
        }
        // 尝试删除分片目录（只有当目录为空时才会成功）
        Files.deleteIfExists(dir);

        // 入库
        // 通过扩展名推断内容类型（如 image/png、video/mp4 等）
        String contentType = getContentTypeFromExtension(ext);
        // 组装数据库实体：保存原始文件名、存储文件名、物理路径、大小、类型等
        FileObject fo = new FileObject();
        // 归属用户
        fo.setUserId(userId);
        // 原始文件名（用于展示/下载时显示）
        fo.setFileName(originalFilename);
        // 实际落盘文件名（避免冲突）
        fo.setStoredFileName(stored);
        // 物理路径（用于后续下载/预览定位文件）
        fo.setFilePath(target.toString());
        // 目标父文件夹：若 folderId 为空则放到根目录(0)
        fo.setParentId(folderId != null ? folderId : 0L);
        // 文件大小：以合并后真实大小为准
        fo.setFileSize(realSize);
        // 文件类型：用于预览/前端展示
        fo.setFileType(contentType);
        // 写入数据库并拿到保存后的实体（含 id）
        FileObject saved = fileRepository.save(fo);
        
        // 再次检查存储空间（防止并发问题）
        // 上面第一次检查是“提前失败”；这里再次检查是为了兜底并发：
        // 例如同时上传多个文件，可能在第一次检查后又写入了新的文件记录，导致超配额。
        long newUsedStorage = calculateUserStorage(userId);
        if (newUsedStorage > user.getMaxStorage()) {
            // 回滚：删除已保存的文件记录和物理文件
            fileRepository.deleteById(saved.getId());
            Files.deleteIfExists(Paths.get(saved.getFilePath()));
            throw new RuntimeException("存储空间检查失败，上传已取消");
        }
        
        // 返回合并成功后的文件对象
        return Optional.of(saved);
    }
}


