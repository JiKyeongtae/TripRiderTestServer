package com.TripRider.TripRider.dto.weather;

import lombok.Data;

@Data
public class SimpleWeatherResponse {
    private String category;    // 예: TMP, PTY, SKY 등
    private String fcstTime;    // 예: 0600
    private String fcstValue;   // 예: 23
}
