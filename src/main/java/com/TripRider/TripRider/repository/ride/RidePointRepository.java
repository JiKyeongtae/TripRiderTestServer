package com.TripRider.TripRider.repository.ride;

import com.TripRider.TripRider.domain.ride.RidePoint;
import com.TripRider.TripRider.domain.ride.RideSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RidePointRepository extends JpaRepository<RidePoint, Long> {
    List<RidePoint> findByRideOrderBySeqAsc(RideSession ride);
    long countByRide(RideSession ride);
}