package com.recipeplatform.backend.dto;

import com.recipeplatform.backend.entity.Ingredient;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.RecipeStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDto {
    private Long id;
    private String youtubeUrl;
    private String videoId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String channelName;
    private String summary;
    private String servings;
    private String cookingTime;
    private String difficulty;
    private String tags;
    private List<IngredientDto> ingredients;
    private List<StepDto> steps;
    private LocalDateTime createdAt;
    private Long creatorId;
    private String creatorName;
    private Double avgScore;
    private Integer ratingCount;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngredientDto {
        private String name;
        private String amount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StepDto {
        private Integer stepNo;
        private String instruction;
        private String tip;
    }

    public static RecipeDto from(Recipe r) {
        return RecipeDto.builder()
                .id(r.getId())
                .youtubeUrl(r.getYoutubeUrl())
                .videoId(r.getVideoId())
                .title(r.getTitle())
                .description(r.getDescription())
                .thumbnailUrl(r.getThumbnailUrl())
                .channelName(r.getChannelName())
                .summary(r.getSummary())
                .servings(r.getServings())
                .cookingTime(r.getCookingTime())
                .difficulty(r.getDifficulty())
                .tags(r.getTags())
                .ingredients(r.getIngredients().stream().map(RecipeDto::toIng).toList())
                .steps(r.getSteps().stream().map(RecipeDto::toStep).toList())
                .createdAt(r.getCreatedAt())
                .creatorId(r.getCreator() == null ? null : r.getCreator().getId())
                .creatorName(r.getCreator() == null ? null : r.getCreator().getUsername())
                .avgScore(r.getAvgScore())
                .ratingCount(r.getRatingCount())
                .build();
    }

    public static RecipeDto summary(Recipe r) {
        return RecipeDto.builder()
                .id(r.getId())
                .youtubeUrl(r.getYoutubeUrl())
                .videoId(r.getVideoId())
                .title(r.getTitle())
                .thumbnailUrl(r.getThumbnailUrl())
                .channelName(r.getChannelName())
                .cookingTime(r.getCookingTime())
                .difficulty(r.getDifficulty())
                .tags(r.getTags())
                .createdAt(r.getCreatedAt())
                .creatorId(r.getCreator() == null ? null : r.getCreator().getId())
                .creatorName(r.getCreator() == null ? null : r.getCreator().getUsername())
                .avgScore(r.getAvgScore())
                .ratingCount(r.getRatingCount())
                .build();
    }

    private static IngredientDto toIng(Ingredient i) {
        return IngredientDto.builder().name(i.getName()).amount(i.getAmount()).build();
    }

    private static StepDto toStep(RecipeStep s) {
        return StepDto.builder().stepNo(s.getStepNo()).instruction(s.getInstruction()).tip(s.getTip()).build();
    }
}
