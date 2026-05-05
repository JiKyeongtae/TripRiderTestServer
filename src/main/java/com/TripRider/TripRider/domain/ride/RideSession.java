package com.TripRider.TripRider.domain.ride;

import com.TripRider.TripRider.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RideSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private double totalDistanceMeters;
    private long   movingSeconds;
    private double avgSpeedKmh;
    private double maxSpeedKmh;

    private boolean finished;
    private String routeImageUrl;

    private String title;
    private String memo;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Duration duration() {
        if (startedAt != null && finishedAt != null) {
            return Duration.between(startedAt, finishedAt);
        }
        return Duration.ofSeconds(movingSeconds);
    }
}
