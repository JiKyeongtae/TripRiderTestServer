package com.TripRider.TripRider.repository.user;

import com.TripRider.TripRider.domain.user.Badge;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.domain.user.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUser(User user);
    boolean existsByUserAndBadge(User user, Badge badge);
}
