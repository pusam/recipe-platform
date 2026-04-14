package com.recipeplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRecipeRequest {
    @NotBlank
    private String title;
    private String summary;
    private String servings;
    private String cookingTime;
    private String difficulty;
    private String tags;
    private List<IngredientInput> ingredients;
    private List<StepInput> steps;

    @Data
    public static class IngredientInput {
        private String name;
        private String amount;
    }

    @Data
    public static class StepInput {
        private Integer stepNo;
        private String instruction;
        private String tip;
    }
}
