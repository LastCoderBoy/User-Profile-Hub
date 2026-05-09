package com.jk.User_Profile_Hub.dto.response;

import com.jk.User_Profile_Hub.entity.AuditLog;
import com.jk.User_Profile_Hub.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private Long actorId;
    private String actorRole;
    private AuditAction action;
    private String fileUuid;

    /**
     * IP address is included in the admin view for security investigation.
     * Must NOT be included in user-facing audit views (privacy).
     */
    private String ipAddress;

    private LocalDateTime actedAt;
    private boolean adminAction;

    // ============================================
    // Factory Method
    // ============================================

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .actorId(log.getActorId())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .fileUuid(log.getFileUuid())
                .ipAddress(log.getIpAddress())
                .actedAt(log.getActedAt())
                .adminAction(log.isAdminAction())
                .build();
    }
}
