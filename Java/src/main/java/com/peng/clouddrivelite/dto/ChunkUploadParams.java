package com.peng.clouddrivelite.dto;

/**
 * 分片上传参数
 */
public class ChunkUploadParams {
    private String identifier;
    private Integer chunkNumber;
    private Integer totalChunks;
    private Long chunkSize;
    private Long totalSize;
    private String filename;

    public ChunkUploadParams() {
    }

    public ChunkUploadParams(String identifier, Integer chunkNumber, Integer totalChunks,
                             Long chunkSize, Long totalSize, String filename) {
        this.identifier = identifier;
        this.chunkNumber = chunkNumber;
        this.totalChunks = totalChunks;
        this.chunkSize = chunkSize;
        this.totalSize = totalSize;
        this.filename = filename;
    }

    /**
     * 规范化参数，填充缺失的值
     *
     * @param originalFilename 原始文件名
     * @param fileSize 文件大小
     * @param userId 用户ID
     * @return 规范化后的参数对象
     */
    public ChunkUploadParams normalize(String originalFilename, long fileSize, Long userId) {
        if (filename == null || filename.isBlank()) {
            filename = originalFilename;
        }
        if (totalSize == null || totalSize <= 0) {
            totalSize = fileSize;
        }
        if (totalChunks == null || totalChunks <= 0) {
            totalChunks = 1;
        }
        if (chunkNumber == null || chunkNumber <= 0) {
            chunkNumber = 1;
        }
        if (identifier == null || identifier.isBlank()) {
            identifier = (filename + "_" + totalSize + "_" + userId).replaceAll("[^a-zA-Z0-9_-]", "");
        }
        return this;
    }

    // Getters and Setters
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Integer getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(Integer chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}



