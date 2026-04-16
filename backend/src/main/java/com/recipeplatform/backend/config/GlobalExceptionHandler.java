package com.recipeplatform.backend.config;

import com.recipeplatform.backend.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, RecipeService.AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(AuthenticationException e) {
        return body(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }

    @ExceptionHandler(RecipeService.RecipeProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleRecipeProcessing(RecipeService.RecipeProcessingException e) {
        log.warn("레시피 처리 실패: {}", e.getMessage(), e);
        return body(HttpStatus.BAD_GATEWAY, "레시피를 생성하지 못했습니다. 잠시 후 다시 시도해주세요.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message == null ? status.getReasonPhrase() : message);
        return ResponseEntity.status(status).body(body);
    }
}
