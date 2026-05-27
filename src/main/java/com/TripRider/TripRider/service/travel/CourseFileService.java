package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.riding.RidingCourseCardDto;
import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.lang.Nullable;
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

    // (category,id) 조합으로 구분
    private final Map<String, RidingCourseDetailDto> store =
            new ConcurrentHashMap<>();

    // 최초 1회만 로딩
    private boolean loaded = false;

    private String keyOf(String category, long id) {
        return category + "#" + id;
    }

    // =========================
    // Lazy Loading
    // =========================
    public synchronized void loadAll() throws Exception {

        // 이미 로딩했으면 종료
        if (loaded) {
            return;
        }

        loaded = true;

        if (coursePath.startsWith("classpath:")) {

            String base =
                    coursePath.replaceFirst("^classpath:/*", "");

            if (base.isBlank()) {
                base = "course";
            }

            ResourcePatternResolver rpr =
                    new PathMatchingResourcePatternResolver();

            Resource[] files = rpr.getResources(
                    "classpath*:"
                            + trimRight(base, "/")
                            + "/*-course/*/*.json"
            );

            if (files.length == 0) {

                System.err.println(
                        "[WARN] No course files found under classpath:"
                                + base
                );

                return;
            }

            Pattern pathPattern = Pattern.compile(
                    ".*/"
                            + Pattern.quote(trimRight(base, "/"))
                            + "/([a-zA-Z0-9_-]+-course)/(\\w+)/[^/]+\\.json$"
            );

            for (Resource file : files) {

                String pathStr = safeResourcePath(file);

                Matcher m = pathPattern.matcher(pathStr);

                if (!m.find()) {

                    System.err.println(
                            "[WARN] Unrecognized course path: "
                                    + pathStr
                    );

                    continue;
                }

                String category = m.group(1);

                long id = parseLongSafe(m.group(2));

                if (id < 0) {

                    System.err.println(
                            "[WARN] Invalid course id in path: "
                                    + pathStr
                    );

                    continue;
                }

                String json = new String(
                        file.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                store.putIfAbsent(
                        keyOf(category, id),
                        parse(category, id, json)
                );
            }

            System.out.println(
                    "[INFO] Loaded course files: "
                            + store.size()
            );

        } else {

            Files.walk(Path.of(coursePath))
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadOneSafe);
        }
    }

    private String safeResourcePath(Resource r) {

        try {

            return r.getURL()
                    .toString()
                    .replace('\\', '/');

        } catch (Exception e) {

            try {

                return r.getURI()
                        .toString()
                        .replace('\\', '/');

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

        while (t.endsWith(chs)) {

            t = t.substring(
                    0,
                    t.length() - chs.length()
            );
        }

        return t;
    }

    // =========================
    // 외부 디렉토리 로딩
    // =========================
    private void loadOneSafe(Path p) {

        try {

            String json = Files.readString(p);

            String idStr =
                    p.getParent()
                            .getFileName()
                            .toString();

            String category =
                    p.getParent()
                            .getParent()
                            .getFileName()
                            .toString();

            long id = Long.parseLong(idStr);

            store.put(
                    keyOf(category, id),
                    parse(category, id, json)
            );

        } catch (Exception ignored) {
        }
    }

    private RidingCourseDetailDto parse(
            String category,
            long id,
            String json
    ) throws Exception {

        JsonNode root = mapper.readTree(json);

        JsonNode route0 =
                root.path("routes").get(0);

        JsonNode coords =
                route0.path("geometry")
                        .path("coordinates");

        List<RidingCourseDetailDto.LatLng> poly =
                new ArrayList<>();

        for (JsonNode c : coords) {

            poly.add(
                    new RidingCourseDetailDto.LatLng(
                            c.get(1).asDouble(),
                            c.get(0).asDouble()
                    )
            );
        }

        double km =
                route0.path("summary")
                        .path("distance")
                        .asDouble(0.0);

        int totalMeters =
                (int) Math.round(km * 1000.0);

        String title = "Riding Course";

        JsonNode places = root.path("places");

        if (places.size() >= 2) {

            String from =
                    places.get(0)
                            .path("placeName")
                            .asText("");

            String to =
                    places.get(1)
                            .path("placeName")
                            .asText("");

            if (!from.isBlank() && !to.isBlank()) {

                title = from + " → " + to;
            }
        }

        String coverFromJson =
                root.path("coverImageUrl").isMissingNode()
                        ? null
                        : root.path("coverImageUrl").asText();

        String cover =
                (coverFromJson != null
                        && !coverFromJson.isBlank())
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

    private String resolveCoverUrl(
            String category,
            long id
    ) {

        String[] roots = {
                "classpath:/static.images.course/"
        };

        String[] exts = {
                "png",
                "jpg",
                "jpeg",
                "webp"
        };

        ResourcePatternResolver rpr =
                new PathMatchingResourcePatternResolver();

        for (String root : roots) {

            for (String ext : exts) {

                Resource r =
                        rpr.getResource(
                                root
                                        + category
                                        + "/"
                                        + id
                                        + "."
                                        + ext
                        );

                if (r.exists()) {

                    return "/images/course/"
                            + category
                            + "/"
                            + id
                            + "."
                            + ext;
                }
            }
        }

        return null;
    }

    // =========================
    // 카드 목록 조회
    // =========================
    public List<RidingCourseCardDto> listCards(
            @Nullable Double myLat,
            @Nullable Double myLng
    ) {

        try {
            loadAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return store.values().stream().map(d -> {

            var first =
                    d.getPolyline().isEmpty()
                            ? new RidingCourseDetailDto.LatLng(0, 0)
                            : d.getPolyline().get(0);

            Double dist =
                    (myLat != null && myLng != null)
                            ? haversine(
                            first.getLat(),
                            first.getLng(),
                            myLat,
                            myLng
                    )
                            : null;

            return RidingCourseCardDto.builder()
                    .id(d.getId())
                    .category(d.getCategory())
                    .title(d.getTitle())
                    .coverImageUrl(d.getCoverImageUrl())
                    .totalDistanceMeters(
                            d.getTotalDistanceMeters()
                    )
                    .startLat(first.getLat())
                    .startLng(first.getLng())
                    .distanceMetersFromMe(dist)
                    .build();

        }).toList();
    }

    // =========================
    // 상세 조회
    // =========================
    public RidingCourseDetailDto get(
            String category,
            Long id
    ) {

        try {
            loadAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var d = store.get(keyOf(category, id));

        if (d == null) {
            throw new IllegalArgumentException("코스 없음");
        }

        return d;
    }

    private double haversine(
            double lat1,
            double lng1,
            double lat2,
            double lng2
    ) {

        double R = 6371000;

        double dLat =
                Math.toRadians(lat2 - lat1);

        double dLng =
                Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2)
                        * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2)
                        * Math.sin(dLng / 2);

        return 2 * R
                * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );
    }
}