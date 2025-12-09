package org.example.folderservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Folder entity for microservices architecture.
 * Note: User relationship is replaced with userId (Long) for service decoupling.
 * User information is fetched via Feign client when needed.
 */
@Entity
@Table(name = "folders")
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class Folder {
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "path", nullable = false)
    private String path;

    /**
     * Reference to user in user-service (microservices pattern)
     * Instead of @ManyToOne User user
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonBackReference
    private Folder parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Folder> subfolders = new ArrayList<>();

    /**
     * File count - managed via file-service calls
     * Files are now in a separate microservice
     */
    @Column(name = "file_count")
    private Integer fileCount = 0;

    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite = false;

    @Column(name = "color")
    private String color;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public String getFullPath() {
        if (parent == null) {
            return "/" + name;
        }
        return parent.getFullPath() + "/" + name;
    }

    public int getSubfolderCount() {
        return subfolders != null ? subfolders.size() : 0;
    }

    /**
     * Get total size - requires file-service call in actual implementation
     * This is a placeholder that returns 0. Actual size calculation
     * should be done in the service layer via Feign client.
     */
    public long getTotalSize() {
        // In microservices, this would be calculated via file-service
        return 0L;
    }
}
