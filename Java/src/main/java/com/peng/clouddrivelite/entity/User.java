package com.peng.clouddrivelite.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;


import java.time.LocalDateTime;


@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "user_number"),
                @UniqueConstraint(columnNames = "phone_number")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Column(name = "username", nullable = false, length = 20)
    private String username;

    @NotBlank(message = "用户号码不能为空")
    @Size(min = 6, max = 20, message = "用户号码长度必须在6-20个字符之间")
    @Column(name = "user_number", nullable = false, length = 20)
    private String userNumber;

    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "电话号码格式不正确")
    @Column(name = "phone_number", nullable = false, length = 11)
    private String phoneNumber;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    // 修改为驼峰命名法
    @Column(name = "file_dir", nullable = false, length = 255)
    private String fileDir;

    // 添加状态字段
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 1-正常, 0-禁用

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 无参构造函数
    public User() {
    }

    // 用于创建用户的构造函数（不包含id和日期字段）
    public User(String username, String userNumber, String phoneNumber,
                String password, String email) {
        this.username = username;
        this.userNumber = userNumber;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.email = email;
    }

    // 生命周期回调方法
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 自动生成文件目录
        if (fileDir == null) {
            fileDir = "user_" + userNumber + "_" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserNumber(String userNumber) {
        this.userNumber = userNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFileDir(String fileDir) {
        this.fileDir = fileDir;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getUserNumber() {
        return userNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getFileDir() {
        return fileDir;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", userNumber='" + userNumber + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", fileDir='" + fileDir + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}