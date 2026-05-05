package com.TripRider.TripRider.controller.travel;

import com.TripRider.TripRider.dto.custom.PlacePageDto;
import com.TripRider.TripRider.service.travel.PlaceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/custom")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceSearchService placeSearchService;

    @GetMapping("/places")
    public PlacePageDto search(
            @RequestParam String type,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") int radius,
            @RequestParam(required = false) String cat1,
            @RequestParam(required = false) String cat2,
            @RequestParam(required = false) String cat3,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "jeju") String scope,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return placeSearchService.search(type, lat, lng, radius, cat1, cat2, cat3, sort, scope, page, limit);
    }
}