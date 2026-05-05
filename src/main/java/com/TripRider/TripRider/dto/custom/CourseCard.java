package com.TripRider.TripRider.dto.custom;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseCard {
    private String id;
    private String title;
    private String stopsPreview; // "성산일출봉 → 갈치구이 → OO카페"
    private String createdAt;
}
