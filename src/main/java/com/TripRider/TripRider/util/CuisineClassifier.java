package com.TripRider.TripRider.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public class CuisineClassifier {

    // 간단 오버라이드 맵 (수동 보정)
    private static final Map<Long, Cuisine> OVERRIDE = new ConcurrentHashMap<>();

    // 키워드 패턴 (필요시 계속 보강)
    private static final Pattern P_KOREAN   = Pattern.compile("국밥|한정식|한식|백반|갈비|불고기|찌개|비빔밥|칼국수|냉면|삼겹|족발|보쌈|막창|감자탕|해장국|순댓국");
    private static final Pattern P_CHINESE  = Pattern.compile("중식|중화|짜장|자장|짬뽕|탕수육|마라|우육면|딤섬|훠궈");
    private static final Pattern P_JAPANESE = Pattern.compile("일식|스시|초밥|라멘|우동|사시미|덮밥|오마카세|텐동|돈카츠|규카츠");
    private static final Pattern P_WESTERN  = Pattern.compile("양식|파스타|피자|스테이크|버거|리조또|브런치|샐러드");
    // 한식에서 제외하고 싶은 대표적 비한식 키워드 (인도/태국 등)
    private static final Pattern P_NON_KOREAN_HINT = Pattern.compile("인도|커리|카레|난|탄두리|마살라|타이|태국");

    public static void putOverride(long contentId, Cuisine c) { OVERRIDE.put(contentId, c); }

    public static Cuisine guess(String... texts) {
        String src = String.join(" ", texts).toLowerCase();
        if (P_JAPANESE.matcher(src).find()) return Cuisine.JAPANESE;
        if (P_CHINESE.matcher(src).find())  return Cuisine.CHINESE;
        if (P_WESTERN.matcher(src).find())  return Cuisine.WESTERN;
        if (P_KOREAN.matcher(src).find())   return Cuisine.KOREAN;
        // 힌트: ‘한식’으로 태깅돼도 인도/태국 키워드가 강하면 기타로
        if (P_NON_KOREAN_HINT.matcher(src).find()) return Cuisine.ETC;
        return Cuisine.ETC;
    }

    public static Cuisine decide(Long contentId, String title, String addr,
                                 String firstmenu, String treatmenu, String overview) {
        if (contentId != null && OVERRIDE.containsKey(contentId)) {
            return OVERRIDE.get(contentId);
        }
        // 1차: 제목/주소만
        Cuisine c1 = guess(title, addr);
        // 2차 보강: 메뉴/개요가 있으면 점수 재평가
        if (firstmenu != null || treatmenu != null || overview != null) {
            Cuisine c2 = guess(title, addr, firstmenu==null?"":firstmenu,
                    treatmenu==null?"":treatmenu, overview==null?"":overview);
            // 보강 결과가 더 구체적이면 채택
            if (c2 != Cuisine.ETC) return c2;
        }
        return c1;
    }
}