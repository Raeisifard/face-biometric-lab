package com.isc.facebiometricservice.api;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class) public org.springframework.http.ResponseEntity<ErrorResponse> bad(IllegalArgumentException e){return org.springframework.http.ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST",e.getMessage()));}
  public record ErrorResponse(String code,String message){}
}
