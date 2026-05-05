package com.TripRider.TripRider.repository.user;

import com.TripRider.TripRider.domain.user.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    /**
     * 유저 ID와 뱃지 이름으로 이미 보유 중인지 확인
     */
    boolean existsByUserIdAndName(Long userId, String name);

    /**
     * 뱃지 이름으로 뱃지 엔티티 조회
     */
    Optional<Badge> findByName(String name);
}