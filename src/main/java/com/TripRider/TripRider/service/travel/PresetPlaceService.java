package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.common.NearbyPlaceDto;
import com.TripRider.TripRider.dto.custom.PlacePageDto;
import com.TripRider.TripRider.preset.PresetResolver;
import com.TripRider.TripRider.util.Cuisine;
import com.TripRider.TripRider.util.CuisineClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresetPlaceService {

    private final TourApiService tour;

    public PlacePageDto searchByPreset(String presetKey, int page, int limit) {
        var f = PresetResolver.of(presetKey);

        log.info("[Preset] key={} ctype={} cat1={} cat2={} cat3={} page={} limit={}",
                presetKey, f.getContentTypeId(), f.getCat1(), f.getCat2(), f.getCat3(), page, limit);

        // 1) 제주 전역 + 정밀 카테고리 (조회순 B)
        List<NearbyPlaceDto> items = tour.areaBasedList(
                39, null, f.getContentTypeId(),
                f.getCat1(), f.getCat2(), f.getCat3(),
                limit, page, "B"
        );

        boolean isFood = (f.getContentTypeId() == 39);

        // 2) (비-음식만) cat2 완화 폴백
        if (!isFood && (items == null || items.isEmpty()) && f.getCat2() != null) {
            log.info("[Preset] fallback widen: cat2 -> null");
            items = tour.areaBasedList(
                    39, null, f.getContentTypeId(),
                    f.getCat1(), null, f.getCat3(),
                    limit, page, "B"
            );
        }

        // 2-1) 체험 전용 대체 폴백: 관광지(12, A03)가 비면 레포츠(28, A03)로 시도
        if ((items == null || items.isEmpty()) && "tour.experience".equals(presetKey)) {
            log.info("[Preset] experience alt fallback: switch to ctype=28, cat1=A03");
            items = tour.areaBasedList(
                    39, null,
                    /* contentTypeId */ 28,
                    /* cat1/2/3 */     "A03", null, null,
                    /* size/page */     limit, page,
                    /* arrange */       "B"
            );
        }

        // 3) 키워드 폴백
        if (items == null || items.isEmpty()) {
            String kw = f.getKeyword();

            // 음식이면 기본 키워드가 없을 때 확장 키워드 적용
            if (isFood && (kw == null || kw.isBlank())) {
                kw = keywordForFood(presetKey); // 예: 일식|스시|초밥 ...
            }

            if (kw != null && !kw.isBlank()) {
                log.info("[Preset] fallback keyword='{}' (food={}, keepCat2={})",
                        kw, isFood, !isFood);

                if (isFood) {
                    // ⚠ 음식: cat2를 제거해서(더 넓게) 키워드 검색
                    items = tour.searchKeyword(
                            kw, 39, null,
                            f.getContentTypeId(),
                            /* cat1 */ f.getCat1(),
                            /* cat2 */ null,          // ★ cat2 제거
                            /* cat3 */ null,
                            limit, page, "B"
                    );
                } else {
                    // 비-음식: cat 필터 유지한 채 키워드 보완
                    items = tour.searchKeyword(
                            kw, 39, null,
                            f.getContentTypeId(),
                            f.getCat1(), f.getCat2(), f.getCat3(),
                            limit, page, "B"
                    );
                }
            }
        }

        if (items == null) items = new ArrayList<>();
        int total = items.size(); // 임시 total (추후 totalCount 파싱 권장)

        if (isFood) {
            // 기대 카테고리 매핑
            Cuisine expected = switch (presetKey) {
                case "food.korean"   -> Cuisine.KOREAN;
                case "food.chinese"  -> Cuisine.CHINESE;
                case "food.japanese" -> Cuisine.JAPANESE;
                case "food.western"  -> Cuisine.WESTERN;
                default              -> Cuisine.ETC;
            };

            var kept        = new ArrayList<NearbyPlaceDto>(); // 기본은 유지
            var needCheck   = new ArrayList<NearbyPlaceDto>(); // “다른 종”으로 강하게 의심되는 것만

            for (var i : items) {
                String t = i.getTitle() == null ? "" : i.getTitle();
                String a = i.getAddr()  == null ? "" : i.getAddr();
                Cuisine quick = CuisineClassifier.guess(t, a);

                if (quick == expected || quick == Cuisine.ETC) {
                    // 기대와 일치 or 애매하면 일단 유지
                    kept.add(i);
                } else {
                    // 다른 종으로 강한 시그널 → 상세로 재확인
                    needCheck.add(i);
                }
            }

            int budget = Math.min(needCheck.size(), 8);
            for (int k = 0; k < budget; k++) {
                var i = needCheck.get(k);
                var d = tour.fetchFoodDetail(i.getContentId());  // 실패 가능성 고려
                Cuisine decided = CuisineClassifier.decide(
                        i.getContentId(),
                        i.getTitle() == null ? "" : i.getTitle(),
                        i.getAddr()  == null ? "" : i.getAddr(),
                        d.firstmenu(), d.treatmenu(), d.overview()
                );
                if (decided == expected || decided == Cuisine.ETC) {
                    kept.add(i);       // 여전히 애매/일치 → 유지
                } // else: 명확히 다른 종이면만 제외
            }

            // 상세 못 본 나머지 needCheck는 “빼지 말고” 일단 유지(보수적)
            for (int k = budget; k < needCheck.size(); k++) {
                kept.add(needCheck.get(k));
            }

            items = kept;
        }

        //  프리셋 cat 값으로 서버에서 한 번 더 거르기 (null은 무시)
        if (f.getCat1() != null) items = items.stream().filter(i -> f.getCat1().equals(i.getCat1())).toList();
        if (f.getCat2() != null) items = items.stream().filter(i -> f.getCat2().equals(i.getCat2())).toList();
        if (f.getCat3() != null) items = items.stream().filter(i -> f.getCat3().equals(i.getCat3())).toList();

        return PlacePageDto.builder()
                .items(items)
                .page(page)
                .total(total)
                .build();
    }

    private String keywordForFood(String presetKey) {
        if (presetKey.endsWith("korean"))   return "한식";
        if (presetKey.endsWith("chinese"))  return "중식|중화요리|짜장|짬뽕|탕수육";
        if (presetKey.endsWith("japanese")) return "일식|스시|초밥|라멘|오마카세";
        if (presetKey.endsWith("western"))  return "양식|파스타|피자|스테이크|버거";
        if (presetKey.endsWith("etc"))      return "분식|퓨전";
        return null;
    }
}
