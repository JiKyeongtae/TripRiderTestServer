package com.TripRider.TripRider.dto.custom;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryTreeDto {
    private int contentTypeId;
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
    private List<Node> cat1; //대분류

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Node {
        private String code;
        private String name;
        private List<Node> cat2; //중분류
        private List<Node> cat3; //소분류
    }
}
