package com.TripRider.TripRider.service.travel;
import com.TripRider.TripRider.domain.travel.CourseEntity;
import com.TripRider.TripRider.dto.custom.*;
import com.TripRider.TripRider.repository.travel.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomCourseService {
    private final CourseRepository courseRepository;

    @Transactional
    public CourseView save(SaveCourseReq req, Long userId) {
        // 게스트 저장을 막고 싶으면 userId null일 때 예외
        if (userId == null) throw new IllegalStateException("로그인이 필요합니다.");

        String id = "crs_" + UUID.randomUUID().toString().replace("-", "");
        List<CoursePreview.Waypoint> wps =
                (req.getWaypoints() == null) ? new ArrayList<>() : req.getWaypoints();

        CourseEntity e = CourseMapper.toEntity(
                id,
                userId, // ★ 소유자 고정
                Optional.ofNullable(req.getTitle()).orElse("나의 여행 코스"),
                Optional.ofNullable(req.getDistanceKm()).orElse(0.0),
                Optional.ofNullable(req.getDurationMin()).orElse(0),
                req.getPolyline(),
                wps
        );

        CourseEntity saved = courseRepository.save(e);
        return CourseMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseCard> findMine(Long userId, int page, int size) {
        if (userId == null) throw new IllegalStateException("로그인이 필요합니다.");
        var pageable = PageRequest.of(Math.max(page-1, 0), Math.max(size, 1));
        var pageData = courseRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return pageData.getContent().stream().map(CourseMapper::toCard).toList();
    }

    // 소유자 검증 버전
    @Transactional(readOnly = true)
    public CourseView detailForOwner(String id, Long userId) {
        CourseEntity e = courseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("course not found"));
        e.getWaypoints().size(); // LAZY 초기화
        return CourseMapper.toView(e);
    }

    @Transactional
    public void deleteForOwner(String id, Long userId) {
        long cnt = courseRepository.deleteByIdAndUserId(id, userId);
        if (cnt == 0) throw new NoSuchElementException("course not found or not owned");
    }
}
