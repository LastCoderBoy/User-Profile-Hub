package com.jk.User_Profile_Hub.repository;

import com.jk.User_Profile_Hub.entity.FileMetadata;
import com.jk.User_Profile_Hub.enums.FileStatus;
import com.jk.User_Profile_Hub.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByUuid(String uuid);

    /**
     * Finds the most recent READY file of a given type for a user.
     * Used to resolve the "current active avatar/CV" for profile display.
     * Hits the composite index idx_file_user_type.
     */
    @Query("""
            SELECT f FROM FileMetadata f
            WHERE f.user.id = :userId
              AND f.fileType = :fileType
              AND f.status = :status
            ORDER BY f.uploadedAt DESC
            LIMIT 1
            """)
    Optional<FileMetadata> findLatestByUserIdAndFileTypeAndStatus(
            @Param("userId") Long userId,
            @Param("fileType") FileType fileType,
            @Param("status") FileStatus status
    );

    /**
     * Finds all non-deleted files of a given type for a user.
     * Used to list file history (e.g. all CV versions).
     */
    @Query("""
            SELECT f FROM FileMetadata f
            WHERE f.user.id = :userId
              AND f.fileType = :fileType
              AND f.status != com.jk.User_Profile_Hub.enums.FileStatus.DELETED
            ORDER BY f.uploadedAt DESC
            """)
    List<FileMetadata> findAllActiveByUserIdAndFileType(
            @Param("userId") Long userId,
            @Param("fileType") FileType fileType
    );

    /**
     * Used by the scheduled cleanup job to find soft-deleted files
     * whose physical bytes can now be permanently removed.
     */
    @Query("""
            SELECT f FROM FileMetadata f
            WHERE f.status = com.jk.User_Profile_Hub.enums.FileStatus.DELETED
              AND f.deletedAt < CURRENT_TIMESTAMP - :retentionDays DAY
            """)
    List<FileMetadata> findExpiredDeletedFiles(@Param("retentionDays") int retentionDays);

    /**
     * Updates file status directly — used by async scan job
     * without loading the full entity.
     */
    @Modifying
    @Query("""
            UPDATE FileMetadata f
            SET f.status = :status, f.scanResult = :scanResult, f.scannedAt = CURRENT_TIMESTAMP
            WHERE f.uuid = :uuid
            """)
    int updateScanResult(@Param("uuid") String uuid, @Param("status") FileStatus status, @Param("scanResult") String scanResult);

    boolean existsByChecksumSha256AndUserIdAndStatus(String checksumSha256, Long userId, FileStatus status);
}