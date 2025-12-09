package org.example.userservice.controller;

import jakarta.validation.Valid;
import org.example.userservice.dto.SendCodeRequest;
import org.example.userservice.dto.VerifyCodeRequest;
import org.example.userservice.service.EmailVerificationService;
import org.example.userservice.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(@Valid @RequestBody SendCodeRequest request) {
        emailVerificationService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Code de vérification envoyé"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        boolean isValid = emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Email vérifié avec succès", isValid));
    }

    @GetMapping("/is-verified")
    public ResponseEntity<ApiResponse<Boolean>> isEmailVerified(@RequestParam String email) {
        boolean isVerified = emailVerificationService.isEmailVerified(email);
        return ResponseEntity.ok(ApiResponse.success(isVerified));
    }
}
