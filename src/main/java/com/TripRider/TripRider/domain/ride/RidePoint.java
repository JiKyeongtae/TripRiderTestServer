package com.TripRider.TripRider.domain.ride;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RidePoint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ride_id", nullable=false)
    private RideSession ride;

    private long seq;
    private double latitude;
    private double longitude;
    private Double altitude;
    private Double speedKmh;
    private long epochMillis;
}
