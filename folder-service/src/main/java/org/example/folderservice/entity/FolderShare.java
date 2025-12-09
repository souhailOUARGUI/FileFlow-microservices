package org.example.folderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * FolderShare entity for microservices architecture.
 * Note: User relationships are replaced with userId fields for service decoupling.
 * User information is fetched via Feign client when needed.
 */
@Entity
@Table(name = "folder_shares")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    /**
     * Reference to owner user in user-service (microservices pattern)
     * Instead of @ManyToOne User owner
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * Reference to target user in user-service (microservices pattern)
     * Instead of @ManyToOne User targetUser
     */
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(nullable = false)
    private String permissions = "read"; // "read", "write", "admin"

    @Column(length = 500)
    private String message;

    @CreationTimestamp
    @Column(name = "shared_at")
    private LocalDateTime sharedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private String status = "pending"; // "pending", "accepted", "rejected", "revoked"

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "requires_password")
    private boolean requiresPassword = false;

    @Column(name = "requires_approval")
    private boolean requiresApproval = true;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
