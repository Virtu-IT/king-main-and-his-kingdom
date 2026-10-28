package com.bnp.kingmainandhiskingdom.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final HttpCatImages images;

    public GlobalExceptionHandler(HttpCatImages images) {
        this.images = images;
    }

    @ExceptionHandler(PersonNotFoundException.class)
    public ResponseEntity<byte[]> handlePersonNotFoundException(PersonNotFoundException ex) {
        return images.response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<byte[]> handleUserNotFoundException(UserNotFoundException ex) {
        return images.response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<byte[]> handleAlreadyExistsException(AlreadyExistsException ex) {
        return images.response(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<byte[]> handleIllegalArgumentException(IllegalArgumentException ex) {
        return images.response(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.IMAGE_JPEG);
        responseHeaders.add(HttpCatImages.ERROR_MESSAGE_HEADER, message.replaceAll("[^\\x20-\\x7E]", " "));
        return new ResponseEntity<>(images.imageFor(status.value()), responseHeaders, status);
    }
}
