package com.TripRider.TripRider.controller.mypage;

import com.TripRider.TripRider.domain.user.Badge;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.mypage.MyPageResponse;
import com.TripRider.TripRider.dto.mypage.MyPageUpdateRequest;
import com.TripRider.TripRider.dto.mypage.RepresentativeBadgeRequest;
import com.TripRider.TripRider.repository.user.UserRepository;
import com.TripRider.TripRider.service.mypage.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageApiController {

    private final MyPageService myPageService;
    private final UserRepository userRepository; // ★ principal(id)로 조회

    // 공통: userId로 현재 사용자 로드(+401/404 처리)
    private User requireUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }

    //  마이페이지 정보 조회
    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPage(
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User user = requireUser(userId);

        String representativeBadge = user.getRepresentativeBadge();
        if (representativeBadge == null && user.getBadge() != null) {
            representativeBadge = user.getBadge(); // 최근 획득 뱃지를 기본 대표로
        }

        MyPageResponse response = MyPageResponse.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .intro(user.getIntro())
                .profileImage(user.getProfileImage())
                .totalDistance(user.getTotalDistance())
                .representativeBadge(representativeBadge) // DTO가 이 필드를 갖고 있음
                .build();

        return ResponseEntity.ok(response);
    }

    //  마이페이지 정보 수정
    @PutMapping
    public ResponseEntity<?> updateMyPage(
            @RequestBody MyPageUpdateRequest request,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User user = requireUser(userId);
        myPageService.updateProfile(request.getNickname(), request.getIntro(), request.getBadge(), user);
        return ResponseEntity.ok("마이페이지 수정 완료");
    }

    //  한줄 소개만 수정
    @PutMapping("/intro")
    public ResponseEntity<?> updateIntro(
            @RequestBody String intro,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User user = requireUser(userId);
        myPageService.updateIntro(intro, user);
        return ResponseEntity.ok("한줄소개 수정 완료");
    }

    // 🔹 프로필 이미지 업로드
    @PostMapping("/profile-image")
    public ResponseEntity<String> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User user = requireUser(userId);
        String url = myPageService.updateProfileImage(user, image);
        return ResponseEntity.ok(url);
    }

    //  대표 뱃지 선택
    @PutMapping("/representative-badge")
    public ResponseEntity<?> updateRepresentativeBadge(
            @RequestBody RepresentativeBadgeRequest request,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User user = requireUser(userId);
        myPageService.updateRepresentativeBadge(user, request.getBadgeName());
        return ResponseEntity.ok("대표 뱃지 변경 완료");
    }

    //  내가 가진 뱃지 전체 조회
    @GetMapping("/badges")
    public ResponseEntity<List<String>> getMyBadges(
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User user = requireUser(userId);
        List<String> badges = myPageService.getUserBadges(user).stream()
                .map(Badge::getName)
                .toList();
        return ResponseEntity.ok(badges);
    }

    //  [테스트용] 거리 기반 뱃지 지급 강제 실행
    @PostMapping("/badges/check")
    public ResponseEntity<?> checkBadges(
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        User user = requireUser(userId);
        myPageService.checkAndGiveDistanceBadge(user);
        return ResponseEntity.ok("뱃지 체크 완료 ✅");
    }
}
