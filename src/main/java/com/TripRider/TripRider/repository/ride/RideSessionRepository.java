package com.TripRider.TripRider.repository.ride;

import com.TripRider.TripRider.domain.ride.RideSession;
import com.TripRider.TripRider.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideSessionRepository extends JpaRepository<RideSession, Long> {
    List<RideSession> findByUserOrderByCreatedAtDesc(User user);

    // ← 추가: 미종료 세션이 이미 있으면 재사용
    Optional<RideSession> findTopByUserAndFinishedFalseOrderByCreatedAtDesc(User user);
}
