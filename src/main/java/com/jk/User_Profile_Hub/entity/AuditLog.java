package com.jk.User_Profile_Hub.entity;

import com.jk.User_Profile_Hub.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_user_id",    columnList = "user_id"),
                @Index(name = "idx_audit_file_id",    columnList = "file_id"),
                @Index(name = "idx_audit_action",     columnList = "action"),
                @Index(name = "idx_audit_acted_at",   columnList = "acted_at"),
                @Index(name = "idx_audit_actor_id",   columnList = "actor_id"),
                @Index(name = "idx_audit_user_action",columnList = "user_id, action")  // composite for admin queries
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============ Subject ============

    /**
     * The user whose data was affected.
     * Stored as a plain Long (not FK) so the log survives even if the User row is deleted.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The file involved in the action (nullable — some actions are profile-level).
     * Stored as a plain Long for the same durability reason as userId.
     */
    @Column(name = "file_id")
    private Long fileId;

    /**
     * Public UUID of the file at the time of the action.
     * Kept here so admin can reference the file even after the FileMetadata row is gone.
     */
    @Column(name = "file_uuid", length = 36)
    private String fileUuid;

    // ============ Actor ============

    /**
     * The user who performed the action.
     * For user-initiated actions: actorId == userId.
     * For admin actions: actorId != userId.
     */
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    /**
     * Role of the actor at the time of the action.
     * Snapshot — not linked to the live User.role in case it changes later.
     */
    @Column(name = "actor_role", nullable = false, length = 10)
    private String actorRole;

    // ============ Action ============

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    // ============ Request Context ============

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // ============ Timestamp ============

    @CreationTimestamp
    @Column(name = "acted_at", nullable = false, updatable = false)
    private LocalDateTime actedAt;

    // ============================================
    // Factory Methods
    // ============================================

    public static AuditLog forFile(Long userId, Long actorId, String actorRole, AuditAction action,
                                   FileMetadata file, String ipAddress, String userAgent) {
        return AuditLog.builder()
                .userId(userId)
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .fileId(file.getId())
                .fileUuid(file.getUuid())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    public static AuditLog forProfile(Long userId, Long actorId, String actorRole, AuditAction action,
                                      String detail, String ipAddress, String userAgent) {
        return AuditLog.builder()
                .userId(userId)
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Returns true if the action was performed by an admin on another user's data.
     */
    public boolean isAdminAction() {
        return !this.actorId.equals(this.userId);
    }
}
