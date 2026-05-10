package com.jk.User_Profile_Hub.service.file;

import com.jk.User_Profile_Hub.enums.FileStatus;
import com.jk.User_Profile_Hub.repository.FileMetadataRepository;
import com.jk.User_Profile_Hub.service.file.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.jk.User_Profile_Hub.utils.AppConstants.THUMBNAIL_SIZE;

/**
 * Handles all async file processing tasks that run AFTER the HTTP response is returned.
 *
 * Executes on the dedicated "fileAsyncExecutor" thread pool (defined in AsyncConfig),
 * never on the HTTP request thread. Each method uses REQUIRES_NEW so failures here
 * don't roll back the upload transaction that already committed.
 *
 * Current tasks:
 *   - Avatar thumbnail generation (200×200 PNG)
 *   - Simulated virus scan (replace with ClamAV integration in production)
 *
 * Status flow:
 *   PROCESSING -> READY (scan clean)
 *   PROCESSING -> INFECTED (scan flagged)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFileProcessingService {

    private final FileMetadataRepository fileMetadataRepository;
    private final StorageProvider storageProvider;

    // ============================================
    // Thumbnail Generation
    // ============================================

    /**
     * Generates a 200×200 thumbnail from the uploaded avatar.
     *
     * The thumbnail is stored at: {userId}/avatar/thumb/{storedName}
     * The FileMetadata.thumbnailPath is updated on completion.
     *
     * @param fileUuid the public UUID of the FileMetadata record
     * @param storagePath the original file's storage path
     * @param thumbnailPath the target path for the generated thumbnail
     */
    @Async("fileAsyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAvatarThumbnail(String fileUuid, String storagePath, String thumbnailPath) {
        log.info("[ASYNC-FILE] Starting thumbnail generation for file: {}", fileUuid);
        try {
            // Read original image from storage
            try (InputStream inputStream = storageProvider.retrieve(storagePath)) {
                BufferedImage original = ImageIO.read(inputStream);

                if (original == null) {
                    log.error("[ASYNC-FILE] Failed to decode image for file: {}", fileUuid);
                    return;
                }

                // Scale to 200×200 maintaining aspect ratio with crop
                BufferedImage thumbnail = resizeAndCrop(original, THUMBNAIL_SIZE, THUMBNAIL_SIZE);

                // Write thumbnail bytes to a byte array
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(thumbnail, "png", outputStream);
                byte[] thumbnailBytes = outputStream.toByteArray();

                // Store thumbnail via StorageProvider
                storageProvider.store(
                        new ByteArrayMultipartFile(thumbnailBytes, thumbnailPath, "image/png"),
                        thumbnailPath
                );

                // Update the thumbnailPath on the FileMetadata record
                fileMetadataRepository.findByUuid(fileUuid).ifPresent(file -> {
                    file.setThumbnailPath(thumbnailPath);
                    fileMetadataRepository.save(file);
                });

                log.info("[ASYNC-FILE] Thumbnail generated for file: {} at path: {}", fileUuid, thumbnailPath);
            }
        } catch (IOException e) {
            // Thumbnail failure is non-fatal — original file is still served
            log.error("[ASYNC-FILE] Thumbnail generation failed for file {}: {}", fileUuid, e.getMessage(), e);
        }
    }

    // ============================================
    // Virus Scan
    // ============================================

    /**
     * Performs a virus/malware scan on the stored file bytes.
     *
     * @param fileUuid    the public UUID of the FileMetadata record
     * @param storagePath the path of the file to scan
     */
    @Async("fileAsyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scanFile(String fileUuid, String storagePath) {
        log.info("[ASYNC-FILE] Starting virus scan for file: {}", fileUuid);
        try {
            // Production: replace with ClamAV integration ...

            // Dev stub - always returns clean
            String scanResult = "OK";
            FileStatus newStatus = FileStatus.READY;

            int updated = fileMetadataRepository.updateScanResult(fileUuid, newStatus, scanResult);

            if (updated == 0) {
                log.warn("[ASYNC-FILE] File not found for scan update: {}", fileUuid);
            } else {
                log.info("[ASYNC-FILE] Scan complete for file: {} — status: {}", fileUuid, newStatus);
            }

        } catch (Exception e) {
            log.error("[ASYNC-FILE] Scan failed for file {}: {}", fileUuid, e.getMessage(), e);
            // On unexpected scan failure: leave status as PROCESSING
            // A retry mechanism or admin alert would be added here in production
        }
    }

    // ============================================
    // Private Helpers
    // ============================================

    /**
     * Resizes an image to the target dimensions using a center-crop strategy.
     * Maintains aspect ratio by scaling to fill, then cropping to the exact target size.
     */
    private BufferedImage resizeAndCrop(BufferedImage original, int targetWidth, int targetHeight) {
        // Scale to fill (maintain aspect ratio)
        double scaleX = (double) targetWidth  / original.getWidth();
        double scaleY = (double) targetHeight / original.getHeight();
        double scale  = Math.max(scaleX, scaleY);

        int scaledWidth  = (int) Math.round(original.getWidth()  * scale);
        int scaledHeight = (int) Math.round(original.getHeight() * scale);

        Image scaled = original.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        // Center crop to target size
        int x = (scaledWidth  - targetWidth)  / 2;
        int y = (scaledHeight - targetHeight) / 2;
        return scaledImage.getSubimage(x, y, targetWidth, targetHeight);
    }

    /**
     * Minimal MultipartFile adapter for passing byte arrays to StorageProvider.store().
     * Used internally to store generated thumbnails without writing temp files.
     */
    private record ByteArrayMultipartFile(
            byte[] bytes,
            String name,
            String contentType
    ) implements MultipartFile {

        @Override public String getName()                        { return name; }
        @Override public String getOriginalFilename()           { return name; }
        @Override public String getContentType()                { return contentType; }
        @Override public boolean isEmpty()                      { return bytes.length == 0; }
        @Override public long getSize()                         { return bytes.length; }
        @Override public byte[] getBytes()                      { return bytes; }
        @Override public InputStream getInputStream()           { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            throw new UnsupportedOperationException("transferTo not supported for in-memory files");
        }
    }
}
