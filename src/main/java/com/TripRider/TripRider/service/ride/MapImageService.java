package com.TripRider.TripRider.service.ride;

import com.TripRider.TripRider.domain.ride.RidePoint;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapImageService {

    private final OkHttpClient client = new OkHttpClient();

    @Value("${google.api.key}")
    private String googleApiKey;

    // Google Static Maps 기본 옵션
    private static final int DEFAULT_WIDTH = 640;   // 최대 640
    private static final int DEFAULT_HEIGHT = 640;  // scale=2 → 1280x1280
    private static final int DEFAULT_SCALE = 2;     // 1 or 2
    private static final int MAX_PATH_POINTS = 80;  // URL 길이 보호용 샘플링

    /**
     * 기본 버전 (경로 + 시작/끝 마커)
     */
    public String generateAndSaveMap(Long rideId, List<RidePoint> points) throws IOException {
        MapOptions opt = MapOptions.defaults();
        return generateAndSaveMap(rideId, points, opt);
    }

    /**
     * 옵션 버전: 크기, 지도타입, 경로 스타일, 마커 등 세밀 제어 가능
     */
    public String generateAndSaveMap(Long rideId, List<RidePoint> points, MapOptions opt) throws IOException {
        if (points == null || points.isEmpty()) return null;

        // URL 길이 방지를 위해 포인트 샘플링
        List<RidePoint> usePoints = downSample(points, MAX_PATH_POINTS);

        // --- path 파라미터 ---
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append("color:").append(opt.pathColor)
                .append("|weight:").append(opt.pathWeight);
        for (RidePoint p : usePoints) {
            pathBuilder.append("|")
                    .append(p.getLatitude()).append(",").append(p.getLongitude());
        }
        String pathParam = URLEncoder.encode(pathBuilder.toString(), StandardCharsets.UTF_8);

        // --- markers 파라미터 ---
        List<String> markerParams = new ArrayList<>();
        if (opt.showStartMarker && !usePoints.isEmpty()) {
            RidePoint s = usePoints.get(0);
            String startMarker = "color:" + opt.startMarkerColor
                    + "|label:" + opt.startMarkerLabel
                    + "|" + s.getLatitude() + "," + s.getLongitude();
            markerParams.add("markers=" + URLEncoder.encode(startMarker, StandardCharsets.UTF_8));
        }
        if (opt.showEndMarker && !usePoints.isEmpty()) {
            RidePoint e = usePoints.get(usePoints.size() - 1);
            String endMarker = "color:" + opt.endMarkerColor
                    + "|label:" + opt.endMarkerLabel
                    + "|" + e.getLatitude() + "," + e.getLongitude();
            markerParams.add("markers=" + URLEncoder.encode(endMarker, StandardCharsets.UTF_8));
        }
        if (opt.extraMarkers != null && !opt.extraMarkers.isEmpty()) {
            for (ExtraMarker m : opt.extraMarkers) {
                String mk = "color:" + m.color
                        + (m.label != null && !m.label.isBlank() ? "|label:" + m.label : "")
                        + "|" + m.lat + "," + m.lng;
                markerParams.add("markers=" + URLEncoder.encode(mk, StandardCharsets.UTF_8));
            }
        }

        // --- visible 파라미터 (경로 전체 보이도록 자동 zoom) ---
        StringBuilder visibleBuilder = new StringBuilder();
        for (RidePoint p : usePoints) {
            if (visibleBuilder.length() > 0) visibleBuilder.append("|");
            visibleBuilder.append(p.getLatitude()).append(",").append(p.getLongitude());
        }
        String visibleParam = URLEncoder.encode(visibleBuilder.toString(), StandardCharsets.UTF_8);

        int width = clamp(opt.width, 1, DEFAULT_WIDTH);
        int height = clamp(opt.height, 1, DEFAULT_HEIGHT);
        int scale = (opt.scale == 2) ? 2 : 1;

        // --- 최종 URL 조립 ---
        StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/staticmap");
        url.append("?size=").append(width).append("x").append(height);
        url.append("&scale=").append(scale);
        url.append("&maptype=").append(opt.mapType); // roadmap|satellite|terrain|hybrid
        url.append("&path=").append(pathParam);
        for (String mp : markerParams) {
            url.append("&").append(mp);
        }
        url.append("&visible=").append(visibleParam);
        url.append("&key=").append(googleApiKey);

        System.out.println("📌 Google Static Map 요청 URL: " + url);

        // API 호출
        Request request = new Request.Builder().url(url.toString()).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Google Static Map API 호출 실패: "
                        + response.code() + " " + response.message());
            }

            // 저장 경로
            String uploadDir = System.getProperty("user.home") + "/triprider-uploads/rides/" + rideId;
            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("이미지 저장 폴더 생성 실패: " + uploadDir);
            }

            String filePath = uploadDir + "/map.png";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(response.body().bytes());
            }

            return "/uploads/rides/" + rideId + "/map.png";
        }
    }

    private List<RidePoint> downSample(List<RidePoint> src, int max) {
        if (src.size() <= max) return src;
        int step = (int) Math.ceil(src.size() / (double) max);
        ArrayList<RidePoint> out = new ArrayList<>();
        for (int i = 0; i < src.size(); i += step) {
            out.add(src.get(i));
        }
        if (!out.get(out.size() - 1).equals(src.get(src.size() - 1))) {
            out.add(src.get(src.size() - 1));
        }
        return out;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ===== 옵션 클래스 =====
    public static class MapOptions {
        public int width = DEFAULT_WIDTH;
        public int height = DEFAULT_HEIGHT;
        public int scale = DEFAULT_SCALE;
        public String mapType = "roadmap";  // roadmap|satellite|terrain|hybrid

        public String pathColor = "0xff0000ff"; // ARGB
        public int pathWeight = 5;

        public boolean showStartMarker = true;
        public boolean showEndMarker = true;
        public String startMarkerColor = "green";
        public String endMarkerColor = "red";
        public String startMarkerLabel = "S";
        public String endMarkerLabel = "E";

        public List<ExtraMarker> extraMarkers = List.of();

        public static MapOptions defaults() { return new MapOptions(); }
    }

    public static class ExtraMarker {
        public final double lat;
        public final double lng;
        public final String color;
        public final String label;

        public ExtraMarker(double lat, double lng, String color, String label) {
            this.lat = lat;
            this.lng = lng;
            this.color = color;
            this.label = label;
        }
    }
}
