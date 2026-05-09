package com.jk.User_Profile_Hub.dto.request;

import com.jk.User_Profile_Hub.enums.FileType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadRequest {

    /**
     * The actual file bytes from the multipart form.
     * MIME type validation is performed by FileValidationService using Apache Tika —
     * the Content-Type header from this field is explicitly NOT trusted.
     */
    @NotNull(message = "File must not be null")
    private MultipartFile file;

    /**
     * The intended file type declared by the client.
     * Cross-validated against the detected MIME type:
     *   AVATAR -> image/jpeg, image/png, image/webp
     *   CV -> application/pdf
     *   COVER_LETTER -> application/pdf, application/vnd.openxmlformats-officedocument.wordprocessingml.document
     */
    @NotNull(message = "File type is required")
    private FileType fileType;
    
}
