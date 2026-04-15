package com.maritime.platform.common.web.advice;

import com.maritime.platform.common.core.result.R;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Injects traceId and request path into every {@link R} response
 * body — both success and error paths.
 *
 * <p>Reads traceId from SLF4J MDC (set by TraceIdFilter).
 * This ensures all JSON responses carry the trace ID for
 * end-to-end request correlation.
 */
@RestControllerAdvice
public class TraceIdResponseAdvice
        implements ResponseBodyAdvice<Object> {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(
            @Nullable Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {
        if (body instanceof R<?> r) {
            if (r.getTraceId() == null) {
                r.setTraceId(MDC.get(TRACE_ID_KEY));
            }
            if (r.getPath() == null) {
                r.setPath(request.getURI().getPath());
            }
        }
        return body;
    }
}
