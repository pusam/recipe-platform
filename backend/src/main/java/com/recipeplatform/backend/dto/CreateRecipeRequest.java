package com.recipeplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRecipeRequest {
    @NotBlank
    private String youtubeUrl;
}
