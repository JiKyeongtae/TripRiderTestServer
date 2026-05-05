package com.TripRider.TripRider.dto.mypage;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@Builder
public class MyPageResponse {
    private String nickname;
    private String email;
    private String profileImage;
    private String intro;
    private int totalDistance;
    private String badge;
    private String representativeBadge; // 대표 뱃지
}
