package org.example.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.userservice.dto.*;
import org.example.userservice.service.AuthService;
import org.example.userservice.service.ForgotPasswordService;
import org.example.userservice.service.RefreshTokenService;
import org.example.userservice.utils.ApiResponse;
import org.example.userservice.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        
        AuthResponse authResponse = authService.register(request);
        
        // Set access token in cookie
        CookieUtils.addCookie(response, "access_token", authResponse.getToken(), 86400, true);
        
        return ResponseEntity.ok(ApiResponse.success("Inscription réussie", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        
        AuthResponse authResponse = authService.login(request);
        
        // Set access token in cookie
        CookieUtils.addCookie(response, "access_token", authResponse.getToken(), 86400, true);
        
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        if (authentication != null) {
            authService.logout(authentication.getName());
        }
        
        // Delete cookies
        CookieUtils.deleteCookie(request, response, "access_token");
        CookieUtils.deleteCookie(request, response, "refresh_token");
        
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse response) {
        
        RefreshTokenResponse tokenResponse = authService.refreshToken(request.getRefreshToken());
        
        // Update access token cookie
        CookieUtils.addCookie(response, "access_token", tokenResponse.getAccessToken(), 86400, true);
        
        return ResponseEntity.ok(ApiResponse.success("Token rafraîchi", tokenResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(Authentication authentication) {
        UserDTO user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgetPasswordRequest request) {
        forgotPasswordService.sendPasswordResetEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Email de réinitialisation envoyé"));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<ApiResponse<Void>> validateResetToken(@RequestParam String token) {
        forgotPasswordService.validateResetToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token valide"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request) {
        
        forgotPasswordService.resetPassword(token, request);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe réinitialisé avec succès"));
    }
}
