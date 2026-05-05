package com.TripRider.TripRider.controller.travel;

import com.TripRider.TripRider.config.UserPrincipal;
import com.TripRider.TripRider.dto.custom.*;
import com.TripRider.TripRider.service.travel.CourseBuilderService;
import com.TripRider.TripRider.service.travel.CustomCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/custom/courses")
@RequiredArgsConstructor
public class CustomCourseController {
    private final CourseBuilderService builderService;
    private final CustomCourseService courseService;

    @PostMapping("/auto")
    public CoursePreview auto(@RequestBody AutoCourseReq req) {
        return builderService.buildFromSelection(req.getSelectionId(), req.isOptimize());
    }

    @PostMapping("/manual/preview")
    public CoursePreview manualPreview(@RequestBody CoursePreview preview,
                                       @RequestParam(defaultValue = "true") boolean optimize) {
        return builderService.buildFromManual(preview.getWaypoints(), optimize);
    }

    // 저장 (헤더 제거)
    @PostMapping
    public CourseView save(@RequestBody SaveCourseReq req,
                           @AuthenticationPrincipal UserPrincipal me) {
        return courseService.save(req, me.id());
    }

    // 내 코스 리스트
    @GetMapping("/mine")
    public List<CourseCard> mine(@AuthenticationPrincipal UserPrincipal me,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return courseService.findMine(me.id(), page, size);
    }

    // 내 코스 상세 (소유자 검증 위해 userId 필요)
    @GetMapping("/{id}")
    public CourseView detail(@PathVariable String id,
                             @AuthenticationPrincipal UserPrincipal me) {
        return courseService.detailForOwner(id, me.id());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id,
                       @AuthenticationPrincipal UserPrincipal me) {
        courseService.deleteForOwner(id, me.id());
    }
}