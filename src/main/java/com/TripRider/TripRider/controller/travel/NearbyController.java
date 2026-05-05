package com.TripRider.TripRider.controller.travel;

import com.TripRider.TripRider.dto.common.NearbyPlaceDto;
import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import com.TripRider.TripRider.service.travel.CourseFileService;
import com.TripRider.TripRider.service.travel.TourApiService;
import com.TripRider.TripRider.util.CoursePointsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/travel/nearby")
public class NearbyController {

    private final CourseFileService courseFileService;
    private final TourApiService tourApiService;

    // 카테고리 코드: 관광지/문화/행사/레포츠/숙박/쇼핑/음식점
    private static final Map<String, Integer> TYPE = Map.of(
            "tour", 12, "culture", 14, "event", 15,
            "leports", 28, "stay", 32, "shop", 38, "food", 39
    );

    /**
     * 코스 기준으로 주변 장소 조회
     * mode=sme  : 시작·중간(경로길이 50%)·끝 3지점
     * mode=along: 경로를 count개로 균등 분할하여 샘플
     *
     * only 파라미터가 있으면 해당 카테고리(문자열 또는 숫자코드)만 리스트로 반환
     * 예) ?only=food  /  ?only=39
     */
    @GetMapping("/{category}/{id}")
    public Object nearbyByCourse(
            @PathVariable String category,
            @PathVariable Long id,
            @RequestParam(defaultValue = "3000") int radius,     // m
            @RequestParam(defaultValue = "8") int size,          // 지점당 가져올 개수
            @RequestParam(defaultValue = "sme") String mode,     // sme | along
            @RequestParam(defaultValue = "5") int count,         // mode=along 일 때 샘플 개수
            @RequestParam(required = false) String only          // 선택 카테고리 필터
    ) {
        RidingCourseDetailDto course = courseFileService.get(category, id);

        // 검색 포인트 결정
        List<RidingCourseDetailDto.LatLng> points =
                "along".equalsIgnoreCase(mode)
                        ? CoursePointsUtil.sampleByCount(course, Math.max(2, count))
                        : CoursePointsUtil.startMidEndByLength(course);

        // 총 반환 상한(지점수 * 지점당 size의 1.2배 정도)
        int maxTotal = Math.max(10, (int) Math.round(points.size() * size * 1.2));

        Map<String, List<NearbyPlaceDto>> result = new LinkedHashMap<>();
        TYPE.forEach((key, ctype) -> {
            List<NearbyPlaceDto> merged = tourApiService.mergedByPoints(
                    points, radius, ctype, size, maxTotal
            );
            result.put(key, merged);
        });

        // only 처리 (문자 키 or 숫자 코드 허용)
        return filterByOnly(result, only);
    }

    /**
     * 임의 좌표 기준 주변 장소 조회 (지도 중심 재검색 등)
     * 카테고리별로 한 번에 내려줌
     *
     * only 파라미터가 있으면 해당 카테고리(문자열 또는 숫자코드)만 리스트로 반환
     */
    @GetMapping("/point")
    public Object nearbyByPoint(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") int radius,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String only
    ) {
        var singlePoint = List.of(new RidingCourseDetailDto.LatLng(lat, lng));
        int maxTotal = size * TYPE.size();

        Map<String, List<NearbyPlaceDto>> result = new LinkedHashMap<>();
        TYPE.forEach((key, ctype) -> {
            List<NearbyPlaceDto> merged = tourApiService.mergedByPoints(
                    singlePoint, radius, ctype, size, maxTotal
            );
            result.put(key, merged);
        });

        // only 처리
        return filterByOnly(result, only);
    }

    /**
     * only 파라미터에 따라 전체 맵 또는 특정 카테고리 리스트만 반환
     * - only 가 null/blank 이면 전체 맵 그대로 반환
     * - only 가 "food" 같은 키면 해당 리스트 반환
     * - only 가 "39" 같은 숫자 코드면 매핑 후 리스트 반환
     * - 매칭 실패 시 전체 맵 반환 (원한다면 400 응답으로 바꿀 수 있음)
     */
    private Object filterByOnly(Map<String, List<NearbyPlaceDto>> result, String only) {
        if (only == null || only.isBlank()) return result;

        String k = only.trim().toLowerCase();
        if (result.containsKey(k)) {
            return result.get(k);
        }
        try {
            int ctype = Integer.parseInt(k);
            String key = TYPE.entrySet().stream()
                    .filter(e -> e.getValue() == ctype)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            if (key != null) return result.get(key);
        } catch (NumberFormatException ignore) {
            // not a number, fall through
        }
        // 잘못된 only 값이면 전체 맵 반환 (혹은 적절히 400으로 처리 가능)
        return result;
    }
}
