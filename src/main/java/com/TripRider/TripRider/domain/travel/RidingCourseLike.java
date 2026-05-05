package com.TripRider.TripRider.domain.travel;

import com.TripRider.TripRider.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "course_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_user",
                columnNames = {"category", "course_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_course_like_key", columnList = "category,course_id"),
                @Index(name = "idx_course_like_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RidingCourseLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 코스 구분값: coastal-course / inland-course / udo-course */
    @Column(nullable = false, length = 32)
    private String category;

    /** 카테고리 내 코스 번호 */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** 누른 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
