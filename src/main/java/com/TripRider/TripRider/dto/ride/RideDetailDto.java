package com.TripRider.TripRider.dto.ride;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RideDetailDto {
    private Long id;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private double totalKm;
    private long   movingSeconds;
    private double avgSpeedKmh;
    private double maxSpeedKmh;
    private String routeImageUrl;
    private String title;
    private String memo;
    private List<LatLng> polyline;

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LatLng {
        private double lat;
        private double lng;
    }
}