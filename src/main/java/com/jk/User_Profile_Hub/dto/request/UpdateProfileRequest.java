package com.jk.User_Profile_Hub.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    /**
     * All fields are optional — this is a partial update (PATCH semantics).
     * Null fields are ignored; only non-null fields are applied.
     */

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Summary must not exceed 2000 characters")
    private String summary;

    @Pattern(
            regexp = "^\\+?[1-9]\\d{6,14}$",
            message = "Phone number must be a valid international format (e.g. +48123456789)"
    )
    private String phoneNumber;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @URL(message = "LinkedIn URL must be a valid URL")
    @Size(max = 255, message = "LinkedIn URL must not exceed 255 characters")
    @Pattern(
            regexp = "^(https?://)?(www\\.)?linkedin\\.com/.*$",
            message = "Must be a valid LinkedIn profile URL"
    )
    private String linkedinUrl;

    @URL(message = "Website URL must be a valid URL")
    @Size(max = 255, message = "Website URL must not exceed 255 characters")
    private String websiteUrl;
}
