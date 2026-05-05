package com.TripRider.TripRider.repository.board;

import com.TripRider.TripRider.domain.board.Comment;
import com.TripRider.TripRider.domain.board.CommentLike;
import com.TripRider.TripRider.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentAndUser(Comment comment, User user);
    long countByComment(Comment comment);
    void deleteByCommentAndUser(Comment comment, User user);
}
