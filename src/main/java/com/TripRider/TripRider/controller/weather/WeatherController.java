package com.TripRider.TripRider.controller.weather;

import com.TripRider.TripRider.dto.weather.RidingWeatherAdviceDto;
import com.TripRider.TripRider.dto.weather.SimpleWeatherResponse;
import com.TripRider.TripRider.service.weather.RidingWeatherAdviceService;
import com.TripRider.TripRider.service.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WeatherController {

    private static final String JEJU_CITY = "제주시";

    private final WeatherService weatherService;
    private final RidingWeatherAdviceService ridingWeatherAdviceService;

    @GetMapping("/api/jeju-weather")
    public Map<String, List<SimpleWeatherResponse>> getJejuWeather() {
        Map<String, List<SimpleWeatherResponse>> map = new HashMap<>();
        map.put(JEJU_CITY, weatherService.getJejuCityWeather());
        return map;
    }

    @GetMapping("/jeju-weather")
    public Map<String, String> getJejuSimpleWeather() {
        return weatherService.getSimpleWeather(JEJU_CITY);
    }

    @GetMapping("/api/weather/riding-advice")
    public RidingWeatherAdviceDto getRidingAdvice() {
        return ridingWeatherAdviceService.generateAdvice();
    }
}
