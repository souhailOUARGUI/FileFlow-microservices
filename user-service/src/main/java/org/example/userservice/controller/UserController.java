package org.example.userservice.controller;

import jakarta.validation.Valid;
import org.example.userservice.dto.UserResponseDTO;
import org.example.userservice.dto.UserStorageInfo;
import org.example.userservice.dto.UserUpdateRequest;
import org.example.userservice.service.ProfileService;
import org.example.userservice.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getProfile(Authentication authentication) {
        UserResponseDTO profile = profileService.getProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getProfileById(@PathVariable Long userId) {
        UserResponseDTO profile = profileService.getProfileById(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request) {
        
        UserResponseDTO updatedProfile = profileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Profil mis à jour", updatedProfile));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(Authentication authentication) {
        profileService.deleteAccount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Compte supprimé avec succès"));
    }

    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<UserStorageInfo>> getStorageInfo(Authentication authentication) {
        UserStorageInfo storageInfo = profileService.getStorageInfo(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Storage information retrieved successfully", storageInfo));
    }

    // Internal endpoints for inter-service communication
    @PutMapping("/internal/{userId}/storage")
    public ResponseEntity<ApiResponse<Void>> updateStorageUsed(
            @PathVariable Long userId,
            @RequestParam Long sizeChange) {
        
        profileService.updateStorageUsed(userId, sizeChange);
        return ResponseEntity.ok(ApiResponse.success("Storage updated"));
    }

    @GetMapping("/internal/{userId}/storage/check")
    public ResponseEntity<ApiResponse<Boolean>> hasStorageSpace(
            @PathVariable Long userId,
            @RequestParam Long fileSize) {
        
        boolean hasSpace = profileService.hasStorageSpace(userId, fileSize);
        return ResponseEntity.ok(ApiResponse.success(hasSpace));
    }
}
