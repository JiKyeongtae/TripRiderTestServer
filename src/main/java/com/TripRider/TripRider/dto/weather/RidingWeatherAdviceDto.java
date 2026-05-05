package com.TripRider.TripRider.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RidingWeatherAdviceDto {
    private String weatherSummary; //날씨 요약
    private String ridingAdvice; //라이딩 조언
    private String riskLevel; //위험레벨
}
