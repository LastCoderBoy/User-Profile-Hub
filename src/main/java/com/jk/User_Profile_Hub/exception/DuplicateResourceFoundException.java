package com.jk.User_Profile_Hub.exception;

import java.io.Serial;

public class DuplicateResourceFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateResourceFoundException(String message) {
        super(message);
    }
    public DuplicateResourceFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
