package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.common.NearbyPlaceDto;
import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import com.TripRider.TripRider.dto.custom.PlacePageDto;
import com.TripRider.TripRider.util.GeoUtil;
import com.TripRider.TripRider.util.JejuGridPointsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private final TourApiService tourApi;

    private static final Map<String,Integer> TYPE = Map.of(
            "tour",12,"culture",14,"event",15,"leports",28,"stay",32,"shop",38,"food",39
    );

    public PlacePageDto search(String type, double lat, double lng, int radius,
                               String cat1, String cat2, String cat3,
                               String sort, String scope, int page, int limit) {

        int ctype = TYPE.getOrDefault(type, 12);
        List<NearbyPlaceDto> items;

        switch (scope==null?"jeju":scope.toLowerCase()){
            case "local" -> {
                items = tourApi.locationBasedList(lat, lng, radius, ctype, limit, page)
                        .stream().filter(p -> GeoUtil.isInJeju(p.getLat(), p.getLng())).toList();
            }
            case "grid" -> {
                List<RidingCourseDetailDto.LatLng> grid = JejuGridPointsUtil.grid12();
                items = tourApi.mergedByPoints(grid, Math.max(7000, radius), ctype, limit, limit*6)
                        .stream().filter(p -> GeoUtil.isInJeju(p.getLat(), p.getLng())).toList();
            }
            default -> { // jeju
                items = new ArrayList<>();
                items.addAll(tourApi.areaBasedList(39, null, ctype, cat1, cat2, cat3, limit, page, "B"));
                items = items.stream().filter(p -> GeoUtil.isInJeju(p.getLat(), p.getLng())).toList();
            }
        }

        // 카테고리 필터(응답에 cat필드가 들어온 경우 한 번 더 커팅)
        if (cat1 != null) items = items.stream().filter(i -> cat1.equals(i.getCat1())).toList();
        if (cat2 != null) items = items.stream().filter(i -> cat2.equals(i.getCat2())).toList();
        if (cat3 != null) items = items.stream().filter(i -> cat3.equals(i.getCat3())).toList();

        Comparator<NearbyPlaceDto> cmp = switch (sort==null?"distance":sort){
            case "popularity" -> Comparator.comparing(NearbyPlaceDto::getTitle); // placeholder
            case "rating" -> Comparator.comparing(NearbyPlaceDto::getTitle);     // placeholder
            default -> Comparator.comparing(p -> Optional.ofNullable(p.getDistMeters()).orElse(Integer.MAX_VALUE));
        };

        items = items.stream().sorted(cmp).limit(limit).collect(Collectors.toList());
        return PlacePageDto.builder().items(items).page(page).total(-1).build();
    }
}
