package com.peng.clouddrivelite.repository;

import com.peng.clouddrivelite.entity.FileObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileObject, Long>, JpaSpecificationExecutor<FileObject> {
    /**
     * 根据用户ID分页查询该用户上传的所有文件
     * 按上传时间倒序（最新上传在前）
     *
     * @param userId   用户主键
     * @param pageable 分页参数（页码、每页条数、排序规则）
     * @return 分页结果集
     */
    Page<FileObject> findByUserIdOrderByUploadedTimeDesc(Long userId, Pageable pageable);

    /**
     * 按用户 + 父文件夹分页列出
     */
    Page<FileObject> findByUserIdAndParentIdOrderByUploadedTimeDesc(Long userId, Long parentId, Pageable pageable);

    /**
     * 根据文件主键 + 用户ID 联合查询唯一文件记录
     * 用于校验该文件是否属于指定用户
     *
     * @param id     文件主键
     * @param userId 用户主键
     * @return 匹配的文件对象（可能为空）
     */
    Optional<FileObject> findByIdAndUserId(Long id, Long userId);

    /**
     * 判断指定用户下是否已存在某个存储文件名
     * 通常用于避免重复上传同名文件（物理文件名）
     *
     * @param userId          用户主键
     * @param storedFileName  存储在服务器上的文件名（如UUID重命名后的名称）
     * @return true=已存在
     */
    boolean existsByUserIdAndStoredFileName(Long userId, String storedFileName);

    /**
     * 统计指定用户上传的文件总数
     *
     * @param userId 用户主键
     * @return 文件数量
     */
    long countByUserId(Long userId);
    
    // ====== 新增：文件夹/重名校验/层级查询 ======
    @Query("SELECT f FROM FileObject f WHERE f.userId = :userId AND f.parentId = :parentId ORDER BY (case when lower(f.fileType) = 'folder' then 0 else 1 end), f.fileName ASC")
    Page<FileObject> findByUserIdAndParentIdOrderByFolderFirst(@Param("userId") Long userId,
                                                               @Param("parentId") Long parentId,
                                                               Pageable pageable);

    boolean existsByUserIdAndParentIdAndFileNameAndIdNot(Long userId, Long parentId, String fileName, Long excludeId);

    boolean existsByUserIdAndParentIdAndFileName(Long userId, Long parentId, String fileName);

    List<FileObject> findByUserIdAndParentId(Long userId, Long parentId);

    @Query("SELECT f FROM FileObject f WHERE f.userId = :userId AND f.id = :folderId AND lower(f.fileType) = 'folder'")
    Optional<FileObject> findFolderByUserIdAndId(@Param("userId") Long userId, @Param("folderId") Long folderId);

    /**
     * 搜索文件（按文件名模糊匹配，忽略大小写）
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 搜索结果分页
     */
    Page<FileObject> findByUserIdAndFileNameContainingIgnoreCaseOrderByUploadedTimeDesc(Long userId, String keyword, Pageable pageable);

}


