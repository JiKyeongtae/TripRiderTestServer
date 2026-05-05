package com.TripRider.TripRider.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeType {
    KM_10(10, "10km 달성"),
    KM_50(50, "50km 달성"),
    KM_100(100, "100km 달성"),
    KM_300(300, "300km 달성"),
    KM_500(500, "500km 달성"),
    KM_1000(1000, "1000km 달성");

    private final int distanceRequired;
    private final String name;
}
