package io.myforevermusic.api.common.error;

import io.myforevermusic.api.modules.auth.application.AuthEmailAlreadyRegisteredException;
import io.myforevermusic.api.modules.auth.application.AuthInvalidCredentialsException;
import io.myforevermusic.api.modules.platform.application.PlatformProviderOperationException;
import io.myforevermusic.api.modules.platform.application.PlatformReconnectRequiredException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthEmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailConflict(AuthEmailAlreadyRegisteredException exception) {
        return buildResponse(HttpStatus.CONFLICT, null, exception.getMessage(), List.of());
    }

    @ExceptionHandler(AuthInvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(AuthInvalidCredentialsException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "invalid_credentials", exception.getMessage(), List.of());
    }

    @ExceptionHandler(ApiResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ApiResourceNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, null, exception.getMessage(), List.of());
    }

    @ExceptionHandler(PlatformReconnectRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handlePlatformReconnectRequired(PlatformReconnectRequiredException exception) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "platform_reconnect_required",
            exception.getMessage(),
            List.of()
        );
    }

    @ExceptionHandler(PlatformProviderOperationException.class)
    public ResponseEntity<ApiErrorResponse> handlePlatformProviderOperation(PlatformProviderOperationException exception) {
        return buildResponse(
            HttpStatus.BAD_GATEWAY,
            "platform_provider_operation_failed",
            exception.getMessage(),
            List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldIssue> issues = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldIssue)
            .toList();

        String message = issues.isEmpty()
            ? "Validation failed for request body."
            : issues.getFirst().message();

        return buildResponse(HttpStatus.BAD_REQUEST, null, message, issues);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, null, exception.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, null, "Request body could not be read.", List.of());
    }

    private ApiErrorResponse.FieldIssue toFieldIssue(FieldError error) {
        return new ApiErrorResponse.FieldIssue(
            error.getField(),
            error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String code,
        String message,
        List<ApiErrorResponse.FieldIssue> issues
    ) {
        return ResponseEntity.status(status).body(
            new ApiErrorResponse(
                "api",
                "error",
                code,
                message,
                Instant.now(),
                issues.isEmpty() ? null : issues
            )
        );
    }
}
