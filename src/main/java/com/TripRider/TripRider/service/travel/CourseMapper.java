package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.domain.travel.CourseEntity;
import com.TripRider.TripRider.domain.travel.WaypointEntity;
import com.TripRider.TripRider.dto.custom.CourseCard;
import com.TripRider.TripRider.dto.custom.CoursePreview;
import com.TripRider.TripRider.dto.custom.CourseView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CourseMapper {

    private CourseMapper() {}

    public static CourseEntity toEntity(String id, Long userId, String title,
                                        Double distanceKm, Integer durationMin,
                                        String polyline, List<CoursePreview.Waypoint> wps) {

        CourseEntity e = CourseEntity.builder()
                .id(id)
                .userId(userId)
                .title(title)
                .distanceKm(distanceKm)
                .durationMin(durationMin)
                .polyline(polyline)
                .createdAt(java.time.LocalDateTime.now())
                .waypoints(new ArrayList<>())
                .build();

        if (wps != null) {
            for (CoursePreview.Waypoint w : wps) {
                WaypointEntity we = WaypointEntity.builder()
                        .ordIdx(w.getOrder())
                        .contentId(w.getContentId())
                        .type(w.getType())
                        .title(w.getTitle())
                        .lat(w.getLat())
                        .lng(w.getLng())
                        .cat1(w.getCat1())
                        .cat2(w.getCat2())
                        .cat3(w.getCat3())
                        .contentTypeId(w.getContentTypeId())
                        .build();
                e.addWaypoint(we);
            }
        }
        return e;
    }

    public static CourseView toView(CourseEntity e) {
        List<CoursePreview.Waypoint> wps = e.getWaypoints()==null ? new ArrayList<>() :
                e.getWaypoints().stream().filter(Objects::nonNull).map(we ->
                        CoursePreview.Waypoint.builder()
                                .order(we.getOrdIdx())
                                .contentId(we.getContentId())
                                .type(we.getType())
                                .title(we.getTitle())
                                .lat(we.getLat())
                                .lng(we.getLng())
                                .cat1(we.getCat1())
                                .cat2(we.getCat2())
                                .cat3(we.getCat3())
                                .contentTypeId(we.getContentTypeId())
                                .build()
                ).collect(Collectors.toList());

        return CourseView.builder()
                .id(e.getId())
                .title(e.getTitle())
                .waypoints(wps)
                .distanceKm(e.getDistanceKm()==null?0.0:e.getDistanceKm())
                .durationMin(e.getDurationMin()==null?0:e.getDurationMin())
                .polyline(e.getPolyline())
                .createdAt(e.getCreatedAt()==null?null:e.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    public static CourseCard toCard(CourseEntity e) {
        String preview = (e.getWaypoints()==null || e.getWaypoints().isEmpty()) ? "" :
                e.getWaypoints().stream()
                        .limit(3)
                        .map(WaypointEntity::getTitle)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" → "));

        return CourseCard.builder()
                .id(e.getId())
                .title(e.getTitle())
                .stopsPreview(preview)
                .createdAt(e.getCreatedAt()==null?null:e.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
