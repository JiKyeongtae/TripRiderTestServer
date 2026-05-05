package com.TripRider.TripRider.dto.common;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NearbyPlaceDto {
    private Long contentId;        // 관광공사 콘텐츠 ID(있으면 중복제거에 유리)
    private String title;          // 명칭
    private String addr;           // 주소(addr1)
    private String tel;            // 전화
    private String image;          // 대표 이미지 URL(firstimage)
    private double lat;            // mapY
    private double lng;            // mapX
    private Integer distMeters;    // 거리(일부 응답에만 옴)
    private Integer contentTypeId;
    /**
     * 12 : 관광지
     * 14 : 문화시설
     * 15 : 행사/공연/축제
     * 25 : 여행코스
     * 28 : 레포츠
     * 32 : 숙박
     * 38 : 쇼핑
     * 39 : 음식점
     */

    //맞춤형 코스 제작 시 필요
    private String cat1;   // 대분류
    private String cat2;   // 중분류
    private String cat3;   // 소분류
}
