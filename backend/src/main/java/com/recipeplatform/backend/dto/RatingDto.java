package com.recipeplatform.backend.dto;

import com.recipeplatform.backend.entity.RecipeRating;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class RatingDto {

    @Data
    public static class RatingRequest {
        @NotNull
        @Min(1) @Max(5)
        private Integer score;

        @Size(max = 2000)
        private String comment;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RatingResponse {
        private Long id;
        private Integer score;
        private String comment;
        private Long userId;
        private String username;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static RatingResponse from(RecipeRating r) {
            return RatingResponse.builder()
                    .id(r.getId())
                    .score(r.getScore())
                    .comment(r.getComment())
                    .userId(r.getUser().getId())
                    .username(r.getUser().getUsername())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RatingAggregate {
        private Double averageScore;
        private Integer ratingCount;
        /** key: 점수(1~5), value: 해당 점수 개수 */
        private Map<Integer, Long> distribution;
        /** 현재 로그인 사용자의 평점 (없으면 null) */
        private RatingResponse myRating;
    }
}
