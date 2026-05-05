package com.TripRider.TripRider.dto.ride;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class StartRideResponse {
    private Long rideId;
    private long startEpochMillis;
}