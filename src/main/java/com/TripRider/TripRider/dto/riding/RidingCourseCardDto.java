package com.TripRider.TripRider.dto.riding;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
// 카드 형태로 여러 코스 목록을 보여줄 때 사용 (목록 조회용)
public class RidingCourseCardDto {
    private Long id;
    private String category;              // coastal(해안)/ inland(내륙) / udo(우도)
    private String title;                 // 자동 생성 or 수동 입력
    private String coverImageUrl;         // 있으면 표시, 없으면 null
    private int totalDistanceMeters;
    private double startLat;
    private double startLng;
    private Double distanceMetersFromMe;  // 거리순 정렬용(요청시 계산)
    private Integer likeCount;   // null 허용
    private Boolean liked;       // 로그인 유저가 좋아요 눌렀는지 확인
}
