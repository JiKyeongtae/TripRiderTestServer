package com.TripRider.TripRider.dto.custom;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseView {
    private String id;
    private String title;

    @Builder.Default
    private List<CoursePreview.Waypoint> waypoints = new ArrayList<>();  // ← 기본 빈 리스트

    private double distanceKm;
    private int durationMin;
    private String polyline;
    private String createdAt;
}
