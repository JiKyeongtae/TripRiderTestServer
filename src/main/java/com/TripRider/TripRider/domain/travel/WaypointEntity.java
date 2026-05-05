package com.TripRider.TripRider.domain.travel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "custom_course_waypoint")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WaypointEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // 내부 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @Column(name = "ord_idx")
    private int ordIdx;              // 표시 순서

    @Column(length = 32)
    private String contentId;

    @Column(length = 16)
    private String type;             // tour|food|stay|shop|event|leports

    private String title;

    private double lat;
    private double lng;

    @Column(length = 16) private String cat1;
    @Column(length = 16) private String cat2;
    @Column(length = 16) private String cat3;

    private Integer contentTypeId;
}