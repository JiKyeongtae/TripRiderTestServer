package com.TripRider.TripRider.dto.custom;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaveCourseReq {
    private String title;

    @Builder.Default
    private List<CoursePreview.Waypoint> waypoints = new ArrayList<>();  // ← 기본 빈 리스트

    private Double distanceKm;
    private Integer durationMin;
    private String polyline;
}
