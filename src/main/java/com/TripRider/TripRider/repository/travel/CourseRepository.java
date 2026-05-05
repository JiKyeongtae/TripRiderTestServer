package com.TripRider.TripRider.repository.travel;

import com.TripRider.TripRider.domain.travel.CourseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<CourseEntity, String> {
    Page<CourseEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 소유자+ID 동시 검증용
    Optional<CourseEntity> findByIdAndUserId(String id, Long userId);

    // (게스트용이 필요 없다면 아래는 삭제)
    Page<CourseEntity> findByUserIdIsNullOrderByCreatedAtDesc(Pageable pageable);

    // 삭제도 소유자 조건으로 (선택)
    long deleteByIdAndUserId(String id, Long userId);
}

