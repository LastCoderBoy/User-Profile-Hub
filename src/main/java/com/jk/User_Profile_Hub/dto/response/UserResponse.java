package com.jk.User_Profile_Hub.dto.response;

import com.jk.User_Profile_Hub.entity.User;
import com.jk.User_Profile_Hub.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Public-facing DTO. Never includes passwordHash, internal id, or deletedAt.
 * Constructed exclusively via the static factory {@link #from(User)} to
 * guarantee no accidental field leakage.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String uuid;
    private String fullName;
    private String email;
    private String title;
    private String summary;
    private String phoneNumber;
    private String location;
    private String linkedinUrl;
    private String websiteUrl;
    private Role role;
    private boolean active;

    // URL to the current active avatar thumbnail. Null if no avatar uploaded.
    private String avatarThumbnailUrl;

    // URL to download the current active CV. Null if no CV uploaded.
    private String cvDownloadUrl;

    // URL to download the current active cover letter. Null if none uploaded.
    private String coverLetterDownloadUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ============================================
    // Factory Method
    // ============================================

    /**
     * Maps a User entity to a safe public response.
     * File URLs are injected separately (they require the StorageProvider base URL).
     * Use {@link #from(User, String, String, String)} when file URLs are available.
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .uuid(user.getUuid())
                .fullName(getFullName(user.getFirstName(), user.getLastName()))
                .email(user.getEmail())
                .title(user.getTitle())
                .summary(user.getSummary())
                .phoneNumber(user.getPhoneNumber())
                .location(user.getLocation())
                .linkedinUrl(user.getLinkedinUrl())
                .websiteUrl(user.getWebsiteUrl())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Maps a User entity with resolved file download/preview URLs.
     *
     * @param avatarThumbnailUrl  URL for the 200×200 WebP avatar thumbnail (nullable)
     * @param cvDownloadUrl       URL for the PDF CV download (nullable)
     * @param coverLetterUrl      URL for the DOCX/PDF cover letter download (nullable)
     */
    public static UserResponse from(User user,
                                    String avatarThumbnailUrl,
                                    String cvDownloadUrl,
                                    String coverLetterUrl) {
        UserResponse response = from(user);
        response.setAvatarThumbnailUrl(avatarThumbnailUrl);
        response.setCvDownloadUrl(cvDownloadUrl);
        response.setCoverLetterDownloadUrl(coverLetterUrl);
        return response;
    }

    private static String getFullName(String firstName, String lastName) {
        return lastName != null ? firstName + " " + lastName : firstName;
    }
}
