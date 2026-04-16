package com.recipeplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRecipeRequest {
    @NotBlank
    @Size(max = 500)
    @Pattern(
            regexp = "^https?://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?v=|shorts/|embed/)|youtu\\.be/)[A-Za-z0-9_-]{11}(?:[?&#].*)?$",
            message = "유효한 YouTube URL이 아닙니다."
    )
    private String youtubeUrl;
}
