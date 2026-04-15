package com.maritime.platform.common.web.handler;

import com.maritime.platform.common.core.exception.BusinessException;
import com.maritime.platform.common.core.result.R;
import com.maritime.platform.common.core.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized exception handler that translates exceptions into unified
 * {@link R} responses.
 *
 * <p>Every error response includes:
 * <ul>
 *   <li>{@code traceId} — distributed trace correlation ID from MDC</li>
 *   <li>{@code path} — the request URI that triggered the error</li>
 *   <li>{@code timestamp} — ISO-8601 timestamp (set by {@link R} constructor)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_KEY = "traceId";

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Object> handleBusiness(BusinessException ex,
                                    HttpServletRequest request) {
        LOG.warn("Business exception: code={}, message={}",
                ex.getCode(), ex.getMessage());
        R<Object> result = R.fail(ex.getCode(), ex.getMessage());
        if (ex.getData() != null) {
            result.setData(ex.getData());
        }
        result.setTraceId(MDC.get(TRACE_ID_KEY));
        result.setPath(request.getRequestURI());
        return result;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidation(MethodArgumentNotValidException ex,
                                    HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        LOG.warn("Validation failed: {}", detail);
        return buildError(ResultCode.PARAM_INVALID, detail, request);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBind(BindException ex,
                              HttpServletRequest request) {
        String detail = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        LOG.warn("Bind exception: {}", detail);
        return buildError(ResultCode.PARAM_INVALID, detail, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        LOG.warn("Missing parameter: {}", ex.getParameterName());
        return buildError(ResultCode.PARAM_INVALID,
                ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String detail = "Parameter '" + ex.getName()
                + "' type mismatch: expected "
                + getSimpleTypeName(ex) + "";
        LOG.warn("Type mismatch: {}", detail);
        return buildError(ResultCode.PARAM_INVALID, detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        LOG.warn("Message not readable: {}", ex.getMessage());
        return buildError(ResultCode.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        LOG.warn("Method not allowed: {}", ex.getMethod());
        return buildError(ResultCode.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R<Void> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        LOG.warn("Media type not supported: {}", ex.getContentType());
        return buildError(ResultCode.BAD_REQUEST,
                "Unsupported media type: " + ex.getContentType(),
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNotFound(NoResourceFoundException ex,
                                  HttpServletRequest request) {
        return buildError(ResultCode.NOT_FOUND, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleUnknown(Exception ex,
                                 HttpServletRequest request) {
        LOG.error("Unhandled exception at {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return buildError(ResultCode.INTERNAL_ERROR, request);
    }

    /**
     * Builds error response with code, default message, trace, and path.
     */
    private static R<Void> buildError(@NonNull ResultCode code,
                                      @NonNull HttpServletRequest request) {
        R<Void> result = R.fail(code);
        result.setTraceId(MDC.get(TRACE_ID_KEY));
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * Builds error response with code, custom detail, trace, and path.
     */
    private static R<Void> buildError(@NonNull ResultCode code,
                                      @NonNull String detail,
                                      @NonNull HttpServletRequest request) {
        R<Void> result = R.fail(code, detail);
        result.setTraceId(MDC.get(TRACE_ID_KEY));
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * Builds error response with numeric code, message, trace, and path.
     */
    private static R<Void> buildError(int code,
                                      @NonNull String message,
                                      @NonNull HttpServletRequest request) {
        R<Void> result = R.fail(code, message);
        result.setTraceId(MDC.get(TRACE_ID_KEY));
        result.setPath(request.getRequestURI());
        return result;
    }

    private static String getSimpleTypeName(
            MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        return requiredType != null
                ? requiredType.getSimpleName() : "unknown";
    }
}
