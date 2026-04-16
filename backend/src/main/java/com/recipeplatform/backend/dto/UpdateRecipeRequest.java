package com.recipeplatform.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRecipeRequest {
    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 2000)
    private String summary;

    @Size(max = 50)
    private String servings;

    @Size(max = 50)
    private String cookingTime;

    @Size(max = 20)
    private String difficulty;

    @Size(max = 500)
    private String tags;

    @Size(max = 200, message = "재료는 최대 200개까지 가능합니다.")
    private List<@Valid IngredientInput> ingredients;

    @Size(max = 200, message = "조리 단계는 최대 200개까지 가능합니다.")
    private List<@Valid StepInput> steps;

    @Data
    public static class IngredientInput {
        @Size(max = 200)
        private String name;
        @Size(max = 100)
        private String amount;
    }

    @Data
    public static class StepInput {
        private Integer stepNo;
        @Size(max = 5000)
        private String instruction;
        @Size(max = 2000)
        private String tip;
    }
}
