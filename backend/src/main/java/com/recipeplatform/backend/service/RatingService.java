package com.recipeplatform.backend.service;

import com.recipeplatform.backend.dto.RatingDto;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.RecipeRating;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.RecipeRatingRepository;
import com.recipeplatform.backend.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RecipeRatingRepository ratingRepository;
    private final RecipeRepository recipeRepository;

    @Transactional
    public RatingDto.RatingResponse upsert(Long recipeId, User user, RatingDto.RatingRequest req) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + recipeId));

        RecipeRating rating = ratingRepository.findByRecipe_IdAndUser_Id(recipeId, user.getId())
                .orElseGet(() -> RecipeRating.builder()
                        .recipe(recipe)
                        .user(user)
                        .build());

        rating.setScore(req.getScore());
        rating.setComment(req.getComment());
        ratingRepository.save(rating);

        refreshAggregate(recipe);
        return RatingDto.RatingResponse.from(rating);
    }

    @Transactional
    public void delete(Long recipeId, User user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + recipeId));

        ratingRepository.deleteByRecipe_IdAndUser_Id(recipeId, user.getId());
        refreshAggregate(recipe);
    }

    @Transactional(readOnly = true)
    public RatingDto.RatingAggregate aggregate(Long recipeId, Long currentUserId) {
        recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + recipeId));

        Double avg = ratingRepository.avgByRecipeId(recipeId);
        long count = ratingRepository.countByRecipe_Id(recipeId);

        Map<Integer, Long> dist = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) dist.put(i, 0L);
        for (Object[] row : ratingRepository.distributionByRecipeId(recipeId)) {
            dist.put((Integer) row[0], (Long) row[1]);
        }

        RatingDto.RatingResponse my = null;
        if (currentUserId != null) {
            my = ratingRepository.findByRecipe_IdAndUser_Id(recipeId, currentUserId)
                    .map(RatingDto.RatingResponse::from)
                    .orElse(null);
        }

        return RatingDto.RatingAggregate.builder()
                .averageScore(avg)
                .ratingCount((int) count)
                .distribution(dist)
                .myRating(my)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<RatingDto.RatingResponse> list(Long recipeId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ratingRepository.findByRecipe_IdOrderByCreatedAtDesc(recipeId, pageable)
                .map(RatingDto.RatingResponse::from);
    }

    private void refreshAggregate(Recipe recipe) {
        Double avg = ratingRepository.avgByRecipeId(recipe.getId());
        long count = ratingRepository.countByRecipe_Id(recipe.getId());
        recipe.setAvgScore(avg);
        recipe.setRatingCount((int) count);
        recipeRepository.save(recipe);
    }
}
