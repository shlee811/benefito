package com.lion.benefito.global.exception;

import com.lion.benefito.domain.code.CodeException;
import com.lion.benefito.domain.posting.PostingException;
import com.lion.benefito.domain.posting.PostingNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CodeException.class)
    public ResponseEntity<String> handleCodeException(CodeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(PostingException.class)
    public ResponseEntity<String> handlePostingException(PostingException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(PostingNotFoundException.class)
    public ResponseEntity<String> handlePostingNotFoundException(PostingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
