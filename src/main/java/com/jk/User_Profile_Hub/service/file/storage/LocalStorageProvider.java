package com.jk.User_Profile_Hub.service.file.storage;

import com.jk.User_Profile_Hub.exception.custom.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local disk storage provider - active on 'dev' profile only.
 *
 * Files are stored under the configured root directory, preserving
 * the relative path structure: {storageRoot}/{userId}/{fileType}/original/{storedName}
 *
 * NOT suitable for production:
 *   - Not horizontally scalable (files live on one machine)
 *   - No redundancy or replication
 *   - Use S3StorageProvider in production
 */
@Slf4j
@Component
@Profile("dev")
public class LocalStorageProvider implements StorageProvider {

    @Value("${app.storage.local.root-dir}")
    private String rootDir;

    @Value("${app.server.base-url}")
    private String baseUrl;

    private Path storageRoot;

    @PostConstruct
    public void init() {
        this.storageRoot = Paths.get(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
            log.info("[LOCAL-STORAGE] Initialized at: {}", storageRoot);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize local storage directory: " + storageRoot, e);
        }
    }

    @Override
    public String store(MultipartFile file, String relativePath) {
        try {
            Path targetPath = resolveAndValidatePath(relativePath);
            Files.createDirectories(targetPath.getParent());

            // REPLACE_EXISTING handles the "replace CV" use case atomically
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.debug("[LOCAL-STORAGE] Stored file at: {}", targetPath);
            return relativePath;

        } catch (IOException e) {
            log.error("[LOCAL-STORAGE] Failed to store file at path {}: {}", relativePath, e.getMessage());
            throw new FileStorageException("Failed to store file: " + relativePath, e);
        }
    }

    @Override
    public InputStream retrieve(String relativePath) {
        try {
            Path filePath = resolveAndValidatePath(relativePath);
            if (!Files.exists(filePath)) {
                throw new FileStorageException("File not found at path: " + relativePath);
            }
            return Files.newInputStream(filePath);

        } catch (IOException e) {
            log.error("[LOCAL-STORAGE] Failed to retrieve file at path {}: {}", relativePath, e.getMessage());
            throw new FileStorageException("Failed to retrieve file: " + relativePath, e);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path filePath = resolveAndValidatePath(relativePath);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("[LOCAL-STORAGE] Deleted file at: {}", filePath);
            } else {
                log.warn("[LOCAL-STORAGE] File not found for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.error("[LOCAL-STORAGE] Failed to delete file at path {}: {}", relativePath, e.getMessage());
            throw new FileStorageException("Failed to delete file: " + relativePath, e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            Path filePath = resolveAndValidatePath(relativePath);
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String resolveUrl(String relativePath) {
        // URL is assembled by the controller using the file UUID, not the storage path.
        // This just provides the base - actual download URL: GET /files/{uuid}/download
        return baseUrl + "/files/" + relativePath;
    }

    // ============================================
    // Private Helpers
    // ============================================

    /**
     * Resolves a relative path against the storage root and validates that
     * the result stays within the root (path traversal prevention).
     *
     * Rejects any path containing ".." segments that could escape the storage root.
     */
    private Path resolveAndValidatePath(String relativePath) {
        Path resolved = storageRoot.resolve(relativePath).normalize();

        if (!resolved.startsWith(storageRoot)) {
            log.error("[LOCAL-STORAGE] Path traversal attempt detected: {}", relativePath);
            throw new FileStorageException("Invalid file path — path traversal detected");
        }

        return resolved;
    }
}
