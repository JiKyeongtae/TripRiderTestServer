package com.TripRider.TripRider.dto.riding;

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
public class AiCourseSummaryDto {
    private String aiDescription; //ai 코스 요약
    private String recommendedFor; //추천하는 사용자
    private String riderNotice; //라이더 주의사항
} // AI 코스 요약
