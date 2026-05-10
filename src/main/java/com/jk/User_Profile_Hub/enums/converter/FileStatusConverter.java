package com.jk.User_Profile_Hub.enums.converter;

import com.jk.User_Profile_Hub.enums.FileStatus;
import com.jk.User_Profile_Hub.enums.FileType;
import com.jk.User_Profile_Hub.exception.custom.ValidationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class FileStatusConverter implements Converter<String, FileStatus> {

    @Override
    public FileStatus convert(@NonNull String source) {
        try{
            return FileStatus.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException e){
            String validValues = Arrays.stream(FileStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            throw new ValidationException(
                    String.format("Invalid File Status value: %s. Valid values are: %s", source, validValues)
            );
        }
    }
}
