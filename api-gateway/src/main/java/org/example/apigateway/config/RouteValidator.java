package org.example.apigateway.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // List of public endpoints that don't require authentication
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/verify",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/email/send-verification",
            "/api/file/share/public",
            "/api/file/share/access",
            "/eureka",
            "/actuator"
    );

    public Predicate<String> isSecured =
            path -> openApiEndpoints.stream()
                    .noneMatch(uri -> path.contains(uri));
}
