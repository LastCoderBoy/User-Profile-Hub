package com.jk.User_Profile_Hub.service.file;

import com.jk.User_Profile_Hub.enums.FileType;
import com.jk.User_Profile_Hub.exception.custom.FileValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static com.jk.User_Profile_Hub.utils.AppConstants.*;

/**
 * Validates uploaded files before they are stored.
 *
 * Security principles applied:
 *   1. MIME type is detected from file BYTES via Apache Tika — never trusted from the HTTP header.
 *      A renamed .exe with Content-Type: image/jpeg will be detected and rejected.
 *   2. Detected MIME must be in the whitelist for the declared FileType.
 *   3. File size is checked against per-type limits.
 *   4. Empty files are rejected immediately.
 */
@Slf4j
@Service
public class FileValidationService {

    private final Tika tika = new Tika();

    // ============================================
    // Public API
    // ============================================

    /**
     * Validates the file and returns the detected MIME type.
     * Throws {@link FileValidationException} if any check fails.
     *
     * @param file     the uploaded multipart file
     * @param fileType the declared file type from the request
     * @return the detected MIME type string (e.g. "application/pdf")
     */
    public String validateAndDetectMime(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File must not be empty");
        }
        validateSize(file, fileType);

        String detectedMime = detectMimeType(file);
        validateMimeAllowed(detectedMime, fileType, file.getOriginalFilename());

        log.debug("[FILE-VALIDATION] File '{}' passed validation — type: {}, mime: {}, size: {} bytes",
                file.getOriginalFilename(), fileType, detectedMime, file.getSize());

        return detectedMime;
    }

    // Derives the file extension from a detected MIME type.
    public String extensionFromMime(String detectedMime) {
        String ext = MIME_TO_EXTENSION.get(detectedMime);
        if (ext == null) {
            throw new FileValidationException("Unsupported MIME type: " + detectedMime);
        }
        return ext;
    }

    // ============================================
    // Private Validation Steps
    // ============================================

    private void validateSize(MultipartFile file, FileType fileType) {
        long maxSize = switch (fileType) {
            case AVATAR -> MAX_AVATAR_SIZE_BYTES;
            case CV -> MAX_CV_SIZE_BYTES;
            case COVER_LETTER -> MAX_COVER_LETTER_SIZE_BYTES;
        };

        if (file.getSize() > maxSize) {
            throw new FileValidationException(String.format(
                    "File size %s exceeds the maximum allowed size of %s for %s",
                    humanReadable(file.getSize()),
                    humanReadable(maxSize),
                    fileType.name()
            ));
        }
    }

    /**
     * Uses Apache Tika to detect the true MIME type from the first bytes of the file.
     * Tika reads a magic-byte header (typically first 4–8KB) to identify the format.
     */
    private String detectMimeType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String detected = tika.detect(inputStream, file.getOriginalFilename());
            log.debug("[FILE-VALIDATION] Tika detected MIME: {} for file: {}",
                    detected, file.getOriginalFilename());
            return detected;
        } catch (IOException e) {
            log.error("[FILE-VALIDATION] Failed to detect MIME type for file: {}", file.getOriginalFilename());
            throw new FileValidationException("Failed to read file content for MIME detection", e);
        }
    }

    private void validateMimeAllowed(String detectedMime, FileType fileType, String originalName) {
        Set<String> allowed = ALLOWED_MIME_TYPES.get(fileType);

        if (allowed == null || !allowed.contains(detectedMime)) {
            log.warn("[FILE-VALIDATION] Rejected file '{}' — detected MIME '{}' not allowed for type {}",
                    originalName, detectedMime, fileType);
            throw new FileValidationException(String.format(
                    "File type '%s' is not allowed for %s uploads. Allowed types: %s",
                    detectedMime, fileType.name(), allowed
            ));
        }
    }

    private String humanReadable(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.1f GB", bytes / 1073741824.0);
    }
}
