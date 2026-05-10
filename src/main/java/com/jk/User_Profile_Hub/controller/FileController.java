package com.jk.User_Profile_Hub.controller;

import com.jk.User_Profile_Hub.dto.ApiResponse;
import com.jk.User_Profile_Hub.dto.request.FileUploadRequest;
import com.jk.User_Profile_Hub.dto.response.FileMetadataResponse;
import com.jk.User_Profile_Hub.enums.FileType;
import com.jk.User_Profile_Hub.security.UserPrincipal;
import com.jk.User_Profile_Hub.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.jk.User_Profile_Hub.utils.AppConstants.FILE_PATH;

/**
 * Handles all file upload, download, and management endpoints.
 *
 * All endpoints require authentication via JWT (enforced by Spring Security config).
 *
 * File bytes are never returned as JSON — they are streamed via ResponseEntity<Resource>
 * with the appropriate Content-Type and Content-Disposition headers.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(FILE_PATH)
public class FileController {

    private final FileService fileService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<FileMetadataResponse>>> getMyFiles(
            @RequestParam FileType fileType,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[FILE-CONTROLLER] Get my files request - user: {}, type: {}", principal.getUuid(), fileType);

        List<FileMetadataResponse> files = fileService.getMyFiles(fileType, principal);
        return ResponseEntity.ok(ApiResponse.success("Files retrieved", files));
    }

    /**
     * Uploads a new file for the authenticated user.
     *
     * Returns 202 ACCEPTED (not 201) because the file is in PROCESSING state -
     * async scan and thumbnail generation are still running.
     *
     * @param request the file and its type (AVATAR, CV, COVER_LETTER)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadataResponse>> uploadFile(
            @Valid @ModelAttribute FileUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[FILE-CONTROLLER] Upload request - user: {}, type: {}, file: {}",
                principal.getUuid(), request.getFileType(), request.getFile().getOriginalFilename());

        FileMetadataResponse response = fileService.uploadFile(request.getFile(), request.getFileType(), principal);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File uploaded successfully. Processing in progress.", response));
    }

    /**
     * Replaces the user's current file of the given type.
     * Soft-deletes the previous version and uploads the new one atomically.
     *
     * Use case: user uploads a new CV: old CV is archived, new one is active.
     *
     * @param uuid      the UUID of the file being replaced (used for audit trail)
     * @param request   the new file and its type (must match the existing file's type)
     */
    @PutMapping(value = "/{uuid}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadataResponse>> replaceFile(
            @PathVariable String uuid,
            @Valid @ModelAttribute FileUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[FILE-CONTROLLER] Replace request - user: {}, type: {}, replacing: {}",
                principal.getUuid(), request.getFileType(), uuid);

        FileMetadataResponse response = fileService.replaceFile(request.getFile(), request.getFileType(), principal);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File replaced successfully. Processing in progress.", response));
    }

    // ============================================
    // Download / Stream
    // ============================================

    /**
     * Streams the file bytes to the client.
     *
     * Content-Disposition: attachment forces browser download.
     * Content-Type is set from the stored MIME type so browsers handle it correctly.
     * Only READY files are served - PROCESSING/INFECTED/DELETED return an error.
     *
     * @param uuid the public UUID of the file
     */
    @GetMapping("/{uuid}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[FILE-CONTROLLER] Download request - file: {}, user: {}", uuid, principal.getUuid());

        // Metadata is needed for Content-Type and Content-Disposition headers
        FileMetadataResponse metadata = fileService.getFileMetadata(uuid, principal);
        Resource resource = fileService.downloadFile(uuid, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                .body(resource);
    }

    /**
     * Streams the avatar thumbnail (200×200 PNG).
     * Returns inline (no Content-Disposition: attachment) so browsers render it directly.
     * Returns 404 if the file is not an avatar or thumbnail hasn't been generated yet.
     *
     * @param uuid the public UUID of the avatar file
     */
    @GetMapping("/{uuid}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[FILE-CONTROLLER] Thumbnail request - file: {}, user: {}", uuid, principal.getUuid());

        FileMetadataResponse metadata = fileService.getFileMetadata(uuid, principal);

        if (metadata.getThumbnailUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = fileService.downloadFile(uuid, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }

    /**
     * Returns metadata for a specific file without downloading the bytes.
     * Used by the frontend to check file status (PROCESSING -> READY) after upload.
     *
     * @param uuid the public UUID of the file
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getFileMetadata(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileMetadataResponse response = fileService.getFileMetadata(uuid, principal);
        return ResponseEntity.ok(ApiResponse.success("File metadata retrieved", response));
    }

    /**
     * Soft-deletes a file.
     * Physical bytes are retained for the configured retention period
     * and removed by a scheduled cleanup job.
     *
     * @param uuid the public UUID of the file to delete
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[FILE-CONTROLLER] Delete request: file: {}, user: {}", uuid, principal.getUuid());

        fileService.deleteFile(uuid, principal);

        return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
    }
}
