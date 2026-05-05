package com.TripRider.TripRider.dto.custom;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoursePreview {
    private List<Waypoint> waypoints;
    private double distanceKm;
    private int durationMin;
    private String polyline;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Waypoint {
        private int order;
        private String contentId;
        private String type;
        private String title;
        private double lat, lng;
        private String cat1, cat2, cat3;
        private Integer contentTypeId;
    }
}
