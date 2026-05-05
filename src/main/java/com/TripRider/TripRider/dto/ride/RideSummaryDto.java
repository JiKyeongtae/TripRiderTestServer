package com.TripRider.TripRider.dto.ride;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RideSummaryDto {
    private Long id;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private double totalKm;
    private long   movingSeconds;
    private double avgSpeedKmh;
    private double maxSpeedKmh;
    private String routeImageUrl;
    private String title;
}