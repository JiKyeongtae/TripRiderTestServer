package com.TripRider.TripRider.domain.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;   // 뱃지 이름 (예: "10km 달성")

    private String description;   // 뱃지 설명 (예: "100km 이상 달성한 라이더")

    //  거리 조건 (예: 10km, 50km, 100km 등)
    private int distanceRequired;


    private String iconUrl;       // 뱃지 아이콘 경로 (예: "/images/badges/100.png")

    //  어떤 유저의 뱃지인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}