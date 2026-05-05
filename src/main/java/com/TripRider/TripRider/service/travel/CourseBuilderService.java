package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.custom.CoursePreview;
import com.TripRider.TripRider.dto.custom.CoursePreview.Waypoint;
import com.TripRider.TripRider.util.RouteUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseBuilderService {

    private final SelectionService selectionService;

    /** 바구니 기반 자동(또는 비최적화) 경로 생성 */
    public CoursePreview buildFromSelection(String selectionId, boolean optimize){
        var picked = selectionService.getAll(selectionId);
        List<Waypoint> wps = picked.stream().map(p ->
                Waypoint.builder()
                        .order(0).contentId(p.getContentId()).type(p.getType())
                        .title(p.getTitle()).lat(p.getLat()).lng(p.getLng())
                        .cat1(p.getCat1()).cat2(p.getCat2()).cat3(p.getCat3())
                        .contentTypeId(p.getContentTypeId()).build()
        ).collect(Collectors.toList());

        if (wps.isEmpty()) return CoursePreview.builder().waypoints(List.of()).distanceKm(0).durationMin(0).build();

        List<Waypoint> path = optimize ? RouteUtils.twoOpt(RouteUtils.nearestNeighbor(wps)) : wps;
        for (int i=0;i<path.size();i++) path.get(i).setOrder(i+1);

        double km = RouteUtils.totalKm(path);
        int minutes = (int)Math.round(km/35.0*60); // 평균 주행 35km/h 가정

        return CoursePreview.builder().waypoints(path).distanceKm(km).durationMin(minutes).polyline(null).build();
    }

    /** 사용자가 보낸 순서를 그대로 경로화 (선택적으로 2-opt) */
    public CoursePreview buildFromManual(List<Waypoint> ordered, boolean optimize){
        if (ordered==null || ordered.isEmpty())
            return CoursePreview.builder().waypoints(List.of()).distanceKm(0).durationMin(0).build();

        List<Waypoint> path = optimize ? RouteUtils.twoOpt(new ArrayList<>(ordered)) : ordered;
        for (int i=0;i<path.size();i++) path.get(i).setOrder(i+1);

        double km = RouteUtils.totalKm(path);
        int minutes = (int)Math.round(km/35.0*60);

        return CoursePreview.builder().waypoints(path).distanceKm(km).durationMin(minutes).polyline(null).build();
    }
}
