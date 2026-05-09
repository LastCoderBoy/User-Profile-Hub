package com.jk.User_Profile_Hub.exception;

import java.io.Serial;

public class InternalServerException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InternalServerException(String message) {
        super(message);
    }
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
