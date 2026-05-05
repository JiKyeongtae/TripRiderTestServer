package com.TripRider.TripRider.service.mypage;

import com.TripRider.TripRider.domain.user.Badge;
import com.TripRider.TripRider.domain.user.BadgeType;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.repository.user.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;

    @Transactional
    public void checkAndGrantDistanceBadges(User user) {
        int totalDistance = user.getTotalDistance();

        for (BadgeType type : BadgeType.values()) {
            if (totalDistance >= type.getDistanceRequired()
                    && !badgeRepository.existsByUserIdAndName(user.getId(), type.getName())) {

                Badge badge = Badge.builder()
                        .name(type.getName())
                        .distanceRequired(type.getDistanceRequired())
                        .user(user)
                        .build();

                badgeRepository.save(badge);
            }
        }
    }
}
