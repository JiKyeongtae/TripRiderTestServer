package com.TripRider.TripRider.dto.board;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {
    private Long id;
    private String user;        // 작성자 닉네임
    private String content;     // 댓글 내용
    private String createdAt;   // 작성일
    private long likeCount;      // 좋아요 개수
    private boolean likedByMe;  // 내가 좋아요 눌렀는지
    private boolean mine;       // 내가 쓴 댓글인지
    private String profileImage;
}