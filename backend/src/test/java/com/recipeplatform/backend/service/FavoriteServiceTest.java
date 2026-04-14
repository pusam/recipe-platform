package com.recipeplatform.backend.service;

import com.recipeplatform.backend.entity.FavoriteRecipe;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.FavoriteRecipeRepository;
import com.recipeplatform.backend.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock FavoriteRecipeRepository favoriteRepository;
    @Mock RecipeRepository recipeRepository;

    @InjectMocks FavoriteService favoriteService;

    User user = User.builder().id(1L).email("u@u.u").username("u").build();
    Recipe recipe = Recipe.builder().id(42L).title("t").build();

    @Test
    void add_savesNew() {
        when(recipeRepository.findById(42L)).thenReturn(Optional.of(recipe));
        when(favoriteRepository.existsByUserAndRecipe(user, recipe)).thenReturn(false);

        boolean added = favoriteService.add(user, 42L);

        assertThat(added).isTrue();
        verify(favoriteRepository).save(any(FavoriteRecipe.class));
    }

    @Test
    void add_skipsIfAlreadyFavorited() {
        when(recipeRepository.findById(42L)).thenReturn(Optional.of(recipe));
        when(favoriteRepository.existsByUserAndRecipe(user, recipe)).thenReturn(true);

        boolean added = favoriteService.add(user, 42L);

        assertThat(added).isFalse();
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void add_throws_onMissingRecipe() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.add(user, 999L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
