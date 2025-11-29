package com.peng.clouddrivelite.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 简单的密码加密工具
 * 使用 SHA-256 哈希算法
 */
public final class PasswordUtil {
    
    private PasswordUtil() {}

    /**
     * 对密码进行哈希加密
     * @param raw 原始密码
     * @return 加密后的密码（十六进制字符串）
     */
    public static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 验证密码是否匹配
     * @param raw 原始密码
     * @param hashed 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String raw, String hashed) {
        String hashedRaw = hash(raw);
        return hashedRaw.equals(hashed);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}


