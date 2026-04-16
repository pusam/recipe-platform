package com.recipeplatform.backend.service;

import com.recipeplatform.backend.dto.RatingDto;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.RecipeRating;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.RecipeRatingRepository;
import com.recipeplatform.backend.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock RecipeRatingRepository ratingRepository;
    @Mock RecipeRepository recipeRepository;

    @InjectMocks RatingService ratingService;

    private User user(long id) {
        return User.builder().id(id).email("u" + id + "@x.com").username("u" + id).build();
    }

    private Recipe recipe(long id) {
        return Recipe.builder()
                .id(id).title("r").youtubeUrl("https://youtu.be/abcdefghijk")
                .ratingCount(0).ingredients(new ArrayList<>()).steps(new ArrayList<>())
                .build();
    }

    @Test
    void upsert_newRating_createsAndUpdatesCache() {
        Recipe r = recipe(1L);
        User u = user(10L);
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(r));
        when(ratingRepository.findByRecipe_IdAndUser_Id(1L, 10L)).thenReturn(Optional.empty());
        when(ratingRepository.save(any())).thenAnswer(inv -> {
            RecipeRating saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(ratingRepository.avgByRecipeId(1L)).thenReturn(4.0);
        when(ratingRepository.countByRecipe_Id(1L)).thenReturn(1L);
        when(recipeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RatingDto.RatingRequest req = new RatingDto.RatingRequest();
        req.setScore(4);
        req.setComment("맛있어요!");

        RatingDto.RatingResponse res = ratingService.upsert(1L, u, req);

        assertThat(res.getScore()).isEqualTo(4);
        assertThat(res.getComment()).isEqualTo("맛있어요!");
        assertThat(r.getAvgScore()).isEqualTo(4.0);
        assertThat(r.getRatingCount()).isEqualTo(1);
    }

    @Test
    void upsert_existingRating_updatesScoreAndCache() {
        Recipe r = recipe(1L);
        User u = user(10L);
        RecipeRating existing = RecipeRating.builder().id(99L).recipe(r).user(u).score(3).build();
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(r));
        when(ratingRepository.findByRecipe_IdAndUser_Id(1L, 10L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ratingRepository.avgByRecipeId(1L)).thenReturn(5.0);
        when(ratingRepository.countByRecipe_Id(1L)).thenReturn(1L);
        when(recipeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RatingDto.RatingRequest req = new RatingDto.RatingRequest();
        req.setScore(5);

        RatingDto.RatingResponse res = ratingService.upsert(1L, u, req);

        assertThat(res.getScore()).isEqualTo(5);
        assertThat(res.getId()).isEqualTo(99L);
    }

    @Test
    void delete_removesAndRefreshesCache() {
        Recipe r = recipe(1L);
        User u = user(10L);
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(r));
        when(ratingRepository.avgByRecipeId(1L)).thenReturn(null);
        when(ratingRepository.countByRecipe_Id(1L)).thenReturn(0L);
        when(recipeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ratingService.delete(1L, u);

        verify(ratingRepository).deleteByRecipe_IdAndUser_Id(1L, 10L);
        assertThat(r.getRatingCount()).isEqualTo(0);
        assertThat(r.getAvgScore()).isNull();
    }

    @Test
    void upsert_missingRecipe_throws() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());
        RatingDto.RatingRequest req = new RatingDto.RatingRequest();
        req.setScore(5);

        assertThatThrownBy(() -> ratingService.upsert(999L, user(1L), req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    void aggregate_returnsDistributionAndMyRating() {
        Recipe r = recipe(1L);
        User u = user(10L);
        RecipeRating mine = RecipeRating.builder().id(50L).recipe(r).user(u).score(5).comment("최고").build();

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(r));
        when(ratingRepository.avgByRecipeId(1L)).thenReturn(4.5);
        when(ratingRepository.countByRecipe_Id(1L)).thenReturn(10L);
        when(ratingRepository.distributionByRecipeId(1L)).thenReturn(List.of(
                new Object[]{4, 5L},
                new Object[]{5, 5L}
        ));
        when(ratingRepository.findByRecipe_IdAndUser_Id(1L, 10L)).thenReturn(Optional.of(mine));

        RatingDto.RatingAggregate agg = ratingService.aggregate(1L, 10L);

        assertThat(agg.getAverageScore()).isEqualTo(4.5);
        assertThat(agg.getRatingCount()).isEqualTo(10);
        assertThat(agg.getDistribution()).containsEntry(4, 5L);
        assertThat(agg.getDistribution()).containsEntry(5, 5L);
        assertThat(agg.getDistribution()).containsEntry(1, 0L);
        assertThat(agg.getMyRating()).isNotNull();
        assertThat(agg.getMyRating().getScore()).isEqualTo(5);
    }
}
