package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.riding.RidingCourseCardDto;
import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.lang.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourseFileService {

    @Value("${course.path}")
    private String coursePath;

    private final ObjectMapper mapper = new ObjectMapper();

    // (category,id) 조합으로 구분 (예: "coastal-course#1")
    private final Map<String, RidingCourseDetailDto> store = new ConcurrentHashMap<>();
    private String keyOf(String category, long id) { return category + "#" + id; }



    @PostConstruct
    public void loadAll() throws Exception {
        if (coursePath.startsWith("classpath:")) {
            // base 정규화: "classpath:/course" 또는 "classpath:course" 모두 허용
            String base = coursePath.replaceFirst("^classpath:/*", ""); // 앞의 "classpath:"와 "/"들 제거
            if (base.isBlank()) base = "course";

            // 존재하는 파일만 패턴 스캔 (하드코딩된 1..1000 제거)
            // 구조: course/<category>-course/<id>/*.json
            ResourcePatternResolver rpr = new PathMatchingResourcePatternResolver();
            Resource[] files = rpr.getResources("classpath*:" + trimRight(base, "/") + "/*-course/*/*.json");

            if (files.length == 0) {
                System.err.println("[WARN] No course files found under classpath:" + base);
                return;
            }

            Pattern pathPattern = Pattern.compile(".*/" + Pattern.quote(trimRight(base, "/")) + "/([a-zA-Z0-9_-]+-course)/(\\w+)/[^/]+\\.json$");

            for (Resource file : files) {
                String pathStr = safeResourcePath(file);
                Matcher m = pathPattern.matcher(pathStr);
                if (!m.find()) {
                    System.err.println("[WARN] Unrecognized course path: " + pathStr);
                    continue;
                }
                String category = m.group(1);     // ex) coastal-course, inland-course, udo-course
                long id = parseLongSafe(m.group(2));

                if (id < 0) {
                    System.err.println("[WARN] Invalid course id in path: " + pathStr);
                    continue;
                }

                String json = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                // 같은 (category,id)에 여러 json이 있을 경우 첫 것만 사용 (원하면 merge 로직 추가)
                store.putIfAbsent(keyOf(category, id), parse(category, id, json));
            }

            System.out.println("[INFO] Loaded course files: " + store.size());

        } else {
            // 외부 디렉토리(배포용)
            Files.walk(Path.of(coursePath))
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadOneSafe);
        }
    }

    /** Resource의 경로를 문자열로 안전하게 얻기 (JAR/파일시스템 호환) */
    private String safeResourcePath(Resource r) {
        try {
            // getURL/getURI 모두 가능하지만 문자열 매칭만 필요하므로 둘 중 하나만 일관 사용
            return r.getURL().toString().replace('\\', '/');
        } catch (Exception e) {
            try {
                return r.getURI().toString().replace('\\', '/');
            } catch (Exception ex) {
                return r.getDescription();
            }
        }
    }

    private long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return -1L;
        }
    }

    private String trimRight(String s, String chs) {
        String t = s;
        while (t.endsWith(chs)) t = t.substring(0, t.length() - chs.length());
        return t;
    }

    // ====== 아래는 기존 코드 그대로 ======

    private void loadOneSafe(Path p) {
        try {
            String json = Files.readString(p);
            String idStr = p.getParent().getFileName().toString();               // .../<id>/<file>.json
            String category = p.getParent().getParent().getFileName().toString(); // .../<cat>/<id>/
            long id = Long.parseLong(idStr);
            store.put(keyOf(category, id), parse(category, id, json));
        } catch (Exception ignored) { }
    }


    private RidingCourseDetailDto parse(String category, long id, String json) throws Exception {
        JsonNode root   = mapper.readTree(json);
        JsonNode route0 = root.path("routes").get(0);

        // 좌표 (GeoJSON: [lng,lat] -> DTO: lat,lng)
        JsonNode coords = route0.path("geometry").path("coordinates");
        List<RidingCourseDetailDto.LatLng> poly = new ArrayList<>();
        for (JsonNode c : coords) {
            poly.add(new RidingCourseDetailDto.LatLng(
                    c.get(1).asDouble(),
                    c.get(0).asDouble()
            ));
        }

        // 거리(km) -> m
        double km = route0.path("summary").path("distance").asDouble(0.0);
        int totalMeters = (int) Math.round(km * 1000.0);

        // places로 자동 타이틀 생성
        String title = "Riding Course";
        JsonNode places = root.path("places");
        if (places.size() >= 2) {
            String from = places.get(0).path("placeName").asText("");
            String to   = places.get(1).path("placeName").asText("");
            if (!from.isBlank() && !to.isBlank()) title = from + " → " + to;
        }

        // JSON에 coverImageUrl 키가 있으면 우선 사용, 없으면 classpath에서 자동 탐색
        String coverFromJson = root.path("coverImageUrl").isMissingNode() ? null : root.path("coverImageUrl").asText();
        String cover = (coverFromJson != null && !coverFromJson.isBlank())
                ? coverFromJson
                : resolveCoverUrl(category, id);

        return RidingCourseDetailDto.builder()
                .id(id)
                .category(category)
                .title(title)
                .description("")
                .coverImageUrl(cover)
                .totalDistanceMeters(totalMeters)
                .polyline(poly)
                .build();
    }

    //** classpath:static/images/course/<category>/<id>.(png|jpg|jpeg|webp) 를 찾아 URL 리턴 */
    private String resolveCoverUrl(String category, long id) {
        String[] roots = {
                "classpath:/static.images.course/"
        };
        String[] exts = {"png", "jpg", "jpeg", "webp"};
        ResourcePatternResolver rpr = new PathMatchingResourcePatternResolver();

        for (String root : roots) {
            for (String ext : exts) {
                Resource r = rpr.getResource(root + category + "/" + id + "." + ext);
                if (r.exists()) {
                    // URL 은 동일하게 /images/course/** 로 반환
                    return "/images/course/" + category + "/" + id + "." + ext;
                }
            }
        }
        return null;
    }

    public List<RidingCourseCardDto> listCards(@Nullable Double myLat, @Nullable Double myLng) {
        return store.values().stream().map(d -> {
            var first = d.getPolyline().isEmpty()
                    ? new RidingCourseDetailDto.LatLng(0, 0)
                    : d.getPolyline().get(0);
            Double dist = (myLat != null && myLng != null)
                    ? haversine(first.getLat(), first.getLng(), myLat, myLng)
                    : null;

            return RidingCourseCardDto.builder()
                    .id(d.getId())
                    .category(d.getCategory())
                    .title(d.getTitle())
                    .coverImageUrl(d.getCoverImageUrl())
                    .totalDistanceMeters(d.getTotalDistanceMeters())
                    .startLat(first.getLat())
                    .startLng(first.getLng())
                    .distanceMetersFromMe(dist)
                    .build();
        }).toList();
    }

    public RidingCourseDetailDto get(String category, Long id) {
        var d = store.get(keyOf(category, id));
        if (d == null) throw new IllegalArgumentException("코스 없음");
        return d;
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
