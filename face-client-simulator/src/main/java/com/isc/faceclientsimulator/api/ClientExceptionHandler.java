package com.isc.faceclientsimulator.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ClientExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse badRequest(IllegalArgumentException ex){return new ErrorResponse("BAD_REQUEST",ex.getMessage());}
    @ExceptionHandler(IllegalStateException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> state(IllegalStateException ex){return org.springframework.http.ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse("PROCESSING_ERROR",ex.getMessage()));}
    public record ErrorResponse(String code,String message){}
}
