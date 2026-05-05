package com.TripRider.TripRider.service.board;

import com.TripRider.TripRider.domain.board.Comment;
import com.TripRider.TripRider.domain.board.CommentLike;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.repository.board.CommentLikeRepository;
import com.TripRider.TripRider.repository.board.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;

    /** 댓글 좋아요 */
    @Transactional
    public long likeComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (commentLikeRepository.existsByCommentAndUser(comment, user)) {
            throw new IllegalStateException("이미 좋아요를 누른 댓글입니다.");
        }

        CommentLike like = CommentLike.builder()
                .comment(comment)
                .user(user)
                .build();
        commentLikeRepository.save(like);

        return commentLikeRepository.countByComment(comment);
    }

    /** 댓글 좋아요 취소 */
    @Transactional
    public long unlikeComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        commentLikeRepository.deleteByCommentAndUser(comment, user);

        return commentLikeRepository.countByComment(comment);
    }

    /** 좋아요 개수 조회 */
    @Transactional(readOnly = true)
    public long countLikes(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        return commentLikeRepository.countByComment(comment);
    }

    /** 내가 좋아요 눌렀는지 여부 */
    @Transactional(readOnly = true)
    public boolean likedByMe(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        return commentLikeRepository.existsByCommentAndUser(comment, user);
    }
}
