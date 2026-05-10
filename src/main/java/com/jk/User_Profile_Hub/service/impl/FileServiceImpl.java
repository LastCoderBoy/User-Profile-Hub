package com.jk.User_Profile_Hub.service.impl;

import com.jk.User_Profile_Hub.dto.response.FileMetadataResponse;
import com.jk.User_Profile_Hub.entity.FileMetadata;
import com.jk.User_Profile_Hub.entity.User;
import com.jk.User_Profile_Hub.enums.FileStatus;
import com.jk.User_Profile_Hub.enums.FileType;
import com.jk.User_Profile_Hub.exception.custom.FileStorageException;
import com.jk.User_Profile_Hub.exception.custom.FileValidationException;
import com.jk.User_Profile_Hub.exception.custom.ResourceNotFoundException;
import com.jk.User_Profile_Hub.redis.RedisService;
import com.jk.User_Profile_Hub.repository.FileMetadataRepository;
import com.jk.User_Profile_Hub.repository.UserRepository;
import com.jk.User_Profile_Hub.security.UserPrincipal;
import com.jk.User_Profile_Hub.service.FileService;
import com.jk.User_Profile_Hub.service.file.AsyncFileProcessingService;
import com.jk.User_Profile_Hub.service.file.FileValidationService;
import com.jk.User_Profile_Hub.service.file.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final RedisService redisService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final StorageProvider storageProvider;
    private final FileValidationService fileValidationService;
    private final AsyncFileProcessingService asyncFileProcessingService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMetadataResponse uploadFile(MultipartFile file,
                                           FileType fileType,
                                           UserPrincipal principal) {
        log.info("[FILE-SERVICE] Upload started — user: {}, type: {}, file: {}",
                principal.getId(), fileType, file.getOriginalFilename());

        User user = loadUser(principal.getId());

        // Validate - throws FileValidationException if invalid
        String detectedMime = fileValidationService.validateAndDetectMime(file, fileType);
        String extension = fileValidationService.extensionFromMime(detectedMime);

        // Compute checksum for duplicate detection
        String checksum = computeChecksum(file);

        if (fileMetadataRepository.existsByChecksumSha256AndUserIdAndStatus(checksum, user.getId(), FileStatus.READY)) {
            log.warn("[FILE-SERVICE] Duplicate file detected for user: {}", principal.getId());
            throw new FileValidationException("This file has already been uploaded and is active");
        }

        // Build safe storage path - user input never touches the path
        String storedName = UUID.randomUUID() + "." + extension;
        String relativePath = buildStoragePath(user.getId(), fileType, storedName);

        // Persist bytes - if this fails, nothing is saved to DB (method rolls back)
        storageProvider.store(file, relativePath);

        // Persist metadata with status PROCESSING
        FileMetadata metadata = FileMetadata.createNew(
                user,
                fileType,
                sanitizeOriginalName(file.getOriginalFilename()),
                storedName,
                relativePath,
                detectedMime,
                extension,
                file.getSize(),
                checksum
        );
        metadata = fileMetadataRepository.save(metadata);

        log.info("[FILE-SERVICE] File metadata saved — uuid: {}, status: PROCESSING", metadata.getUuid());

        // Trigger async tasks - run AFTER transaction commits so the
        // async thread sees the committed FileMetadata row
        triggerAsyncProcessing(metadata);

        // Invalidate the user's profile cache'
        redisService.invalidateUserProfile(user.getId());
        return toResponse(metadata);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMetadataResponse replaceFile(MultipartFile file,
                                            FileType fileType,
                                            UserPrincipal principal) {
        log.info("[FILE-SERVICE] Replace started — user: {}, type: {}", principal.getId(), fileType);

        // Soft-delete the current active file of this type (if any)
        fileMetadataRepository
                .findLatestByUserIdAndFileTypeAndStatus(principal.getId(), fileType, FileStatus.READY)
                .ifPresent(old -> {
                    old.softDelete();
                    fileMetadataRepository.save(old);
                    log.debug("[FILE-SERVICE] Soft-deleted previous file: {}", old.getUuid());
                });

        // Upload the new file using the same flow
        return uploadFile(file, fileType, principal);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(String fileUuid, UserPrincipal principal) {
        FileMetadata metadata = loadFileMetadata(fileUuid);

        enforceOwnerOrAdmin(metadata, principal);

        if (!metadata.isServable()) {
            throw new FileStorageException(
                    "File is not available for download — current status: " + metadata.getStatus());
        }

        InputStream stream = storageProvider.retrieve(metadata.getStoragePath());
        log.info("[FILE-SERVICE] File downloaded — uuid: {}, by user: {}", fileUuid, principal.getId());

        return new InputStreamResource(stream);
    }

    // ============================================
    //          METADATA QUERIES
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public FileMetadataResponse getFileMetadata(String fileUuid, UserPrincipal principal) {
        FileMetadata metadata = loadFileMetadata(fileUuid);
        enforceOwnerOrAdmin(metadata, principal);
        return toResponse(metadata);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getMyFiles(FileType fileType, UserPrincipal principal) {
        return fileMetadataRepository
                .findAllActiveByUserIdAndFileType(principal.getId(), fileType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================================
    // Delete
    // ============================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileUuid, UserPrincipal principal) {
        FileMetadata metadata = loadFileMetadata(fileUuid);
        enforceOwnerOrAdmin(metadata, principal);

        metadata.softDelete();
        fileMetadataRepository.save(metadata);

        // Invalidate the user's profile cache'
        redisService.invalidateUserProfile(principal.getId());
        log.info("[FILE-SERVICE] File soft-deleted — uuid: {}, by user: {}", fileUuid, principal.getId());
    }

    // ============================================
    //              HELPER METHODS
    // ============================================

    /**
     * Builds the relative storage path for a new file.
     * Format: {userId}/{fileType-lowercase}/original/{storedName}
     * Example: "42/cv/original/a3f8c2e1-d4b7.pdf"
     *
     * User ID is used (not UUID) as the directory name. It's internal,
     * shorter, and the directory is never exposed in URLs.
     */
    private String buildStoragePath(Long userId, FileType fileType, String storedName) {
        return userId + "/" + fileType.name().toLowerCase() + "/original/" + storedName;
    }

    /**
     * Builds the thumbnail storage path for an avatar.
     * Format: {userId}/avatar/thumb/{storedName}
     */
    private String buildThumbnailPath(Long userId, String storedName) {
        return userId + "/avatar/thumb/" + storedName;
    }

    /**
     * Triggers async post-processing tasks after the file is stored and metadata committed.
     * Scan runs for all file types. Thumbnail runs for AVATAR only.
     */
    private void triggerAsyncProcessing(FileMetadata metadata) {
        // Virus scan all types
        asyncFileProcessingService.scanFile(
                metadata.getUuid(),
                metadata.getStoragePath()
        );

        // Thumbnail - avatars only
        if (metadata.getFileType() == FileType.AVATAR) {
            String thumbnailPath = buildThumbnailPath(
                    metadata.getUploadedBy(),
                    metadata.getStoredName()
            );
            asyncFileProcessingService.generateAvatarThumbnail(
                    metadata.getUuid(),
                    metadata.getStoragePath(),
                    thumbnailPath
            );
        }
    }

    /**
     * Computes SHA-256 checksum of the file bytes.
     * Used for duplicate detection: same bytes = same hash.
     */
    private String computeChecksum(MultipartFile file) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (Exception e) {
            throw new FileStorageException("Failed to compute file checksum", e);
        }
    }

    /**
     * Sanitizes the original filename for safe storage in the DB.
     * Strips path separators that could be injected to manipulate stored paths.
     * The sanitized name is display-only — never used for I/O.
     */
    private String sanitizeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unnamed";
        }
        // Strip any path components - keep filename only
        String name = originalFilename
                .replaceAll("[/\\\\]", "_")
                .replaceAll("\\.\\.", "_")
                .strip();

        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private FileMetadata loadFileMetadata(String fileUuid) {
        return fileMetadataRepository.findByUuid(fileUuid)
                .orElseThrow(() -> {
                    log.warn("[FILE-SERVICE] File not found: {}", fileUuid);
                    return new ResourceNotFoundException("File not found: " + fileUuid);
                });
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Enforces that only the file owner or an ADMIN can access/modify this file.
     */
    private void enforceOwnerOrAdmin(FileMetadata metadata, UserPrincipal principal) {
        boolean isOwner = metadata.getUploadedBy().equals(principal.getId());
        boolean isAdmin = principal.isAdmin();

        if (!isOwner && !isAdmin) {
            log.warn("[FILE-SERVICE] Unauthorized access attempt — file: {}, user: {}",
                    metadata.getUuid(), principal.getId());
            throw new AccessDeniedException("You do not have permission to access this file");
        }
    }

    /**
     * Maps a FileMetadata entity to a response DTO with resolved download URL.
     * Thumbnail URL is only populated for AVATAR files.
     */
    private FileMetadataResponse toResponse(FileMetadata metadata) {
        String downloadUrl  = "/files/" + metadata.getUuid() + "/download";
        String thumbnailUrl = metadata.getThumbnailPath() != null
                ? "/files/" + metadata.getUuid() + "/thumbnail"
                : null;

        return FileMetadataResponse.from(metadata, downloadUrl, thumbnailUrl);
    }
}
