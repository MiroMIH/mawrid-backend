package com.mawrid.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public RequestResponseLoggingFilter requestResponseLoggingFilter() {
        return new RequestResponseLoggingFilter();
    }

    @Slf4j
    public static class RequestResponseLoggingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain chain
        ) throws ServletException, IOException {

            ContentCachingRequestWrapper wrappedReq = new ContentCachingRequestWrapper(request);
            ContentCachingResponseWrapper wrappedRes = new ContentCachingResponseWrapper(response);

            long start = System.currentTimeMillis();

            try {
                chain.doFilter(wrappedReq, wrappedRes);
            } finally {
                long ms = System.currentTimeMillis() - start;

                String requestBody = new String(wrappedReq.getContentAsByteArray(), StandardCharsets.UTF_8);
                String responseBody = new String(wrappedRes.getContentAsByteArray(), StandardCharsets.UTF_8);

                // Mask Authorization header value
                String authHeader = request.getHeader("Authorization");
                String maskedAuth = authHeader != null
                        ? authHeader.substring(0, Math.min(authHeader.length(), 20)) + "..."
                        : "none";

                log.info("""
                        ─── REQUEST ──────────────────────────────────────
                        {} {} [{}ms] → {}
                        Content-Type : {}
                        Authorization: {}
                        Body         : {}
                        ─── RESPONSE ─────────────────────────────────────
                        Status : {}
                        Body   : {}
                        ──────────────────────────────────────────────────""",
                        request.getMethod(),
                        request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                        ms,
                        response.getStatus(),
                        request.getContentType(),
                        maskedAuth,
                        requestBody.isBlank() ? "(empty)" : requestBody,
                        response.getStatus(),
                        truncate(responseBody, 500)
                );

                wrappedRes.copyBodyToResponse();
            }
        }

        private String truncate(String s, int max) {
            if (s == null || s.isBlank()) return "(empty)";
            return s.length() <= max ? s : s.substring(0, max) + "... [truncated]";
        }
    }
}
