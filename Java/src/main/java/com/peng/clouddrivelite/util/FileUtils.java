package com.peng.clouddrivelite.util;

/**
 * 文件工具类
 */
public class FileUtils {

    // 允许的文件类型
    private static final String[] ALLOWED_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif",
            "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv", "3gp",
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf",
            "zip", "rar", "7z", "tar", "gz", "bz2",
            "java", "js", "css", "html", "htm", "xml", "json", "yaml", "yml", "sql", "py", "cpp", "c", "php",
            "exe", "msi", "dmg", "iso", "apk"
    };

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（小写，不包含点号）
     */
    public static String getFileExtension(String filename) {
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
    public static boolean isAllowedExtension(String extension) {
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

    /**
     * 格式化文件大小显示
     *
     * @param bytes 字节数
     * @return 格式化后的大小字符串
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 验证文件名
     *
     * @param filename 文件名
     * @throws RuntimeException 如果文件名无效
     */
    public static void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
    }

    /**
     * 验证文件扩展名
     *
     * @param extension 扩展名
     * @throws RuntimeException 如果扩展名不被允许
     */
    public static void validateExtension(String extension) {
        if (extension != null && !isAllowedExtension(extension)) {
            throw new RuntimeException("不支持的文件类型: " + extension);
        }
    }
}



