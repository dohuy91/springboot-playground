package com.springbootplayground.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.springbootplayground.common.clock.ClockProvider;
import com.springbootplayground.common.exception.AlreadyExistsException;
import com.springbootplayground.common.exception.BusinessException;
import com.springbootplayground.common.exception.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ClockProvider clockProvider;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = mapStatus(exception);
        ErrorResponse response = new ErrorResponse(
                clockProvider.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus mapStatus(BusinessException exception) {
        return switch (exception) {
            case NotFoundException _ -> HttpStatus.NOT_FOUND;
            case AlreadyExistsException _ -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
