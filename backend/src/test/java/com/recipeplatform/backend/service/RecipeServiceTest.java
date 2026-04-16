package com.recipeplatform.backend.service;

import com.recipeplatform.backend.dto.UpdateRecipeRequest;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock RecipeRepository recipeRepository;
    @Mock YouTubeService youTubeService;
    @Mock GeminiService geminiService;

    @InjectMocks RecipeService recipeService;

    private User user(long id) {
        return User.builder().id(id).email("u" + id + "@x.com").username("u" + id).build();
    }

    private Recipe recipeOwnedBy(User owner) {
        return Recipe.builder()
                .id(10L)
                .title("t")
                .youtubeUrl("https://youtu.be/abcdefghijk")
                .creator(owner)
                .ingredients(new ArrayList<>())
                .steps(new ArrayList<>())
                .build();
    }

    @Test
    void findDtoById_returnsDto() {
        User owner = user(1L);
        Recipe r = recipeOwnedBy(owner);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(r));

        var dto = recipeService.findDtoById(10L);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getCreatorId()).isEqualTo(1L);
    }

    @Test
    void findDtoById_missingThrows() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> recipeService.findDtoById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    void delete_byNonOwner_throwsAccessDenied() {
        User owner = user(1L);
        User other = user(2L);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipeOwnedBy(owner)));

        assertThatThrownBy(() -> recipeService.delete(10L, other))
                .isInstanceOf(RecipeService.AccessDeniedException.class);
        verify(recipeRepository, never()).delete(any());
    }

    @Test
    void delete_byOwner_succeeds() {
        User owner = user(1L);
        Recipe r = recipeOwnedBy(owner);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(r));

        recipeService.delete(10L, owner);

        verify(recipeRepository).delete(r);
    }

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        User owner = user(1L);
        User other = user(2L);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipeOwnedBy(owner)));

        UpdateRecipeRequest req = new UpdateRecipeRequest();
        req.setTitle("new title");

        assertThatThrownBy(() -> recipeService.update(10L, req, other))
                .isInstanceOf(RecipeService.AccessDeniedException.class);
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void update_byOwner_updatesFieldsAndReplacesCollections() {
        User owner = user(1L);
        Recipe r = recipeOwnedBy(owner);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(r));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRecipeRequest req = new UpdateRecipeRequest();
        req.setTitle("updated");
        req.setSummary("s");
        UpdateRecipeRequest.IngredientInput i = new UpdateRecipeRequest.IngredientInput();
        i.setName("양파"); i.setAmount("1개");
        req.setIngredients(java.util.List.of(i));
        UpdateRecipeRequest.StepInput s = new UpdateRecipeRequest.StepInput();
        s.setInstruction("볶는다");
        req.setSteps(java.util.List.of(s));

        var dto = recipeService.update(10L, req, owner);

        assertThat(dto.getTitle()).isEqualTo("updated");
        assertThat(dto.getIngredients()).hasSize(1);
        assertThat(dto.getSteps()).hasSize(1);
        assertThat(dto.getSteps().get(0).getStepNo()).isEqualTo(1);
    }

    @Test
    void createFromYoutube_reusesExistingByVideoId() {
        User owner = user(1L);
        Recipe existing = recipeOwnedBy(owner);
        when(youTubeService.extractVideoId("https://youtu.be/abcdefghijk")).thenReturn("abcdefghijk");
        when(recipeRepository.findFirstByVideoIdOrderByIdDesc("abcdefghijk"))
                .thenReturn(Optional.of(existing));

        var dto = recipeService.createFromYoutube("https://youtu.be/abcdefghijk", owner);

        assertThat(dto.getId()).isEqualTo(10L);
        verify(geminiService, never()).generate(any());
        verify(recipeRepository, never()).save(any());
    }
}
