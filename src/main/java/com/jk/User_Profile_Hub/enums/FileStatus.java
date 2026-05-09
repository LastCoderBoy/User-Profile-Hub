package com.jk.User_Profile_Hub.enums;

public enum FileStatus {

    /**
     * File has been uploaded and stored, async processing is still running
     * (virus scan, thumbnail generation, etc.)
     */
    PROCESSING,

    /**
     * All async checks passed — file is safe and ready to serve
     */
    READY,

    /**
     * Virus scanner or hash check flagged this file as unsafe
     * File bytes are retained for forensic audit but not served to users
     */
    INFECTED,

    /**
     * Soft-deleted by the user or an admin
     * Retained for audit trail; physical deletion is handled by a scheduled cleanup job
     */
    DELETED
}
