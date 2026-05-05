package com.TripRider.TripRider.controller.board;

import com.TripRider.TripRider.domain.board.Post;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.board.PostRequest;
import com.TripRider.TripRider.dto.board.PostResponse;
import com.TripRider.TripRider.repository.user.UserRepository;
import com.TripRider.TripRider.service.board.CommentService;
import com.TripRider.TripRider.service.board.PostService;
import com.TripRider.TripRider.dto.board.CommentRequest;
import com.TripRider.TripRider.dto.board.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostApiController {

    private final PostService postService;
    private final CommentService commentService;
    private final UserRepository userRepository;

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }

    //  게시글 전체 조회
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts(
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User me = requireUser(userId);
        List<PostResponse> posts = postService.getAllPosts(me);
        return ResponseEntity.ok(posts);
    }

    //  게시글 등록
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody PostRequest request,
                                        @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        postService.create(
                request.getContent(),
                request.getImageUrl(),
                request.getLocation(),
                request.getHashtags(),
                me
        );
        return ResponseEntity.status(HttpStatus.CREATED).body("게시글 작성 완료");
    }

    //  게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id,
                                                @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        PostResponse res = postService.getPostById(id, me);
        return ResponseEntity.ok(res);
    }

    //  댓글 작성
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        @RequestBody CommentRequest req,
                                        @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        commentService.addComment(id, req.getContent(), me);
        return ResponseEntity.ok("댓글 등록 완료");
    }

    //  댓글 목록
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long id,
                                                             @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        List<CommentResponse> comments = commentService.getCommentsForPost(id, me);
        return ResponseEntity.ok(comments);
    }

    //  댓글 삭제
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long postId,
                                              @PathVariable Long commentId,
                                              @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        commentService.deleteComment(commentId, me);
        return ResponseEntity.noContent().build();
    }

    //  게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        postService.deletePost(id, me);
        return ResponseEntity.noContent().build();
    }

    //  게시글 좋아요 추가
    @PostMapping("/{id}/likes")
    public ResponseEntity<?> like(@PathVariable Long id,
                                  @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        int count = postService.likePost(id, me);
        return ResponseEntity.ok(Map.of("likeCount", count, "liked", true));
    }

    //  게시글 좋아요 취소
    @DeleteMapping("/{id}/likes")
    public ResponseEntity<?> unlike(@PathVariable Long id,
                                    @AuthenticationPrincipal(expression = "id") Long userId) {
        User me = requireUser(userId);
        int count = postService.unlikePost(id, me);
        return ResponseEntity.ok(Map.of("likeCount", count, "liked", false));
    }

    //  게시글 좋아요 개수 확인
    @GetMapping("/{id}/likes/count")
    public ResponseEntity<?> likeCount(@PathVariable Long id) {
        int count = postService.getLikeCount(id);
        return ResponseEntity.ok(Map.of("likeCount", count));
    }
}
