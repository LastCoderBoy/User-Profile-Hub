package com.jk.User_Profile_Hub.exception.custom;

/**
 * Thrown when a physical file operation fails (read, write, delete).
 * Wraps IOException from the storage layer into an unchecked exception
 * so services don't need to declare checked exceptions.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
