package com.recipeplatform.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipeplatform.backend.dto.RecipeDto;
import com.recipeplatform.backend.dto.UpdateRecipeRequest;
import com.recipeplatform.backend.entity.Ingredient;
import com.recipeplatform.backend.entity.Recipe;
import com.recipeplatform.backend.entity.RecipeStep;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final YouTubeService youTubeService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Page<RecipeDto> search(String q, String tag, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return recipeRepository.search(q, tag, pageable).map(RecipeDto::summary);
    }

    @Transactional(readOnly = true)
    public RecipeDto findDtoById(Long id) {
        Recipe r = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + id));
        return RecipeDto.from(r);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Recipe r = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + id));
        assertOwner(r, currentUser);
        recipeRepository.delete(r);
    }

    @Transactional
    public RecipeDto update(Long id, UpdateRecipeRequest req, User currentUser) {
        Recipe r = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다: " + id));
        assertOwner(r, currentUser);

        r.setTitle(req.getTitle());
        r.setSummary(req.getSummary());
        r.setServings(req.getServings());
        r.setCookingTime(req.getCookingTime());
        r.setDifficulty(req.getDifficulty());
        r.setTags(req.getTags());

        r.getIngredients().clear();
        if (req.getIngredients() != null) {
            int order = 0;
            for (UpdateRecipeRequest.IngredientInput i : req.getIngredients()) {
                Ingredient ing = Ingredient.builder()
                        .name(i.getName())
                        .amount(i.getAmount())
                        .sortOrder(order++)
                        .recipe(r)
                        .build();
                r.getIngredients().add(ing);
            }
        }

        r.getSteps().clear();
        if (req.getSteps() != null) {
            int n = 1;
            for (UpdateRecipeRequest.StepInput s : req.getSteps()) {
                int stepNo = s.getStepNo() != null ? s.getStepNo() : n;
                RecipeStep step = RecipeStep.builder()
                        .stepNo(stepNo)
                        .instruction(s.getInstruction())
                        .tip(s.getTip())
                        .recipe(r)
                        .build();
                r.getSteps().add(step);
                n++;
            }
        }

        return RecipeDto.from(recipeRepository.save(r));
    }

    @Transactional
    public RecipeDto createFromYoutube(String youtubeUrl, User currentUser) {
        String videoId = youTubeService.extractVideoId(youtubeUrl);
        if (videoId != null) {
            Optional<Recipe> existing = recipeRepository.findFirstByVideoIdOrderByIdDesc(videoId);
            if (existing.isPresent()) {
                log.info("이미 추출된 레시피 재사용: videoId={}, id={}", videoId, existing.get().getId());
                return RecipeDto.from(existing.get());
            }
        }

        YouTubeService.VideoInfo info = youTubeService.fetch(youtubeUrl);
        log.info("영상 정보 추출: title={}, transcript={} chars", info.title,
                info.transcript == null ? 0 : info.transcript.length());

        String prompt = buildPrompt(info);
        String json = geminiService.generate(prompt);
        log.debug("Gemini 응답: {}", json);

        Recipe recipe = parseRecipeJson(json);
        recipe.setYoutubeUrl(youtubeUrl);
        recipe.setVideoId(info.videoId);
        recipe.setThumbnailUrl(info.thumbnailUrl);
        if (recipe.getChannelName() == null) recipe.setChannelName(info.channelName);
        if (recipe.getDescription() == null) recipe.setDescription(info.description);
        recipe.setRawTranscript(info.transcript);
        recipe.setCreator(currentUser);

        for (Ingredient ing : recipe.getIngredients()) ing.setRecipe(recipe);
        for (RecipeStep step : recipe.getSteps()) step.setRecipe(recipe);

        Recipe saved = recipeRepository.save(recipe);
        return RecipeDto.from(saved);
    }

    private void assertOwner(Recipe r, User user) {
        if (r.getCreator() == null) {
            throw new IllegalStateException("소유자 정보가 없는 레시피는 수정/삭제할 수 없습니다.");
        }
        if (!r.getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인이 등록한 레시피만 수정/삭제할 수 있습니다.");
        }
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String msg) { super(msg); }
    }

    /** Gemini/YouTube 등 외부 시스템과의 연동 실패. 사용자에게 원본 메시지를 노출하지 않음. */
    public static class RecipeProcessingException extends RuntimeException {
        public RecipeProcessingException(String msg, Throwable cause) { super(msg, cause); }
    }

    private String buildPrompt(YouTubeService.VideoInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 YouTube 요리 영상의 정보를 보고 레시피를 JSON으로 추출해주세요.\n\n");
        sb.append("[영상 제목]\n").append(safe(info.title)).append("\n\n");
        sb.append("[채널]\n").append(safe(info.channelName)).append("\n\n");
        sb.append("[설명]\n").append(safe(info.description)).append("\n\n");
        if (info.transcript != null && !info.transcript.isBlank()) {
            String t = info.transcript.length() > 8000 ? info.transcript.substring(0, 8000) : info.transcript;
            sb.append("[자막/스크립트]\n").append(t).append("\n\n");
        }
        sb.append("""
                다음 형식의 JSON으로만 응답해주세요. 마크다운 코드블럭 없이 순수 JSON만:
                {
                  "title": "요리 이름",
                  "summary": "이 요리는 어떤 음식인지 1-2문장 설명",
                  "servings": "X인분",
                  "cookingTime": "약 X분",
                  "difficulty": "쉬움|보통|어려움",
                  "tags": "한식,찌개,저녁",
                  "ingredients": [
                    {"name": "재료명", "amount": "수량/단위"}
                  ],
                  "steps": [
                    {"stepNo": 1, "instruction": "조리 단계 설명", "tip": "선택적 팁"}
                  ]
                }
                자막에 명시되지 않은 정보는 합리적으로 추정하되, 절대 빈 레시피를 만들지 마세요.
                재료는 최소 3개, 단계는 최소 3개 이상 작성해주세요.
                """);
        return sb.toString();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private Recipe parseRecipeJson(String raw) {
        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("(?s)```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
            }
            JsonNode node = objectMapper.readTree(json);

            Recipe recipe = Recipe.builder()
                    .title(node.path("title").asText("제목 없음"))
                    .summary(node.path("summary").asText(null))
                    .servings(node.path("servings").asText(null))
                    .cookingTime(node.path("cookingTime").asText(null))
                    .difficulty(node.path("difficulty").asText(null))
                    .tags(node.path("tags").asText(null))
                    .ingredients(new ArrayList<>())
                    .steps(new ArrayList<>())
                    .build();

            JsonNode ings = node.path("ingredients");
            if (ings.isArray()) {
                int order = 0;
                for (JsonNode i : ings) {
                    recipe.getIngredients().add(Ingredient.builder()
                            .name(i.path("name").asText(""))
                            .amount(i.path("amount").asText(""))
                            .sortOrder(order++)
                            .build());
                }
            }

            JsonNode steps = node.path("steps");
            if (steps.isArray()) {
                int n = 1;
                for (JsonNode s : steps) {
                    int stepNo = s.path("stepNo").asInt(n);
                    recipe.getSteps().add(RecipeStep.builder()
                            .stepNo(stepNo)
                            .instruction(s.path("instruction").asText(""))
                            .tip(s.path("tip").asText(null))
                            .build());
                    n++;
                }
            }

            return recipe;
        } catch (Exception e) {
            log.warn("Gemini 응답 파싱 실패: {} / 원본(앞 500자): {}",
                    e.getMessage(),
                    raw == null ? "<null>" : raw.substring(0, Math.min(raw.length(), 500)));
            throw new RecipeProcessingException("Gemini 응답 파싱 실패", e);
        }
    }
}
