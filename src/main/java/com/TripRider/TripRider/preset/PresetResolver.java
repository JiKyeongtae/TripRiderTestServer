package com.TripRider.TripRider.preset;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

public class PresetResolver {

    @Value @Builder
    public static class Filter {
        int contentTypeId;     // 12/14/15/28/32/38/39
        String cat1;           // nullable
        String cat2;           // nullable
        String cat3;           // nullable
        String keyword;        // nullable (폴백으로 searchKeyword2 호출)
    }

    public static final Map<String, Filter> PRESETS = Map.ofEntries(
            // 1) 관광지(12)
            entry("tour.nature",     12, "A01", "A0101", null, null),          // 자연
            entry("tour.history",    12, "A02", "A0201", null, "역사"),        // 역사
            entry("tour.experience", 12, "A03", "A0301", null, "체험"),        // 사용X

            // 2) 음식점(39)
            entry("food.korean",     39, "A05", "A0502", null, "한식"),       // 맛집으로 사용
            entry("food.chinese",    39, "A05", "A0503", null, "중식"),       // 사용X
            entry("food.japanese",   39, "A05", "A0504", null, "일식"),       // 사용X
            entry("food.western",    39, "A05", "A0505", null, "양식"),       // 사용X
            entry("food.etc",        39, "A05", "A0508", null, null),       // 사용X

            // 3) 레포츠(28)
            entry("leports.land",    28, "A03", "A0302", null, null),       // 육상
            entry("leports.water",   28, "A03", "A0303", null, null),       // 수상
            entry("leports.air",     28, "A03", "A0304", null, null),       // 사용X
            entry("leports.complex", 28, "A03", "A0307", null, null),       // 사용X

            // 4) 문화시설(14)
            entry("culture.museum",   14, "A02", "A0206", "A02060100", null),  // 박물관
            entry("culture.memorial", 14, "A02", "A0206", "A02060200", null),  // 기념관
            entry("culture.art",14, "A02", "A0206", "A02060500", null),  // 미술관

            // 5) 축제/행사(15) - 기관 세부코드 변동 가능성 대비 키워드 폴백
            entry("event.performance", 15, "A02", "A0207", "A02070100", "공연"), //공연 1개밖에 없음
            entry("event.exhibition",  15, "A02", "A0207", "A02070200", "전시회"), //축제
            entry("event.expo",        15, "A02", "A0207", "A02070300", "박람회"), //사용 X

            // 7) 쇼핑(38) - 지역 편차 대비 키워드 병행
            entry("shop.traditional",   38, "A04", "A0401", "A04010100", null), // 전통시장
            entry("shop.hypermart",    38, "A04", "A0401", "A04010500", null), // 사용 X
            entry("shop.dutyfree",  38, "A04", "A0401", "A04010400", null)  // 면세점
    );

    private static Map.Entry<String, Filter> entry(
            String k, int type, String c1, String c2, String c3, String kw) {
        return Map.entry(k, Filter.builder()
                .contentTypeId(type).cat1(c1).cat2(c2).cat3(c3).keyword(kw).build());
    }

    public static Filter of(String key) {
        Filter f = PRESETS.get(key);
        if (f == null) throw new IllegalArgumentException("Unknown preset: " + key);
        return f;
    }
}