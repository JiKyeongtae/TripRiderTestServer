package com.TripRider.TripRider.service.ride;

import com.TripRider.TripRider.domain.ride.*;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.ride.*;
import com.TripRider.TripRider.repository.ride.*;
import com.TripRider.TripRider.util.GeoUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideSessionRepository rideSessionRepository;
    private final RidePointRepository ridePointRepository;
    private final MapImageService mapImageService;   // 🔹 Static Map API 호출 서비스

    @Transactional
    public StartRideResponse start(User user) {
        var existing = rideSessionRepository.findTopByUserAndFinishedFalseOrderByCreatedAtDesc(user);
        if (existing.isPresent()) {
            var ride = existing.get();
            return StartRideResponse.builder()
                    .rideId(ride.getId())
                    .startEpochMillis(Instant.now().toEpochMilli())
                    .build();
        }

        RideSession ride = RideSession.builder()
                .user(user)
                .startedAt(LocalDateTime.now())
                .finished(false)
                .build();
        rideSessionRepository.save(ride);

        return StartRideResponse.builder()
                .rideId(ride.getId())
                .startEpochMillis(Instant.now().toEpochMilli())
                .build();
    }

    @Transactional
    public void appendPoints(Long rideId, User user, RidePointRequest req) {
        RideSession ride = getOwnedRide(rideId, user);
        if (ride.isFinished()) throw new IllegalStateException("종료된 세션");

        long seqBase = ridePointRepository.countByRide(ride);

        for (RidePointRequest.Point p : req.getPoints()) {
            RidePoint point = RidePoint.builder()
                    .ride(ride)
                    .seq(seqBase + p.getSeq())
                    .latitude(p.getLat())
                    .longitude(p.getLng())
                    .altitude(p.getAltitude())
                    .speedKmh(p.getSpeedKmh())
                    .epochMillis(p.getEpochMillis())
                    .build();
            ridePointRepository.save(point);
        }
    }

    @Transactional
    public RideSummaryDto finish(Long rideId, User user, FinishRideRequest body) {
        RideSession ride = getOwnedRide(rideId, user);
        if (ride.isFinished()) return toSummary(ride);

        var points = ridePointRepository.findByRideOrderBySeqAsc(ride);
        double totalMeters = 0d;
        long movingMs = 0L;
        double maxSpeedKmh = 0d;

        final double SPEED_MIN_MOVING_KMH = 1.0;
        final double SPEED_HARD_CAP_KMH = 1e6;

        for (int i = 1; i < points.size(); i++) {
            var p1 = points.get(i - 1);
            var p2 = points.get(i);

            long dtMs = Math.max(0, p2.getEpochMillis() - p1.getEpochMillis());
            if (dtMs == 0) continue;

            double meters = GeoUtil.haversineKm(
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude()
            ) * 1000.0;

            double vKmh = (meters / (dtMs / 1000.0)) * 3.6;

            if (vKmh > SPEED_HARD_CAP_KMH) continue;

            totalMeters += meters;
            maxSpeedKmh = Math.max(maxSpeedKmh, vKmh);

            if (vKmh >= SPEED_MIN_MOVING_KMH) {
                movingMs += dtMs;
            }
        }

        long movingSeconds = movingMs / 1000;
        double totalKm = totalMeters / 1000.0;
        double avgSpeedKmh = (movingSeconds > 0) ? (totalKm / (movingSeconds / 3600.0)) : 0.0;

        ride.setFinished(true);
        ride.setFinishedAt(LocalDateTime.now());
        if (body != null) {
            ride.setTitle(body.getTitle());
            ride.setMemo(body.getMemo());
        }

        ride.setTotalDistanceMeters(totalMeters);
        ride.setMovingSeconds(movingSeconds);
        ride.setAvgSpeedKmh(avgSpeedKmh);
        ride.setMaxSpeedKmh(maxSpeedKmh);

        //  Static Map API 이용해서 경로 이미지 저장
        if (!points.isEmpty()) {
            try {
                String url = mapImageService.generateAndSaveMap(rideId, points);
                ride.setRouteImageUrl(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        rideSessionRepository.save(ride);
        return toSummary(ride);
    }

    public List<RideSummaryDto> list(User user) {
        return rideSessionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public RideDetailDto detail(Long rideId, User user, boolean withPolyline) {
        RideSession ride = getOwnedRide(rideId, user);

        RideDetailDto.RideDetailDtoBuilder b = RideDetailDto.builder()
                .id(ride.getId())
                .startedAt(ride.getStartedAt())
                .finishedAt(ride.getFinishedAt())
                .totalKm(ride.getTotalDistanceMeters() / 1000.0)
                .movingSeconds(ride.getMovingSeconds())
                .avgSpeedKmh(ride.getAvgSpeedKmh())
                .maxSpeedKmh(ride.getMaxSpeedKmh())
                .routeImageUrl(ride.getRouteImageUrl())
                .title(ride.getTitle())
                .memo(ride.getMemo());

        if (withPolyline) {
            var pts = ridePointRepository.findByRideOrderBySeqAsc(ride);
            b.polyline(pts.stream()
                    .map(p -> new RideDetailDto.LatLng(p.getLatitude(), p.getLongitude()))
                    .toList());
        }
        return b.build();
    }

    public StatsSummaryDto summary(User user) {
        var list = rideSessionRepository.findByUserOrderByCreatedAtDesc(user);
        double meters = list.stream().mapToDouble(RideSession::getTotalDistanceMeters).sum();
        long seconds = list.stream().mapToLong(RideSession::getMovingSeconds).sum();
        return new StatsSummaryDto(list.size(), meters / 1000.0, seconds);
    }

    private RideSession getOwnedRide(Long rideId, User user) {
        var ride = rideSessionRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("ride not found"));
        if (!ride.getUser().getId().equals(user.getId()))
            throw new SecurityException("forbidden");
        return ride;
    }

    private RideSummaryDto toSummary(RideSession r) {
        return RideSummaryDto.builder()
                .id(r.getId())
                .startedAt(r.getStartedAt())
                .finishedAt(r.getFinishedAt())
                .totalKm(r.getTotalDistanceMeters() / 1000.0)
                .movingSeconds(r.getMovingSeconds())
                .avgSpeedKmh(r.getAvgSpeedKmh())
                .maxSpeedKmh(r.getMaxSpeedKmh())
                .routeImageUrl(r.getRouteImageUrl())
                .title(r.getTitle())
                .build();
    }
}
