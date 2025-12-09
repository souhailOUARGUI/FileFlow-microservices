package org.example.fileservice.controller;

import org.example.fileservice.dto.FileDTO;
import org.example.fileservice.service.FileService;
import org.example.fileservice.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favourites")
@RequiredArgsConstructor
@Tag(name = "Favourites", description = "File favourites management APIs")
public class FavouritesController {

    private final FileService fileService;

    @GetMapping
    @Operation(summary = "Get user's favourite files")
    public ResponseEntity<ApiResponse<List<FileDTO>>> getFavouriteFiles(
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileDTO> favourites = fileService.getFavoriteFiles(userId);
            return ResponseEntity.ok(ApiResponse.success("Favourite files retrieved successfully", favourites));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}")
    @Operation(summary = "Toggle file favourite status")
    public ResponseEntity<ApiResponse<FileDTO>> toggleFavourite(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileDTO file = fileService.toggleFavorite(id, userId);
            String message = file.getIsFavorite() ? "File added to favourites" : "File removed from favourites";
            return ResponseEntity.ok(ApiResponse.success(message, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
