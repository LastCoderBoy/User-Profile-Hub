package com.jk.User_Profile_Hub.utils;

import com.jk.User_Profile_Hub.enums.FileType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AppConstants {

    public static final String API_VERSION = "v1";
    public static final String BASE_PATH = "/api/" + API_VERSION;
    public static final String ADMIN_PATH = BASE_PATH + "/admin";
    public static final String AUTH_PATH = BASE_PATH + "/auth";
    public static final String PROFILE_PATH = BASE_PATH + "/profile";
    public static final String FILE_PATH = BASE_PATH + "/file";

    public static final List<String> PUBLIC_PATHS = List.of(
            // Authorization endpoints
            AUTH_PATH + "/register",
            AUTH_PATH + "/login",
            AUTH_PATH + "/refresh-token",

            // Actuator endpoints
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics",
            "/actuator/prometheus",

            // Swagger/API docs
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );


    // ========== HTTP Headers ==========
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String IP_ADDRESS_HEADER = "X-Forwarded-For";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String USER_EMAIL_HEADER = "X-User-Email";

    // ========== JWT ==========
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = 7;
    public static final long ACCESS_TOKEN_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    public static final long REFRESH_TOKEN_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days
    public static final String JWT_CLAIM_USER_ID = "userId";
    public static final String JWT_CLAIM_ROLES = "userRole";
    public static final String JWT_CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    // ========== Cache Keys ==========
    public static final String CACHE_USER_PROFILE_PREFIX = "user:profile:";
    public static final String CACHE_USER_PRINCIPAL = "user:principal:";
    public static final String CACHE_TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    public static final String CACHE_REFRESH_TOKEN_PREFIX = "refresh:token:";

    // ========== Cache TTL (in seconds) ==========
    public static final long CACHE_USER_PROFILE_TTL = 30;      // 30 minutes
    public static final long CACHE_REFRESH_TOKEN_TTL = 10080;  // 7 days
    public static final long CACHE_EMAIL_TOKEN_TTL = 1440;     // 24 hours
    public static final long CACHE_PRINCIPAL_TTL = 5;                // 5 minutes


    // ========== Pagination ==========
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // ========== File Configurations ==========
    public static final int THUMBNAIL_SIZE = 200;
    public static long MAX_AVATAR_SIZE_BYTES = 5242880; // 5 MB default
    public static long MAX_CV_SIZE_BYTES =10485760; // 10 MB default
    public static long MAX_COVER_LETTER_SIZE_BYTES = 10485760; // 10 MB default

    public static final Map<FileType, Set<String>> ALLOWED_MIME_TYPES = Map.of(
            FileType.AVATAR, Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            ),
            FileType.CV, Set.of(
                    "application/pdf"
            ),
            FileType.COVER_LETTER, Set.of(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"  // .docx
            )
    );

    /**
     * Maps detected MIME type to a clean file extension.
     * Used to derive the stored filename extension from actual content not from user input.
     */
    public static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"
    );
}