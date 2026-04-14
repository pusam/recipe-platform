package com.recipeplatform.backend.service;

import com.recipeplatform.backend.entity.FavoriteRecipe;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.FavoriteRecipeRepository;
import com.recipeplatform.backend.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRecipeRepository favoriteRepository;
    private final RecipeRepository recipeRepository;

    @Transactional
    public boolean add(User user, Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + recipeId));
        if (favoriteRepository.existsByUserAndRecipe(user, recipe)) {
            return false;
        }
        favoriteRepository.save(FavoriteRecipe.builder().user(user).recipe(recipe).build());
        return true;
    }

    @Transactional
    public void remove(User user, Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + recipeId));
        favoriteRepository.deleteByUserAndRecipe(user, recipe);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(User user, Long recipeId) {
        return recipeRepository.findById(recipeId)
                .map(r -> favoriteRepository.existsByUserAndRecipe(user, r))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Recipe> listFavorites(User user) {
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(FavoriteRecipe::getRecipe).toList();
    }
}
