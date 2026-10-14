package com.policyflow.claimsservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PolicyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePolicyNotFound(
            PolicyNotFoundException exception,
            HttpServletRequest request) {

        return new ErrorResponse(
                404,
                "Policy Not Found",
                exception.getMessage(),
                request.getRequestURI()
        );
    }
}