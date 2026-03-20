package com.mawrid.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

        private static final ObjectMapper MAPPER = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

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

                String authHeader = request.getHeader("Authorization");
                String maskedAuth = authHeader != null
                        ? authHeader.substring(0, Math.min(authHeader.length(), 20)) + "..."
                        : "none";

                log.info("""
                        
                        ─── REQUEST ──────────────────────────────────────
                        {} {} [{}ms] → {}
                        Content-Type : {}
                        Authorization: {}
                        Body         :
                        {}
                        ─── RESPONSE ─────────────────────────────────────
                        Status : {}
                        Body   :
                        {}
                        ──────────────────────────────────────────────────""",
                        request.getMethod(),
                        request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                        ms,
                        response.getStatus(),
                        request.getContentType(),
                        maskedAuth,
                        requestBody.isBlank() ? "(empty)" : prettyJson(requestBody),
                        response.getStatus(),
                        truncate(prettyJson(responseBody), 2000)
                );

                wrappedRes.copyBodyToResponse();
            }
        }

        private String prettyJson(String raw) {
            if (raw == null || raw.isBlank()) return "(empty)";
            try {
                Object parsed = MAPPER.readValue(raw, Object.class);
                return MAPPER.writeValueAsString(parsed);
            } catch (Exception e) {
                return raw; // not JSON, return as-is
            }
        }

        private String truncate(String s, int max) {
            if (s == null || s.isBlank()) return "(empty)";
            return s.length() <= max ? s : s.substring(0, max) + "... [truncated]";
        }
    }
}