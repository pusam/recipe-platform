package com.recipeplatform.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeService {

    private static final Pattern VIDEO_ID = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|shorts/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})");
    private static final Pattern PLAYER_RESPONSE = Pattern.compile(
            "ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\})\\s*;\\s*(?:var|window|</script>)", Pattern.DOTALL);

    private final WebClient webClient = WebClient.builder()
            .defaultHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
            .defaultHeader("Accept-Language", "ko,en;q=0.9")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m = VIDEO_ID.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    public VideoInfo fetch(String url) {
        String videoId = extractVideoId(url);
        if (videoId == null) {
            throw new IllegalArgumentException("유효한 YouTube URL이 아닙니다: " + url);
        }

        String watchUrl = "https://www.youtube.com/watch?v=" + videoId + "&hl=ko";
        String html = webClient.get().uri(watchUrl).retrieve().bodyToMono(String.class).block();
        if (html == null) {
            throw new IllegalStateException("YouTube 페이지를 불러올 수 없습니다.");
        }

        VideoInfo info = new VideoInfo();
        info.videoId = videoId;
        info.thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

        try {
            Document doc = Jsoup.parse(html);
            info.title = metaContent(doc, "meta[name=title]");
            if (info.title == null) info.title = metaContent(doc, "meta[property=og:title]");
            info.description = metaContent(doc, "meta[name=description]");
            info.channelName = metaContent(doc, "link[itemprop=name]", "content");
        } catch (Exception e) {
            log.warn("HTML 메타 파싱 실패: {}", e.getMessage());
        }

        info.transcript = tryFetchTranscript(html);
        return info;
    }

    private String metaContent(Document doc, String selector) {
        return metaContent(doc, selector, "content");
    }

    private String metaContent(Document doc, String selector, String attr) {
        var el = doc.selectFirst(selector);
        return el != null ? el.attr(attr) : null;
    }

    private String tryFetchTranscript(String html) {
        try {
            Matcher m = PLAYER_RESPONSE.matcher(html);
            if (!m.find()) {
                log.info("ytInitialPlayerResponse 미발견 — 자막 없음으로 처리");
                return null;
            }
            String json = m.group(1);
            JsonNode root = objectMapper.readTree(json);
            JsonNode tracks = root.path("captions")
                    .path("playerCaptionsTracklistRenderer")
                    .path("captionTracks");
            if (!tracks.isArray() || tracks.isEmpty()) {
                log.info("captionTracks 비어있음");
                return null;
            }

            // 한국어 우선, 없으면 첫 번째
            String trackUrl = null;
            for (JsonNode t : tracks) {
                if ("ko".equals(t.path("languageCode").asText())) {
                    trackUrl = t.path("baseUrl").asText();
                    break;
                }
            }
            if (trackUrl == null) trackUrl = tracks.get(0).path("baseUrl").asText();
            if (trackUrl.isEmpty()) return null;

            String xml = webClient.get().uri(trackUrl).retrieve().bodyToMono(String.class).block();
            return parseTranscriptXml(xml);
        } catch (Exception e) {
            log.warn("자막 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    private String parseTranscriptXml(String xml) {
        if (xml == null) return null;
        // <text ...>HTML 인코딩된 문자열</text> 들을 합침
        Pattern p = Pattern.compile("<text[^>]*>(.*?)</text>", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String text = m.group(1)
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replaceAll("<[^>]+>", "")
                    .trim();
            if (!text.isEmpty()) sb.append(text).append(' ');
        }
        return sb.toString().trim();
    }

    @Getter
    public static class VideoInfo {
        public String videoId;
        public String title;
        public String description;
        public String channelName;
        public String thumbnailUrl;
        public String transcript;
    }
}
