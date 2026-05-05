package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.riding.AiCourseSummaryDto;
import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiCourseService {

    private final CourseFileService courseFileService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${googleai.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${gemini.baseurl:https://generativelanguage.googleapis.com}")
    private String geminiBaseUrl;

    public AiCourseSummaryDto generateSummary(String category, Long id) {
        RidingCourseDetailDto course = courseFileService.get(category, id);

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini fallback: googleai.api.key is empty. category={}, id={}", category, id);
            return fallback();
        }

        try {
            String prompt = buildPrompt(course);
            String text = callGemini(prompt);
            if (text == null || text.isBlank()) {
                log.warn("Gemini fallback: empty response text. category={}, id={}", category, id);
                return fallback();
            }

            String normalized = stripCodeFence(text);
            AiCourseSummaryDto dto = objectMapper.readValue(normalized, AiCourseSummaryDto.class);

            if (dto.getAiDescription() == null || dto.getRecommendedFor() == null || dto.getRiderNotice() == null) {
                log.warn("Gemini fallback: parsed json missing fields. category={}, id={}, raw={}", category, id, shorten(normalized));
                return fallback();
            }
            return dto;
        } catch (HttpStatusCodeException e) {
            log.warn("Gemini fallback: http error status={}, body={}, category={}, id={}",
                    e.getStatusCode(), shorten(e.getResponseBodyAsString()), category, id);
            return fallback();
        } catch (Exception e) {
            log.warn("Gemini fallback: exception={}, category={}, id={}", e.toString(), category, id, e);
            return fallback();
        }
    }

    private String buildPrompt(RidingCourseDetailDto course) {
        List<RidingCourseDetailDto.LatLng> polyline = course.getPolyline();
        int polylineSize = polyline == null ? 0 : polyline.size();
        RidingCourseDetailDto.LatLng start = polylineSize > 0 ? polyline.get(0) : new RidingCourseDetailDto.LatLng(0.0, 0.0);
        RidingCourseDetailDto.LatLng end = polylineSize > 0 ? polyline.get(polylineSize - 1) : new RidingCourseDetailDto.LatLng(0.0, 0.0);
        double distanceKm = course.getTotalDistanceMeters() / 1000.0;

        return """
                너는 제주 오토바이 여행 앱 TripRider의 라이딩 코스 설명 AI야.
                아래 코스 정보를 바탕으로 사용자에게 보여줄 설명을 생성해.

                [코스 정보]
                코스명: %s
                카테고리: %s
                총 거리: %.1fkm
                경로 좌표 수: %d
                시작 좌표: %.6f, %.6f
                도착 좌표: %.6f, %.6f

                [작성 조건]
                1. aiDescription은 정확히 3줄로 작성
                2. recommendedFor는 어떤 사용자에게 추천하는지 1~2문장으로 작성
                3. riderNotice는 라이더가 주의해야 할 점을 1~2문장으로 작성
                4. 실제 데이터에 없는 장소명, 맛집, 시설명은 만들지 말 것
                5. 한국어로 작성
                6. 라이더 관점에서 자연스럽게 작성
                7. 과장된 표현은 피할 것

                [출력 형식]
                반드시 아래 JSON 형식으로만 출력:
                {
                  "aiDescription": "...",
                  "recommendedFor": "...",
                  "riderNotice": "..."
                }
                """.formatted(
                nullSafe(course.getTitle()),
                nullSafe(course.getCategory()),
                distanceKm,
                polylineSize,
                start.getLat(),
                start.getLng(),
                end.getLat(),
                end.getLng()
        );
    }

    private String callGemini(String prompt) {
        String url = UriComponentsBuilder
                .fromHttpUrl(geminiBaseUrl)
                .path("/v1beta/models/{model}:generateContent")
                .queryParam("key", geminiApiKey)
                .buildAndExpand(geminiModel)
                .toUriString();

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                JsonNode.class
        );

        JsonNode root = response.getBody();
        if (root == null) return null;
        return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine >= 0) {
                trimmed = trimmed.substring(firstNewLine + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String shorten(String s) {
        if (s == null) return "";
        String oneLine = s.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }

    private AiCourseSummaryDto fallback() {
        return AiCourseSummaryDto.builder()
                .aiDescription("이 코스는 선택한 경로를 따라 라이딩을 즐길 수 있는 코스입니다.\n주변 풍경을 감상하며 이동하기 좋습니다.\n거리와 경로를 확인한 뒤 여유롭게 이용해보세요.")
                .recommendedFor("가볍게 라이딩 코스를 확인하고 싶은 사용자에게 추천합니다.")
                .riderNotice("실제 주행 전 도로 상황과 날씨를 확인하고, 안전장비를 착용한 뒤 운행하세요.")
                .build();
    }
}
