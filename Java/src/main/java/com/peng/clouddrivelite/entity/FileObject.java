package com.peng.clouddrivelite.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "file_object")
public class FileObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 文件的唯一id

    @Column(name = "parent_id")
    private Long parentId = 0L;   // 0 表示根目录

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "文件名不能为空")
    @Size(min = 1, max = 255, message = "文件名长度必须在1-255个字符之间")
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName; // 原始文件名

    // 文件夹可为空
    @Size(max = 255, message = "存储文件名长度不能超过255个字符")
    @Column(name = "stored_file_name", length = 255)
    private String storedFileName; // 落盘名（如 uuid.ext）

    // 文件夹可为空
    @Size(max = 1024, message = "文件路径长度不能超过1024个字符")
    @Column(name = "file_path", length = 1024)
    private String filePath; // 绝对或相对路径

    @PositiveOrZero(message = "文件大小不能为负数")
    @Column(name = "file_size")
    private Long fileSize = 0L; // 字节数

    @Size(max = 128, message = "文件类型长度不能超过128个字符")
    @Column(name = "file_type", length = 128)
    private String fileType; // MIME 类型

    // 是否为文件夹
    @Column(name = "is_folder", nullable = false)
    private Boolean isFolder = false;

    @PastOrPresent(message = "上传时间不能晚于当前时间")
    @Column(name = "upload_time", updatable = false)
    private LocalDateTime uploadedTime;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    public FileObject() {
    }

    public FileObject(String fileName, Long id, Long userId, String storedFileName, String filePath, Long fileSize, String fileType, LocalDateTime uploadedTime) {
        this.fileName = fileName;
        this.id = id;
        this.userId = userId;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.uploadedTime = uploadedTime;
    }

    @PrePersist
    protected void onCreate() {
        if (uploadedTime == null) {
            uploadedTime = LocalDateTime.now();
        }
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
        if (Boolean.TRUE.equals(isFolder)) {
            // 规范化文件夹默认属性
            this.fileType = "folder";
            this.fileSize = 0L;
            if (this.storedFileName == null) this.storedFileName = "";
            if (this.filePath == null) this.filePath = "";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    public String filePath() {
        return filePath;
    }

    public FileObject setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public Long id() {
        return id;
    }

    public FileObject setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getParentId() {
        return parentId;
    }

    public FileObject setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public @NotNull(message = "用户ID不能为空") Long userId() {
        return userId;
    }

    public FileObject setUserId(@NotNull(message = "用户ID不能为空") Long userId) {
        this.userId = userId;
        return this;
    }

    public @NotBlank(message = "文件名不能为空") @Size(min = 1, max = 255, message = "文件名长度必须在1-255个字符之间") String fileName() {
        return fileName;
    }

    public FileObject setFileName(@NotBlank(message = "文件名不能为空") @Size(min = 1, max = 255, message = "文件名长度必须在1-255个字符之间") String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String storedFileName() {
        return storedFileName;
    }

    public FileObject setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
        return this;
    }

    public Long fileSize() {
        return fileSize;
    }

    public FileObject setFileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public String fileType() {
        return fileType;
    }

    public FileObject setFileType(String fileType) {
        this.fileType = fileType;
        return this;
    }

    public LocalDateTime uploadedTime() {
        return uploadedTime;
    }

    public FileObject setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
        return this;
    }

    public Boolean getIsFolder() {
        return isFolder;
    }

    public void setIsFolder(Boolean folder) {
        isFolder = folder;
    }

    public boolean isFolder() {
        return Boolean.TRUE.equals(isFolder);
    }

    public boolean isFile() {
        return !isFolder();
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    @Override
    public String toString() {
        return "FileObject{" +
                "id=" + id +
                ", userId=" + userId +
                ", fileName='" + fileName + '\'' +
                ", storedFileName='" + storedFileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                ", uploadedTime=" + uploadedTime +
                '}';
    }

    // Standard getters for external usage (Controller/Service)
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }
}
