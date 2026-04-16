package com.recipeplatform.backend.controller;

import com.recipeplatform.backend.dto.CreateRecipeRequest;
import com.recipeplatform.backend.dto.RecipeDto;
import com.recipeplatform.backend.dto.UpdateRecipeRequest;
import com.recipeplatform.backend.security.UserPrincipal;
import com.recipeplatform.backend.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RecipeDto> data = recipeService.search(q, tag, page, size);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data.getContent(),
                "page", Map.of(
                        "number", data.getNumber(),
                        "size", data.getSize(),
                        "totalElements", data.getTotalElements(),
                        "totalPages", data.getTotalPages(),
                        "hasNext", data.hasNext()
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", recipeService.findDtoById(id)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateRecipeRequest req,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        RecipeDto saved = recipeService.createFromYoutube(req.getYoutubeUrl(), principal.getUser());
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateRecipeRequest req,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        RecipeDto updated = recipeService.update(id, req, principal.getUser());
        return ResponseEntity.ok(Map.of("success", true, "data", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        recipeService.delete(id, principal.getUser());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
