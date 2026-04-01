package com.springbootplayground.common.exception;

public abstract class AlreadyExistsException extends BusinessException {

    protected AlreadyExistsException(String message) {
        super(message);
    }
}
