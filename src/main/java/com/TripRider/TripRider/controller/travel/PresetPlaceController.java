package com.TripRider.TripRider.controller.travel;

import com.TripRider.TripRider.dto.custom.PlacePageDto;
import com.TripRider.TripRider.service.travel.PresetPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/custom")
@RequiredArgsConstructor
public class PresetPlaceController {

    private final PresetPlaceService service;

    /**
     * 맞춤형 코스 전용 프리셋 검색
     * 예) /api/custom/places/preset?key=food.korean&page=1&limit=20
     */
    @GetMapping("/places/preset")
    public PlacePageDto placesByPreset(
            @RequestParam String key,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return service.searchByPreset(key, page, limit);
    }
}
