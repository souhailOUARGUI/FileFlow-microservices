package org.example.folderservice.repository;

import org.example.folderservice.entity.FolderShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderShareRepository extends JpaRepository<FolderShare, Long> {
    
    List<FolderShare> findByFolderId(Long folderId);
    
    List<FolderShare> findByOwnerId(Long ownerId);
    
    List<FolderShare> findByTargetUserId(Long targetUserId);
    
    @Query("SELECT fs FROM FolderShare fs WHERE fs.targetUserId = :userId AND fs.status = 'pending'")
    List<FolderShare> findPendingSharesForUser(@Param("userId") Long userId);
    
    @Query("SELECT fs FROM FolderShare fs WHERE fs.ownerId = :userId")
    List<FolderShare> findSharesCreatedByUser(@Param("userId") Long userId);
    
    @Query("SELECT fs FROM FolderShare fs WHERE fs.folder.id = :folderId AND fs.targetUserId = :userId")
    Optional<FolderShare> findByFolderIdAndTargetUserId(@Param("folderId") Long folderId, @Param("userId") Long userId);
    
    @Query("SELECT fs FROM FolderShare fs WHERE fs.folder.id = :folderId AND fs.status = 'accepted'")
    List<FolderShare> findAcceptedSharesForFolder(@Param("folderId") Long folderId);
    
    void deleteByFolderIdAndTargetUserId(Long folderId, Long targetUserId);
}
