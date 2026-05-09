package com.jk.User_Profile_Hub.dto.response;

import com.jk.User_Profile_Hub.entity.FileMetadata;
import com.jk.User_Profile_Hub.enums.FileStatus;
import com.jk.User_Profile_Hub.enums.FileType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileMetadataResponse {

    /**
     * Public-facing DTO for a file record.
     * Does NOT expose: internal id, storagePath, storedName, checksumSha256, scanResult.
     * Those fields are internal to the server and must never reach the client.
     */
    private String uuid;
    private FileType fileType;
    private FileStatus status;

    // Original filename as provided by the user at upload time.
    private String originalName;

    // MIME type detected by Apache Tika (not from the client header).
    private String mimeType;

    // File extension (lowercase, no dot).
    private String extension;

    // File size as a formatted human-readable string (e.g. "2.4 MB").
    private String fileSize;

    // Raw size in bytes - useful for client-side progress bars or quota display.
    private Long fileSizeBytes;

    //Download/stream URL for this file.
    private String downloadUrl;

    /**
     * Thumbnail URL - only populated for AVATAR files.
     * Null for CV and COVER_LETTER types.
     */
    private String thumbnailUrl;

    /**
     * True if the file is fully processed and safe to serve.
     * Clients should poll or wait for WebSocket notification while this is false.
     */
    private boolean ready;

    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;

    // ============================================
    // Factory Methods
    // ============================================

    /**
     * Maps a FileMetadata entity to a response DTO.
     * Caller provides the resolved download URL (depends on StorageProvider base URL).
     *
     * @param downloadUrl  resolved download URL
     * @param thumbnailUrl resolved thumbnail URL (null for non-avatar files)
     */
    public static FileMetadataResponse from(FileMetadata file,
                                            String downloadUrl,
                                            String thumbnailUrl) {
        return FileMetadataResponse.builder()
                .uuid(file.getUuid())
                .fileType(file.getFileType())
                .status(file.getStatus())
                .originalName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .extension(file.getExtension())
                .fileSize(file.getHumanReadableSize())
                .fileSizeBytes(file.getSizeBytes())
                .downloadUrl(downloadUrl)
                .thumbnailUrl(thumbnailUrl)
                .ready(file.isServable())
                .uploadedAt(file.getUploadedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    /**
     * Convenience overload - no thumbnail URL (for CV and COVER_LETTER).
     */
    public static FileMetadataResponse from(FileMetadata file, String downloadUrl) {
        return from(file, downloadUrl, null);
    }
}
