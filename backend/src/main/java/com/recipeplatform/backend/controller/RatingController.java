package com.recipeplatform.backend.controller;

import com.recipeplatform.backend.dto.RatingDto;
import com.recipeplatform.backend.security.UserPrincipal;
import com.recipeplatform.backend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recipes/{recipeId}/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PutMapping
    public ResponseEntity<Map<String, Object>> upsert(
            @PathVariable Long recipeId,
            @Valid @RequestBody RatingDto.RatingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        RatingDto.RatingResponse saved = ratingService.upsert(recipeId, principal.getUser(), req);
        RatingDto.RatingAggregate agg = ratingService.aggregate(recipeId, principal.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", saved, "aggregate", agg));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long recipeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ratingService.delete(recipeId, principal.getUser());
        RatingDto.RatingAggregate agg = ratingService.aggregate(recipeId, principal.getId());
        return ResponseEntity.ok(Map.of("success", true, "aggregate", agg));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        Page<RatingDto.RatingResponse> data = ratingService.list(recipeId, page, size);
        RatingDto.RatingAggregate agg = ratingService.aggregate(recipeId, userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data.getContent(),
                "aggregate", agg,
                "page", Map.of(
                        "number", data.getNumber(),
                        "totalPages", data.getTotalPages(),
                        "hasNext", data.hasNext()
                )
        ));
    }
}
