package org.example.fileservice.repository;

import org.example.fileservice.entity.File;
import org.example.fileservice.entity.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    @Query("SELECT fs.targetUserId FROM FileShare fs WHERE fs.file.id = :fileId")
    List<Long> getSharedUserIdsByFileId(@Param("fileId") Long fileId);

    @Query("SELECT fs.file FROM FileShare fs WHERE fs.targetUserId = :targetId AND fs.response = true")
    List<File> findByTargetId(@Param("targetId") Long targetId);

    @Query("SELECT fs FROM FileShare fs WHERE fs.targetUserId = :targetUserId AND fs.response = false")
    List<FileShare> findFileSharesByTargetUserId(@Param("targetUserId") Long targetUserId);

    @Query("SELECT DISTINCT fs.file FROM FileShare fs WHERE fs.file.userId = :userId")
    List<File> findSharedFilesByMe(@Param("userId") Long userId);

    @Query("SELECT fs FROM FileShare fs WHERE fs.file.id = :fileId AND fs.targetUserId = :targetUserId")
    Optional<FileShare> findByFileIdAndTargetUserId(@Param("fileId") Long fileId, @Param("targetUserId") Long targetUserId);

    boolean existsByFileIdAndTargetUserId(Long fileId, Long targetUserId);

    List<FileShare> findByTargetUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT fs FROM FileShare fs WHERE fs.file.userId = :userId AND fs.shareType = :shareType")
    List<FileShare> findByFileUserIdAndShareType(@Param("userId") Long userId, @Param("shareType") String shareType);

    @Query("SELECT fs FROM FileShare fs WHERE fs.file.id = :fileId AND fs.targetUserId = :targetUserId AND fs.file.userId = :ownerId")
    Optional<FileShare> findByFileIdAndTargetUserIdAndFileUserId(@Param("fileId") Long fileId, 
                                                                   @Param("targetUserId") Long targetUserId, 
                                                                   @Param("ownerId") Long ownerId);

    @Query("SELECT fs FROM FileShare fs WHERE fs.file.id = :fileId AND fs.file.userId = :userId AND fs.isActive = true")
    List<FileShare> findActiveSharesByFileAndUser(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Query("SELECT fs FROM FileShare fs WHERE fs.shareToken = :shareToken AND fs.isActive = true AND (fs.expiresAt IS NULL OR fs.expiresAt > :currentTime)")
    Optional<FileShare> findActiveShareByToken(@Param("shareToken") String shareToken, @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query("UPDATE FileShare fs SET fs.accessCount = fs.accessCount + 1 WHERE fs.id = :shareId")
    void incrementAccessCount(@Param("shareId") Long shareId);

    @Query("SELECT fs FROM FileShare fs WHERE fs.file.userId = :userId ORDER BY fs.createdAt DESC")
    List<FileShare> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
