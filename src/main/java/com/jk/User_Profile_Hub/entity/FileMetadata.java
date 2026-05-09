package com.jk.User_Profile_Hub.entity;

import com.jk.User_Profile_Hub.enums.FileStatus;
import com.jk.User_Profile_Hub.enums.FileType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "file_metadata",
        indexes = {
                @Index(name = "idx_file_uuid", columnList = "uuid"),
                @Index(name = "idx_file_user_id", columnList = "user_id"),
                @Index(name = "idx_file_type", columnList = "file_type"),
                @Index(name = "idx_file_status", columnList = "status"),
                @Index(name = "idx_file_user_type", columnList = "user_id, file_type"),  // composite — most common query
                @Index(name = "idx_file_uploaded_at", columnList = "uploaded_at"),
                @Index(name = "idx_file_deleted_at", columnList = "deleted_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_file_uuid", columnNames = "uuid"),
                @UniqueConstraint(name = "uk_stored_name", columnNames = "stored_name")
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing file identifier (UUID v4).
     * Used in download/preview URLs: GET /files/{uuid}
     */
    @Column(name = "uuid", nullable = false, updatable = false, length = 36)
    private String uuid;

    // ============ Ownership ============

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_file_user")
    )
    private User user;

    // ============ File Classification ============

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStatus status;

    // ============ File Identity ============

    /**
     * The original filename as provided by the user at upload time.
     * Stored for display/download purposes only — never used for I/O.
     * Example: "John_Doe_CV_2025.pdf"
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /**
     * The UUID-based filename used for physical storage.
     * Format: {uuid}.{extension}
     * Example: "a3f8c2e1-d4b7-4c2a-9f1e-3b6d8a2e7c4f.pdf"
     * Guaranteed unique by the database constraint uk_stored_name.
     */
    @Column(name = "stored_name", nullable = false, updatable = false, length = 50)
    private String storedName;

    /**
     * Full relative path within the storage root.
     * Format: {userId}/{fileType}/original/{storedName}
     * Example: "42/cv/original/a3f8c2e1-d4b7.pdf"
     * Relative — the storage root prefix is resolved at runtime via StorageProvider.
     */
    @Column(name = "storage_path", nullable = false, updatable = false, length = 500)
    private String storagePath;

    /**
     * Relative path to the generated thumbnail (avatars only).
     * Format: {userId}/avatar/thumb/{storedName}.webp
     * Null for non-avatar file types.
     */
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    // ============ File Properties ============

    /**
     * MIME type as detected by Apache Tika from the file bytes.
     * NOT taken from the Content-Type header (untrusted).
     * Example: "application/pdf", "image/jpeg"
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /**
     * File extension derived from the detected MIME type (lowercase, no dot).
     * Example: "pdf", "jpg", "docx"
     */
    @Column(name = "extension", nullable = false, length = 10)
    private String extension;

    /**
     * File size in bytes of the original uploaded file.
     * Used for storage quota enforcement and display in the UI.
     */
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /**
     * SHA-256 hash of the file bytes.
     * Used for duplicate detection and integrity verification.
     */
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    // ============ Scan Result ============

    /**
     * Result from the antivirus scan (e.g. ClamAV).
     * Null until the async scan job completes.
     * Example: "OK", "Eicar-Test-Signature FOUND"
     */
    @Column(name = "scan_result", length = 255)
    private String scanResult;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    // ============ Timestamps ============

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Set when the file is soft-deleted.
     * Physical bytes are removed by a scheduled cleanup job after the retention period.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ============ Audit ============

    /**
     * User ID who uploaded the file.
     * Redundant with user.id but preserved for audit integrity even if the User record is deleted.
     */
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    // ============================================
    // Factory Methods
    // ============================================

    /**
     * Creates a new FileMetadata record in PROCESSING state.
     * Status transitions to READY or INFECTED once the async scan completes.
     */
    public static FileMetadata createNew(User user, FileType fileType, String originalName,
                                         String storedName, String storagePath, String mimeType,
                                         String extension, Long sizeBytes, String checksumSha256) {
        return FileMetadata.builder()
                .uuid(UUID.randomUUID().toString())
                .user(user)
                .fileType(fileType)
                .status(FileStatus.PROCESSING)
                .originalName(originalName)
                .storedName(storedName)
                .storagePath(storagePath)
                .mimeType(mimeType)
                .extension(extension)
                .sizeBytes(sizeBytes)
                .checksumSha256(checksumSha256)
                .uploadedBy(user.getId())
                .build();
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Marks the file as ready to serve after passing all async checks.
     */
    public void markReady() {
        this.status = FileStatus.READY;
    }

    /**
     * Marks the file as infected. File bytes are kept for forensic audit
     * but the file will not be served to any client.
     */
    public void markInfected(String scanResult) {
        this.status = FileStatus.INFECTED;
        this.scanResult = scanResult;
        this.scannedAt = LocalDateTime.now();
    }

    /**
     * Records a clean scan result and transitions status to READY.
     */
    public void markScanClean(String scanResult) {
        this.scanResult = scanResult;
        this.scannedAt = LocalDateTime.now();
        this.status = FileStatus.READY;
    }

    /**
     * Soft-deletes the file record. Physical byte removal is deferred to a cleanup job.
     */
    public void softDelete() {
        this.status = FileStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isServable() {
        return this.status == FileStatus.READY;
    }

    public boolean isProcessing() {
        return this.status == FileStatus.PROCESSING;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Returns a human-readable file size string (e.g. "2.4 MB").
     */
    public String getHumanReadableSize() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1048576) return String.format("%.1f KB", sizeBytes / 1024.0);
        if (sizeBytes < 1073741824) return String.format("%.1f MB", sizeBytes / 1048576.0);
        return String.format("%.1f GB", sizeBytes / 1073741824.0);
    }
}
