package com.jk.User_Profile_Hub.entity;

import com.jk.User_Profile_Hub.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_uuid", columnList = "uuid"),
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_created_at", columnList = "created_at"),
                @Index(name = "idx_user_deleted_at", columnList = "deleted_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_uuid",  columnNames = "uuid"),
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing identifier (UUID v4).
     * Exposed in all API responses and used in URL paths.
     * Example: "/550e8400-e29b-41d4-a716-446655440000"
     */
    @Column(name = "uuid", nullable = false, updatable = false, length = 36)
    private String uuid;

    // ============ Identity ============

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * BCrypt-hashed password. Never serialized to any DTO or API response.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // ============ Role & Status ============

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    /**
     * Soft-delete flag. Deleted users are retained for audit trail.
     * Physical deletion is handled by a scheduled cleanup job after the
     * retention period (default: 90 days).
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // ============ Profile ============

    /**
     * Job title / headline shown on the profile card.
     * Example: "Senior Backend Engineer"
     */
    @Column(name = "title", length = 150)
    private String title;

    /**
     * User-authored or CV-extracted summary.
     * Populated either manually by the user or asynchronously
     * by the PDF extraction pipeline after CV upload.
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    // ============ File Relations ============

    /**
     * One-to-many: a user can have multiple file records over time
     * (e.g. multiple CV versions, replaced avatars).
     * Only the most recent READY file of each FileType is served actively.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FileMetadata> files = new ArrayList<>();

    // ============ Timestamps ============

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Populated on soft-delete. Null means the account is still active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ============================================
    // Factory Methods
    // ============================================

    /**
     * Creates a new user with a generated UUID and default role USER.
     * Password must already be BCrypt-hashed by the caller.
     */
    public static User createNew(String firstName, String lastName, String email,
                                 String passwordHash, String phoneNumber) {
        return User.builder()
                .uuid(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordHash)
                .phoneNumber(phoneNumber) // might be null
                .role(Role.ROLE_USER)
                .active(true)
                .build();
    }

    // ============================================
    // Helper Methods
    // ============================================

    public boolean isAdmin() {
        return this.role == Role.ROLE_ADMIN;
    }

    /**
     * Soft-deletes the user. Does not remove the DB record.
     * Sets active=false and records the deletion timestamp.
     */
    public void softDelete() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a previously soft-deleted user account.
     */
    public void restore() {
        this.active = true;
        this.deletedAt = null;
    }

    /**
     * Returns true if the user account has been soft-deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Updates mutable profile fields. Null values are ignored (partial update).
     */
    public void updateProfile(String firstName, String lastName, String title, String summary,
                              String phoneNumber, String location, String linkedinUrl, String websiteUrl) {
        if (firstName != null) this.firstName = firstName;
        if (title != null) this.title = title;
        if (summary != null) this.summary = summary;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (location != null) this.location = location;
        if (linkedinUrl != null) this.linkedinUrl = linkedinUrl;
        if (websiteUrl != null) this.websiteUrl  = websiteUrl;
    }
}
