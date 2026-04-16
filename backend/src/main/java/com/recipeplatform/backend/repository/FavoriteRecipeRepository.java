package com.recipeplatform.backend.repository;

import com.recipeplatform.backend.entity.FavoriteRecipe;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRecipeRepository extends JpaRepository<FavoriteRecipe, Long> {
    Optional<FavoriteRecipe> findByUserAndRecipe(User user, Recipe recipe);

    @EntityGraph(attributePaths = {"recipe", "recipe.creator"})
    List<FavoriteRecipe> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndRecipe(User user, Recipe recipe);
    void deleteByUserAndRecipe(User user, Recipe recipe);
}
