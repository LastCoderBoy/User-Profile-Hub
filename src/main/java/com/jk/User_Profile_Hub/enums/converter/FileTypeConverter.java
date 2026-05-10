package com.jk.User_Profile_Hub.enums.converter;


import com.jk.User_Profile_Hub.enums.FileType;
import com.jk.User_Profile_Hub.exception.custom.ValidationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class FileTypeConverter implements Converter<String, FileType> {

    @Override
    public FileType convert(@NonNull String source) {
        try{
            return FileType.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException e){
            String validValues = Arrays.stream(FileType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            throw new ValidationException(
                    String.format("Invalid File Type value: %s. Valid values are: %s", source, validValues)
            );
        }
    }
}
