package com.TripRider.TripRider.service.ride;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;

@Component
@RequiredArgsConstructor
public class LocalRouteImageStorage implements RouteImageStorage {

    /**
     * 업로드 루트 디렉터리
     *
     * 기본값을 프로젝트 루트 내부(${user.dir}/uploads)로 변경.
     * 운영/배포에서는 application.yml 또는 JVM 옵션(-Duploads.base-dir=...)으로
     * 외부 영속 경로를 지정하면 됨.
     */
    @Value("${uploads.base-dir:${user.dir}/uploads}")
    private String uploadsBaseDir;

    @Override
    public String store(Long rideId, MultipartFile file) {
        try {
            // {base}/rides/{rideId}/snapshot.png
            Path base = Paths.get(uploadsBaseDir).toAbsolutePath().normalize();
            Path dir = base.resolve("rides").resolve(String.valueOf(rideId));
            Files.createDirectories(dir);

            Path path = dir.resolve("snapshot.png");
            file.transferTo(path.toFile());

            // /uploads/** 정적서빙 URL과 일치
            return "/uploads/rides/" + rideId + "/snapshot.png";
        } catch (Exception e) {
            throw new RuntimeException("store failed", e);
        }
    }
}
