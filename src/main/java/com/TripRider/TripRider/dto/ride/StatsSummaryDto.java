package com.TripRider.TripRider.dto.ride;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class StatsSummaryDto {
    private int rideCount;
    private double totalKm;
    private long totalSeconds;
}