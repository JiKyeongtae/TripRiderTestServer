package com.TripRider.TripRider.service.weather;

import com.TripRider.TripRider.dto.weather.SimpleWeatherResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.service-key}")
    private String serviceKey;

    // 표시명(한글)
    private static final Map<String, String> CATEGORY_MAP = Map.of(
            "TMP", "기온",
            "WSD", "풍속",
            "SKY", "하늘 상태",
            "PTY", "강수 형태",
            "POP", "강수확률",
            "PCP", "강수량",
            "SNO", "적설"
    );

    // 요약 응답 키
    private static final List<String> SUMMARY_CODES = List.of("TMP", "POP", "PTY", "SKY", "WSD", "PCP", "SNO");

    // 제주시 격자
    private static final int[] JEJU_CITY_COORDS = new int[]{53, 38};
    private static final String JEJU_CITY = "제주시";

    private String[] getLatestBaseDateTime() {
        LocalDateTime now = LocalDateTime.now();
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int hour = now.getHour();

        String baseTime;
        if (hour >= 23) baseTime = "2300";
        else if (hour >= 20) baseTime = "2000";
        else if (hour >= 17) baseTime = "1700";
        else if (hour >= 14) baseTime = "1400";
        else if (hour >= 11) baseTime = "1100";
        else if (hour >= 8) baseTime = "0800";
        else if (hour >= 5) baseTime = "0500";
        else baseTime = "0200";

        return new String[]{baseDate, baseTime};
    }

    // 제주시 전체 예보 목록
    public List<SimpleWeatherResponse> getJejuCityWeather() {
        String[] dateTime = getLatestBaseDateTime();
        String baseDate = dateTime[0];
        String baseTime = dateTime[1];

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        String url = baseUrl + "/getVilageFcst" +
                "?serviceKey=" + serviceKey +
                "&pageNo=1&numOfRows=1000&dataType=JSON" +
                "&base_date=" + baseDate +
                "&base_time=" + baseTime +
                "&nx=" + JEJU_CITY_COORDS[0] +
                "&ny=" + JEJU_CITY_COORDS[1];

        List<SimpleWeatherResponse> weatherList = new ArrayList<>();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            for (JsonNode item : items) {
                String categoryCode = item.path("category").asText();
                if (!CATEGORY_MAP.containsKey(categoryCode)) continue;

                SimpleWeatherResponse simple = new SimpleWeatherResponse();
                simple.setCategory(CATEGORY_MAP.get(categoryCode));
                simple.setFcstTime(item.path("fcstTime").asText());
                simple.setFcstValue(item.path("fcstValue").asText());
                weatherList.add(simple);
            }
        } catch (Exception e) {
            log.warn("[제주시 날씨 정보 조회 실패] {}", e.getMessage());
        }

        return weatherList;
    }

    // 요약 날씨: 기존 temp/rain + 나머지 항목 전부 포함
    public Map<String, String> getSimpleWeather(String region) {
        List<SimpleWeatherResponse> weatherList = JEJU_CITY.equals(region)
                ? getJejuCityWeather()
                : List.of();

        String currentTime = LocalDateTime.now().plusHours(1)
                .format(DateTimeFormatter.ofPattern("HH00"));

        Map<String, String> result = new LinkedHashMap<>();
        result.put("temp", "N/A");   // TMP
        result.put("rain", "N/A");   // POP (기존 호환)
        result.put("sky", "N/A");    // SKY
        result.put("pty", "N/A");    // PTY
        result.put("wind", "N/A");   // WSD
        result.put("pcp", "N/A");    // PCP
        result.put("sno", "N/A");    // SNO

        for (SimpleWeatherResponse response : weatherList) {
            if (!currentTime.equals(response.getFcstTime())) continue;
            String category = response.getCategory();
            String value = response.getFcstValue();

            if ("기온".equals(category)) result.put("temp", value);
            else if ("강수확률".equals(category)) result.put("rain", value);
            else if ("하늘 상태".equals(category)) result.put("sky", value);
            else if ("강수 형태".equals(category)) result.put("pty", value);
            else if ("풍속".equals(category)) result.put("wind", value);
            else if ("강수량".equals(category)) result.put("pcp", value);
            else if ("적설".equals(category)) result.put("sno", value);
        }

        return result;
    }
}
