package org.example.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.example.apigateway.config.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Filter to add user information from JWT to request headers
 * for downstream microservices
 */
@Component
public class UserHeaderFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserHeaderFilter.class);

    private final JwtUtil jwtUtil;

    public UserHeaderFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    Long userId = jwtUtil.extractUserId(token);

                    // Create a wrapper that adds custom headers
                    HttpServletRequest wrappedRequest = new HeaderMapRequestWrapper(request);
                    ((HeaderMapRequestWrapper) wrappedRequest).addHeader("X-User-Id", String.valueOf(userId));
                    ((HeaderMapRequestWrapper) wrappedRequest).addHeader("X-User-Email", username);

                    log.debug("Added user headers: X-User-Id={}, X-User-Email={}", userId, username);
                    filterChain.doFilter(wrappedRequest, response);
                    return;
                }
            } catch (Exception e) {
                log.debug("Could not extract user info from token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    // Custom request wrapper to add headers
    private static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = customHeaders.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>(customHeaders.keySet());
            Enumeration<String> originalNames = super.getHeaderNames();
            while (originalNames.hasMoreElements()) {
                names.add(originalNames.nextElement());
            }
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (customHeaders.containsKey(name)) {
                return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
            }
            return super.getHeaders(name);
        }
    }
}
