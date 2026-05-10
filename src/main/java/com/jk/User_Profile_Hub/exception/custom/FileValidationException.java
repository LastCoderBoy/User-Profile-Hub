package com.jk.User_Profile_Hub.exception.custom;

/**
 * Thrown when an uploaded file fails validation:
 *   - MIME type not allowed for the declared FileType
 *   - File size exceeds the configured limit
 *   - File extension does not match detected MIME type
 *   - File is empty or unreadable
 */
public class FileValidationException extends RuntimeException {

    public FileValidationException(String message) {
        super(message);
    }

    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
