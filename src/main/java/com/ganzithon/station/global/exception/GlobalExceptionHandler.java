package com.ganzithon.station.global.exception;

import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * REST 예외를 모두 {@link ApiResponse} 실패 형식으로 바꿉니다.
 * 각 도메인 코드에서는 try-catch로 잡지 말고 {@link BusinessException}을 던지기만 합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.info("BusinessException: {}", errorCode.getCode());
        return toResponse(errorCode, ErrorResponse.of(errorCode));
    }

    /** 요청 DTO의 @Valid 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotValid(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return invalidInput(fieldErrors);
    }

    /** @PathVariable, @RequestParam 등에 붙인 검증 어노테이션 실패 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorResponse.FieldError(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return invalidInput(fieldErrors);
    }

    /** 타입이 안 맞는 path variable·쿼리 파라미터 (예: /rooms/abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return invalidInput(List.of(new ErrorResponse.FieldError(e.getName(), "형식이 올바르지 않습니다.")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        return invalidInput(List.of(new ErrorResponse.FieldError(e.getParameterName(), "필수 값입니다.")));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        return invalidInput(List.of(new ErrorResponse.FieldError(e.getHeaderName(), "필수 헤더입니다.")));
    }

    /** body가 없거나 JSON 형식·enum 값이 잘못됨 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.info("요청 body를 읽을 수 없음: {}", e.getMessage());
        return toResponse(ErrorCode.COMMON_INVALID_INPUT, ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return toResponse(ErrorCode.COMMON_NOT_FOUND, ErrorResponse.of(ErrorCode.COMMON_NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.COMMON_METHOD_NOT_ALLOWED, ErrorResponse.of(ErrorCode.COMMON_METHOD_NOT_ALLOWED));
    }

    /** 위에서 처리하지 못한 예외 = 서버 버그. 내용은 로그로만 남기고 클라이언트에는 숨깁니다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return toResponse(ErrorCode.COMMON_INTERNAL_ERROR, ErrorResponse.of(ErrorCode.COMMON_INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> invalidInput(List<ErrorResponse.FieldError> fieldErrors) {
        return toResponse(ErrorCode.COMMON_INVALID_INPUT, ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT, fieldErrors));
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, ErrorResponse error) {
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(error));
    }
}
