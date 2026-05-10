package com.jk.User_Profile_Hub.service.file.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Storage abstraction layer.
 *
 * Implementations:
 *   - LocalStorageProvider => profile: dev (writes to local disk)
 *   - S3StorageProvider => profile: prod (writes to AWS S3)
 *
 * Controllers and services never reference implementations directly —
 * they depend only on this interface. Switching from local to S3
 * requires zero code changes in the business layer.
 */
public interface StorageProvider {

    /**
     * Stores the file at the given relative path.
     *
     * @param file         the multipart file to store
     * @param relativePath relative path within the storage root
     *                     (e.g. "42/cv/original/a3f8c2e1.pdf")
     * @return the resolved absolute path or S3 key where the file was stored
     */
    String store(MultipartFile file, String relativePath);

    /**
     * Opens an InputStream for reading a stored file.
     * Caller is responsible for closing the stream.
     *
     * @param relativePath relative path of the stored file
     * @return InputStream of the file bytes
     */
    InputStream retrieve(String relativePath);

    /**
     * Permanently deletes the file at the given relative path.
     * Called by the scheduled cleanup job after the soft-delete retention period.
     *
     * @param relativePath relative path of the file to delete
     */
    void delete(String relativePath);

    /**
     * Returns true if a file exists at the given relative path.
     *
     * @param relativePath relative path to check
     */
    boolean exists(String relativePath);

    /**
     * Resolves a relative path to a publicly accessible URL.
     * For local storage: "http://localhost:8080/files/{uuid}/download"
     * For S3: "https://{bucket}.s3.amazonaws.com/{key}" or a pre-signed URL
     *
     * @param relativePath relative path of the stored file
     * @return public-facing URL string
     */
    String resolveUrl(String relativePath);
}
