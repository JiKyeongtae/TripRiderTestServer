package com.TripRider.TripRider.repository.travel;

import com.TripRider.TripRider.domain.travel.RidingCourseLike;
import com.TripRider.TripRider.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RidingCourseLikeRepository extends JpaRepository<RidingCourseLike, Long> {

    boolean existsByCategoryAndCourseIdAndUser(String category, Long courseId, User user);

    long countByCategoryAndCourseId(String category, Long courseId);

    void deleteByCategoryAndCourseIdAndUser(String category, Long courseId, User user);
}
