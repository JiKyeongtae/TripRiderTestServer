package com.TripRider.TripRider.controller.travel;

import com.TripRider.TripRider.dto.custom.CategoryTreeDto;
import com.TripRider.TripRider.service.travel.CustomCategoryService; // 패키지 경로 본인 프로젝트에 맞춰 조정
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import java.util.Map;

@RestController
@RequestMapping("/api/custom")
@RequiredArgsConstructor
public class CustomCategoryController {

    private final CustomCategoryService categoryService;

    // 문자열 type -> contentTypeId 매핑
    private static final Map<String, Integer> TYPE = Map.of(
            "tour", 12,
            "culture", 14,
            "event", 15,
            "leports", 28,
            "stay", 32,
            "shop", 38,
            "food", 39
    );

    /**
     * 카테고리 트리 조회
     * 예)
     *  - /api/custom/categories?type=food
     *  - /api/custom/categories?type=39
     */
    @GetMapping("/categories")
    public CategoryTreeDto categories(@RequestParam String type) {
        Integer contentTypeId = resolveContentTypeId(type);
        return categoryService.getTreeForType(contentTypeId);
    }

    // 문자열/숫자 모두 지원하는 resolver
    private Integer resolveContentTypeId(String type) {
        if (type == null || type.isBlank()) {
            throw badRequest("type is required (e.g. food, tour, 39, 12)");
        }
        String key = type.trim().toLowerCase();

        // 1) 미리 정의된 키워드 매핑 우선
        if (TYPE.containsKey(key)) return TYPE.get(key);

        // 2) 숫자라면 그대로 contentTypeId로 사용
        try {
            int ctype = Integer.parseInt(key);
            if (ctype == 12 || ctype == 14 || ctype == 15 || ctype == 25 ||
                    ctype == 28 || ctype == 32 || ctype == 38 || ctype == 39) {
                return ctype;
            }
        } catch (NumberFormatException ignore) {
            // fall-through
        }

        // 3) 모두 실패 시 400
        throw badRequest("unknown type: " + type + " (allowed: tour, culture, event, leports, stay, shop, food, or 12/14/15/28/32/38/39)");
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}