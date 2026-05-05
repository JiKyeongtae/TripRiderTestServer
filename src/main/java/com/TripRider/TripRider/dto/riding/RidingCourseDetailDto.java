package com.TripRider.TripRider.dto.riding;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
// 특정 코스를 선택했을 때 상세 정보 + 경로 좌표를 보여줄 때 사용 (상세 화면 용)
public class RidingCourseDetailDto {
    private Long id;
    private String category;
    private String title;
    private String description;
    private String coverImageUrl;
    private int totalDistanceMeters;
    private List<LatLng> polyline;
    private Integer likeCount;   // null 허용
    private Boolean liked;       // 로그인 유저가 좋아요 눌렀는지 확인

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    // 경로 좌표 용
    public static class LatLng { private double lat; private double lng; }
}