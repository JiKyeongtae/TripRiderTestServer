package com.TripRider.TripRider.repository.board;

import com.TripRider.TripRider.domain.board.Post;
import com.TripRider.TripRider.domain.board.PostLike;
import com.TripRider.TripRider.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostAndUser(Post post, User user);
    long countByPost(Post post);
    void deleteByPostAndUser(Post post, User user);
}
