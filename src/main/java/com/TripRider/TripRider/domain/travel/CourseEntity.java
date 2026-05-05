package com.TripRider.TripRider.domain.travel;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_course")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseEntity {

    @Id
    @Column(length = 64)
    private String id;                      // "crs_xxx" 같은 문자열 ID

    private Long userId;                    // X-USER-ID (없으면 null 허용)

    @Column(nullable = false)
    private String title;

    private Double distanceKm;              // null 허용
    private Integer durationMin;            // null 허용

    @Lob
    private String polyline;                // 필요없으면 null

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordIdx ASC")
    private List<WaypointEntity> waypoints = new ArrayList<>();

    // 편의 메서드
    public void addWaypoint(WaypointEntity w) {
        waypoints.add(w);
        w.setCourse(this);
    }
}