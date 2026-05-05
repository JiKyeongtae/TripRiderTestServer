package com.TripRider.TripRider.controller.ride;

import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.ride.*;
import com.TripRider.TripRider.repository.user.UserRepository;
import com.TripRider.TripRider.service.ride.RideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    @PostMapping("/start")
    public ResponseEntity<StartRideResponse> start(
            @AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(rideService.start(requireUser(userId)));
    }

    @PostMapping("/{rideId}/points")
    public ResponseEntity<Void> append(@PathVariable Long rideId,
                                       @RequestBody RidePointRequest request,
                                       @AuthenticationPrincipal(expression = "id") Long userId) {
        rideService.appendPoints(rideId, requireUser(userId), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{rideId}/finish", consumes = "application/json")
    public ResponseEntity<RideSummaryDto> finishJson(@PathVariable Long rideId,
                                                     @RequestBody(required = false) FinishRideRequest body,
                                                     @AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(rideService.finish(rideId, requireUser(userId), body));
    }

    @PostMapping(value = "/{rideId}/finish", consumes = "multipart/form-data")
    public ResponseEntity<RideSummaryDto> finish(@PathVariable Long rideId,
                                                 @RequestPart(value = "body", required = false) String bodyStr,
                                                 @RequestPart(value = "snapshot", required = false) MultipartFile snapshot,
                                                 @AuthenticationPrincipal(expression = "id") Long userId) {
        FinishRideRequest body = new FinishRideRequest();
        try {
            if (bodyStr != null && !bodyStr.isBlank()) {
                body = objectMapper.readValue(bodyStr, FinishRideRequest.class);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(rideService.finish(rideId, requireUser(userId), body));
    }

    @GetMapping
    public ResponseEntity<List<RideSummaryDto>> list(
            @AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(rideService.list(requireUser(userId)));
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideDetailDto> detail(@PathVariable Long rideId,
                                                @RequestParam(defaultValue = "false") boolean withPolyline,
                                                @AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(rideService.detail(rideId, requireUser(userId), withPolyline));
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<StatsSummaryDto> summary(
            @AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(rideService.summary(requireUser(userId)));
    }
}
