package com.TripRider.TripRider.repository.board;

import com.TripRider.TripRider.domain.board.Comment;
import com.TripRider.TripRider.domain.board.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // User를 함께 가져오도록 강제 (LAZY 문제 방지)
    @EntityGraph(attributePaths = "user")
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    void deleteByPost(Post post); //삭제 게시물에 대한 댓글 자동 삭제

    int countByPost(Post post);


}