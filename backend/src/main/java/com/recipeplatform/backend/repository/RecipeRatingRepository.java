package com.recipeplatform.backend.repository;

import com.recipeplatform.backend.entity.RecipeRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecipeRatingRepository extends JpaRepository<RecipeRating, Long> {

    Optional<RecipeRating> findByRecipe_IdAndUser_Id(Long recipeId, Long userId);

    @EntityGraph(attributePaths = {"user"})
    Page<RecipeRating> findByRecipe_IdOrderByCreatedAtDesc(Long recipeId, Pageable pageable);

    @Query("SELECT AVG(r.score) FROM RecipeRating r WHERE r.recipe.id = :recipeId")
    Double avgByRecipeId(@Param("recipeId") Long recipeId);

    long countByRecipe_Id(Long recipeId);

    /** 점수(1~5)별 개수 — (score, count) pair 리스트 */
    @Query("SELECT r.score, COUNT(r) FROM RecipeRating r WHERE r.recipe.id = :recipeId GROUP BY r.score")
    List<Object[]> distributionByRecipeId(@Param("recipeId") Long recipeId);

    void deleteByRecipe_IdAndUser_Id(Long recipeId, Long userId);
}
