package com.jk.User_Profile_Hub.exception;

import java.io.Serial;

public class InvalidTokenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidTokenException(String message) {
        super(message);
    }
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
