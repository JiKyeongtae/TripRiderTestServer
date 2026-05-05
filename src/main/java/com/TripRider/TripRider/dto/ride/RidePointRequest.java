package com.TripRider.TripRider.dto.ride;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RidePointRequest {
    private List<Point> points;

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Point {
        private long seq;
        private double lat;
        private double lng;
        private Double altitude;
        private Double speedKmh;
        private long epochMillis;
    }
}