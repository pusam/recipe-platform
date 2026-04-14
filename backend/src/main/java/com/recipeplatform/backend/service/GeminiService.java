package com.recipeplatform.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되지 않았습니다.");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String response = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText();
            }
            log.warn("Gemini 응답에서 텍스트 추출 실패: {}", response);
            throw new IllegalStateException("Gemini 응답이 비어있습니다.");
        } catch (Exception e) {
            log.error("Gemini 호출 실패", e);
            throw new RuntimeException("AI 추출에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
