package vn.datve.dat_ve.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException exception
    ) {

        ApiErrorResponse response = new ApiErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                Instant.now()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }
}