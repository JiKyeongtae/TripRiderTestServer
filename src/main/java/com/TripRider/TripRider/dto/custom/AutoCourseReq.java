package com.TripRider.TripRider.dto.custom;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoCourseReq {
    private String selectionId; // 선택: 바구니 기반
    @Builder.Default private List<Bucket> buckets = new ArrayList<>(); // 선택: 버킷 기반
    private String sort;        // rating | popularity | distance
    private boolean optimize = true;
    private String scope = "jeju"; // local | jeju | grid
    private Center center;      // 선택
    private Integer radius;     // 선택

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Center { private double lat; private double lng; }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Bucket {
        private String type;     // tour|food|...
        private String cat1;
        private String cat2;
        private String cat3;
        private int pick;        // 뽑을 개수
    }
}


