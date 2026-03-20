package com.springbootplayground.common.exception;

public abstract class NotFoundException extends BusinessException {

    protected NotFoundException(String message) {
        super(message);
    }
}
