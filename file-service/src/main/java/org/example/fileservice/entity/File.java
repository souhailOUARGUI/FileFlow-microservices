package org.example.fileservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "file",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id", "original_file_id"}))
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class File {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String originalFileName;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(nullable = false)
    private String contentType;

    private boolean isShared;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private String fileUuid;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "original_file_id")
    @JsonIgnore
    private File originalFile;

    @OneToMany(mappedBy = "originalFile", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<File> fileCopies;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<FileShare> fileShares;

    // User ID reference (no direct entity relationship in microservices)
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // Folder ID reference (optional)
    @Column(name = "folder_id")
    private Long folderId;

    public String getFileExtension() {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return "";
    }
}
