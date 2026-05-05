package com.TripRider.TripRider.service.weather;

import com.TripRider.TripRider.dto.weather.RidingWeatherAdviceDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RidingWeatherAdviceService {

    private static final String REGION_JEJU_CITY = "제주시";

    private final WeatherService weatherService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${googleai.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${gemini.baseurl:https://generativelanguage.googleapis.com}")
    private String geminiBaseUrl;

    public RidingWeatherAdviceDto generateAdvice() {
        Map<String, String> weather = weatherService.getSimpleWeather(REGION_JEJU_CITY);
        String temp = nvl(weather.get("temp"));
        String rain = nvl(weather.get("rain"));
        String sky = nvl(weather.get("sky"));
        String pty = nvl(weather.get("pty"));
        String wind = nvl(weather.get("wind"));
        String pcp = nvl(weather.get("pcp"));
        String sno = nvl(weather.get("sno"));

        String skyText = toSkyText(sky);
        String ptyText = toPtyText(pty);

        if (geminiApiKey.isBlank()) {
            log.warn("Weather advice fallback: googleai.api.key is empty");
            return fallback();
        }

        try {
            String prompt = buildPrompt(temp, rain, skyText, ptyText, wind, pcp, sno);
            String text = callGemini(prompt);
            if (text == null || text.isBlank()) {
                log.warn("Weather advice fallback: empty response text");
                return fallback();
            }

            String normalized = stripCodeFence(text);
            RidingWeatherAdviceDto dto = objectMapper.readValue(normalized, RidingWeatherAdviceDto.class);
            if (dto.getWeatherSummary() == null || dto.getRidingAdvice() == null || dto.getRiskLevel() == null) {
                log.warn("Weather advice fallback: parsed json missing fields. raw={}", shorten(normalized));
                return fallback();
            }
            if (!List.of("좋음", "주의", "비추천").contains(dto.getRiskLevel())) {
                log.warn("Weather advice fallback: invalid riskLevel={}", dto.getRiskLevel());
                return fallback();
            }
            return dto;
        } catch (HttpStatusCodeException e) {
            log.warn("Weather advice fallback: http error status={}, body={}",
                    e.getStatusCode(), shorten(e.getResponseBodyAsString()));
            return fallback();
        } catch (Exception e) {
            log.warn("Weather advice fallback: exception={}", e.toString(), e);
            return fallback();
        }
    }

    private String buildPrompt(String temp, String rain, String skyText, String ptyText, String wind, String pcp, String sno) {
        return """
                너는 오토바이 여행 앱 TripRider의 라이딩 안전 도우미야.
                아래 날씨 정보를 바탕으로 라이더에게 필요한 주의사항을 작성해.

                [날씨 정보]
                기온: %s℃
                강수확률: %s%%
                하늘상태: %s
                강수형태: %s
                풍속: %sm/s
                강수량: %s
                적설량: %s

                [작성 조건]
                1. weatherSummary는 현재 날씨를 1문장으로 요약
                2. ridingAdvice는 반드시 1문장으로 작성
                3. ridingAdvice는 반드시 50자 이내로 작성
                4. ridingAdvice는 핵심 주의사항만 짧게 작성
                5. riskLevel은 반드시 "좋음", "주의", "비추천" 중 하나만 사용
                6. 비, 눈, 강풍, 높은 강수확률이 있으면 주의 또는 비추천으로 판단
                7. 한국어로 작성
                8. 과장하지 말 것
                9. 실제 데이터에 없는 정보를 만들지 말 것
                10. 오토바이 주행 안전 관점에서 작성

                [판단 기준 참고]
                - 강수형태가 비, 비/눈, 눈, 소나기이면 최소 "주의"
                - 강수확률이 60 이상이면 최소 "주의"
                - 풍속이 8m/s 이상이면 "주의"
                - 풍속이 12m/s 이상이면 "비추천"
                - 눈, 비가 강하면 "비추천"
                - 날씨가 안정적이고 강수확률이 낮고 풍속이 약하면 "좋음"

                [출력 형식]
                반드시 아래 JSON 형식으로만 출력:
                {
                  "weatherSummary": "...",
                  "ridingAdvice": "...",
                  "riskLevel": "좋음"
                }
                """.formatted(temp, rain, skyText, ptyText, wind, pcp, sno);
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
        generationConfig.put("temperature", 0.4);
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

    private String toSkyText(String code) {
        return switch (code) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "알 수 없음";
        };
    }

    private String toPtyText(String code) {
        return switch (code) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "알 수 없음";
        };
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

    private String nvl(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String shorten(String s) {
        if (s == null) return "";
        String oneLine = s.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }

    private RidingWeatherAdviceDto fallback() {
        return RidingWeatherAdviceDto.builder()
                .weatherSummary("현재 날씨 정보를 바탕으로 라이딩 상태를 확인했습니다.")
                .ridingAdvice("주행 전 날씨와 도로 상태를 확인하세요.")
                .riskLevel("주의")
                .build();
    }
}
