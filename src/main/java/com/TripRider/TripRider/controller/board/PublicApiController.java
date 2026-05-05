package com.TripRider.TripRider.controller.board;

import com.TripRider.TripRider.domain.board.Post;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.board.PostResponse;
import com.TripRider.TripRider.dto.ride.StatsSummaryDto;
import com.TripRider.TripRider.repository.board.CommentRepository;
import com.TripRider.TripRider.repository.board.PostLikeRepository;
import com.TripRider.TripRider.repository.board.PostRepository;
import com.TripRider.TripRider.repository.user.UserRepository;
import com.TripRider.TripRider.service.ride.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicApiController {

    private final UserRepository userRepository;
    private final RideService rideService;

    // 게시물 매핑에 필요 (좋아요/댓글수)
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    // ---------- 유저 기본 프로필 ----------
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getPublicUser(@PathVariable Long id) {
        User u = userRepository.findById(id).orElseThrow();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", u.getId());
        body.put("nickname", u.getNickname());
        body.put("intro", u.getIntro());
        body.put("profileImage", u.getProfileImage());
        body.put("totalDistance", u.getTotalDistance()); // 앱에서 rides/summary로 보강해서 사용
        body.put("region", "제주도"); // 기본 표기
        body.put("representativeBadge", u.getRepresentativeBadge());
        return ResponseEntity.ok(body);
    }

    // ---------- 닉네임으로 userId ----------
    @GetMapping("/users/by-nickname")
    public ResponseEntity<Map<String, Object>> getUserIdByNickname(@RequestParam String nickname) {
        User u = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new NoSuchElementException("user not found"));
        return ResponseEntity.ok(Map.of("userId", u.getId(), "nickname", u.getNickname()));
    }

    // ---------- 특정 유저의 라이딩 요약(누적 km/시간) ----------
    @GetMapping("/rides/summary")
    public ResponseEntity<StatsSummaryDto> getSummary(@RequestParam("userId") Long userId) {
        User u = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(rideService.summary(u));
    }

    // ---------- 특정 유저의 게시물 목록 ----------
    @GetMapping("/users/{id}/posts")
    public ResponseEntity<List<PostResponse>> postsByUser(@PathVariable Long id) {
        // 현재 조회자가 누구든 상관없이 공개 목록 제공 (liked=false 고정)
        List<PostResponse> list = postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(p -> p.getUser() != null && Objects.equals(p.getUser().getId(), id))
                .map(this::toPostResponsePublic)
                .toList();
        return ResponseEntity.ok(list);
    }

    // 쿼리 파라미터 버전 ?userId=
    @GetMapping("/posts")
    public ResponseEntity<List<PostResponse>> postsByQuery(@RequestParam(required = false) Long userId) {
        List<Post> all = postRepository.findAllByOrderByCreatedAtDesc();
        if (userId != null) {
            all = all.stream()
                    .filter(p -> p.getUser() != null && Objects.equals(p.getUser().getId(), userId))
                    .toList();
        }
        List<PostResponse> list = all.stream().map(this::toPostResponsePublic).toList();
        return ResponseEntity.ok(list);
    }

    // ---------- 내부 유틸: 공개용 PostResponse ----------
    private PostResponse toPostResponsePublic(Post p) {
        var u = p.getUser();
        String writer = (u != null && u.getNickname() != null && !u.getNickname().isBlank())
                ? u.getNickname() : "익명";
        String profileImage = (u != null) ? u.getProfileImage() : null;
        int comments = commentRepository.countByPost(p);
        long likeCnt = postLikeRepository.countByPost(p);

        return PostResponse.builder()
                .id(p.getId())
                .content(p.getContent())
                .imageUrl(p.getImageUrl())
                .location(p.getLocation())
                .hashtags(p.getHashtags())
                .writer(writer)
                .profileImage(profileImage)
                .likeCount((int) likeCnt)
                .liked(false) // 공개 목록에서는 내가 좋아요 눌렀는지 알 수 없음
                .commentCount(comments)
                .build();
    }
}
