package com.jk.User_Profile_Hub.service;

import com.jk.User_Profile_Hub.dto.response.FileMetadataResponse;
import com.jk.User_Profile_Hub.enums.FileType;
import com.jk.User_Profile_Hub.security.UserPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    FileMetadataResponse uploadFile(MultipartFile file, FileType fileType, UserPrincipal principal);

    FileMetadataResponse replaceFile(MultipartFile file, FileType fileType, UserPrincipal principal);

    FileMetadataResponse getFileMetadata(String uuid, UserPrincipal principal);

    Resource downloadFile(String uuid, UserPrincipal principal);

    List<FileMetadataResponse> getMyFiles(FileType fileType, UserPrincipal principal);

    void deleteFile(String uuid, UserPrincipal principal);
}
