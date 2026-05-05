package com.TripRider.TripRider.controller.travel;

import com.TripRider.TripRider.config.UserPrincipal;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.riding.AiCourseSummaryDto;
import com.TripRider.TripRider.dto.riding.RidingCourseCardDto;
import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import com.TripRider.TripRider.repository.user.UserRepository;
import com.TripRider.TripRider.service.travel.AiCourseService;
import com.TripRider.TripRider.service.travel.CourseFileService;
import com.TripRider.TripRider.service.travel.RidingCourseLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travel/riding")
@RequiredArgsConstructor
public class RidingCourseController {

    private final CourseFileService courseFileService;
    private final RidingCourseLikeService likeService;
    private final UserRepository userRepository;
    private final AiCourseService aiCourseService;

    /** principal → 실제 User 엔티티 (없으면 null) */
    private User toUser(UserPrincipal principal) {
        if (principal == null) return null;
        return userRepository.findById(principal.id()).orElse(null);
    }

    /** 카드 목록 (내 위치 없으면 거리 null) */
    @GetMapping("/cards")
    public List<RidingCourseCardDto> cards(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User me = toUser(principal);
        var cards = courseFileService.listCards(lat, lng);
        // 좋아요 메타 주입
        return cards.stream().map(c -> {
            c.setLikeCount(likeService.count(c.getCategory(), c.getId()));
            c.setLiked(me != null && likeService.likedByMe(c.getCategory(), c.getId(), me));
            return c;
        }).toList();
    }

    /** 인기순: 좋아요 수 내림차순 */
    @GetMapping("/popular")
    public List<RidingCourseCardDto> popular(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User me = toUser(principal);
        return courseFileService.listCards(null, null).stream()
                .peek(c -> {
                    c.setLikeCount(likeService.count(c.getCategory(), c.getId()));
                    c.setLiked(me != null && likeService.likedByMe(c.getCategory(), c.getId(), me));
                })
                .sorted(Comparator.comparing(
                        (RidingCourseCardDto c) -> c.getLikeCount() == null ? 0 : c.getLikeCount()
                ).reversed())
                .limit(limit)
                .toList();
    }

    /** 거리순(=코스 총 길이 기준) 정렬 */
    @GetMapping("/by-length")
    public List<RidingCourseCardDto> byLength(
            @RequestParam(defaultValue = "asc") String order,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User me = toUser(principal);
        var stream = courseFileService.listCards(null, null).stream()
                .peek(c -> {
                    c.setLikeCount(likeService.count(c.getCategory(), c.getId()));
                    c.setLiked(me != null && likeService.likedByMe(c.getCategory(), c.getId(), me));
                });
        Comparator<RidingCourseCardDto> cmp = Comparator.comparingInt(RidingCourseCardDto::getTotalDistanceMeters);
        if ("desc".equalsIgnoreCase(order)) cmp = cmp.reversed();
        return stream.sorted(cmp).toList();
    }

    /** 상세: category + id (좋아요 메타 포함) */
    @GetMapping("/{category}/{id}")
    public RidingCourseDetailDto detail(
            @PathVariable String category,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User me = toUser(principal);
        var dto = courseFileService.get(category, id);
        // 좋아요 메타 주입
        dto.setLikeCount(likeService.count(category, id));
        dto.setLiked(me != null && likeService.likedByMe(category, id, me));
        return dto;
    }

    /** AI 코스 요약 */
    @GetMapping("/{category}/{id}/ai-summary")
    public AiCourseSummaryDto aiSummary(
            @PathVariable String category,
            @PathVariable Long id
    ) {
        return aiCourseService.generateSummary(category, id);
    }

    /** 좋아요 */
    @PostMapping("/{category}/{id}/likes")
    public Map<String, Object> like(
            @PathVariable String category,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User me = toUser(principal);
        if (me == null) throw new RuntimeException("로그인이 필요합니다.");
        int cnt = likeService.like(category, id, me);
        return Map.of("likeCount", cnt, "liked", true);
    }

    /** 좋아요 취소 */
    @DeleteMapping("/{category}/{id}/likes")
    public Map<String, Object> unlike(
            @PathVariable String category,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User me = toUser(principal);
        if (me == null) throw new RuntimeException("로그인이 필요합니다.");
        int cnt = likeService.unlike(category, id, me);
        return Map.of("likeCount", cnt, "liked", false);
    }
}
