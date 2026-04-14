package com.recipeplatform.backend.controller;

import com.recipeplatform.backend.dto.RecipeDto;
import com.recipeplatform.backend.security.UserPrincipal;
import com.recipeplatform.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/users/me/favorites")
    public ResponseEntity<Map<String, Object>> myFavorites(@AuthenticationPrincipal UserPrincipal principal) {
        List<RecipeDto> data = favoriteService.listFavorites(principal.getUser()).stream()
                .map(RecipeDto::summary)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/favorites/{recipeId}")
    public ResponseEntity<Map<String, Object>> isFavorite(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable Long recipeId) {
        boolean fav = favoriteService.isFavorite(principal.getUser(), recipeId);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("favorite", fav)));
    }

    @PostMapping("/favorites/{recipeId}")
    public ResponseEntity<Map<String, Object>> add(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long recipeId) {
        favoriteService.add(principal.getUser(), recipeId);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("favorite", true)));
    }

    @DeleteMapping("/favorites/{recipeId}")
    public ResponseEntity<Map<String, Object>> remove(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long recipeId) {
        favoriteService.remove(principal.getUser(), recipeId);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("favorite", false)));
    }
}
