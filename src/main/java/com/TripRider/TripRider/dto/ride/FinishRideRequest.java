package com.TripRider.TripRider.dto.ride;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FinishRideRequest {
    private String title;
    private String memo;
}