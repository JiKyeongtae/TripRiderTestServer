package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.common.NearbyPlaceDto;
import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {

    @Value("${tourapi.base-url}")  private String baseUrl;
    @Value("${tourapi.service-key}") private String serviceKey; // URL-encoded
    @Value("${tourapi.mobile-os:ETC}") private String mobileOs;
    @Value("${tourapi.mobile-app:TripRider}") private String mobileApp;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();

    /** 위치기반 목록(한 지점) */
    public List<NearbyPlaceDto> locationBasedList(
            double lat, double lng, int radiusMeters, int contentTypeId, int size, int page) {

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/locationBasedList2")
                .queryParam("serviceKey", serviceKey == null ? "" : serviceKey.trim()) // 원문키 그대로 넣고
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("mapX", String.valueOf(lng))
                .queryParam("mapY", String.valueOf(lat))
                .queryParam("radius", radiusMeters)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("arrange", "E")
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .build()   // <<< 여기! true 제거 (UriComponentsBuilder가 자동 인코딩)
                .toUri();



        String raw = rest.getForObject(uri, String.class);
        try {
            JsonNode items = mapper.readTree(raw)
                    .path("response").path("body").path("items").path("item");
            List<NearbyPlaceDto> list = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode n : items) list.add(toDto(n));
            }
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 지역기반 목록(제주 전역/시군구) */
    public List<NearbyPlaceDto> areaBasedList(
            int areaCode,                   // 예: 39 = 제주
            Integer sigunguCode,            // null 이면 도 전역
            int contentTypeId,              // 12/14/15/28/32/38/39
            String cat1, String cat2, String cat3,
            int size, int page, String arrange // A,B,C,D,E
    ) {

        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/areaBasedList2")
                .queryParam("serviceKey", serviceKey == null ? "" : serviceKey.trim())
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("areaCode", areaCode)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page);

        if (sigunguCode != null) b.queryParam("sigunguCode", sigunguCode);
        if (cat1 != null && !cat1.isBlank()) b.queryParam("cat1", cat1);
        if (cat2 != null && !cat2.isBlank()) b.queryParam("cat2", cat2);
        if (cat3 != null && !cat3.isBlank()) b.queryParam("cat3", cat3);
        if (arrange != null && !arrange.isBlank()) b.queryParam("arrange", arrange);

        URI uri = b.build().toUri();
        String raw = rest.getForObject(uri, String.class);

        try {
            JsonNode items = mapper.readTree(raw)
                    .path("response").path("body").path("items").path("item");
            List<NearbyPlaceDto> list = new ArrayList<>();
            if (items.isArray()) for (JsonNode n : items) list.add(toDto(n));
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private NearbyPlaceDto toDto(JsonNode n) {
        Long contentId = n.has("contentid") ? n.get("contentid").asLong() : null;
        String title = n.path("title").asText("");
        String addr = n.path("addr1").asText("");
        String tel = n.path("tel").asText("");
        String image = n.path("firstimage").asText(null);
        double mapx = n.path("mapx").asDouble(); // 경도
        double mapy = n.path("mapy").asDouble(); // 위도
        Integer dist = n.has("dist") ? safeInt(n.get("dist").asText(null)) : null;
        Integer ctype = n.has("contenttypeid") ? n.get("contenttypeid").asInt() : null;

        // 카테고리 코드
        String cat1 = n.has("cat1") ? n.get("cat1").asText(null) : null;
        String cat2 = n.has("cat2") ? n.get("cat2").asText(null) : null;
        String cat3 = n.has("cat3") ? n.get("cat3").asText(null) : null;

        return NearbyPlaceDto.builder()
                .contentId(contentId != null && contentId == 0 ? null : contentId)
                .title(title).addr(addr).tel(tel).image(image)
                .lng(mapx).lat(mapy).distMeters(dist).contentTypeId(ctype)
                .cat1(cat1).cat2(cat2).cat3(cat3)
                .build();
    }

    private Integer safeInt(String s) {
        try { return s == null ? null : Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    /** 여러 지점 결과를 합쳐서(중복 제거 + 거리 오름차순) 반환 */
    public List<NearbyPlaceDto> mergedByPoints(
            List<RidingCourseDetailDto.LatLng> points,
            int radiusMeters, int contentTypeId,
            int sizePerPoint, int maxTotal) {

        Map<String, NearbyPlaceDto> dedup = new LinkedHashMap<>();
        for (var p : points) {
            var list = locationBasedList(p.getLat(), p.getLng(), radiusMeters, contentTypeId, sizePerPoint, 1);
            for (var e : list) {
                String key;
                if (e.getContentId() != null) key = "id:" + e.getContentId();
                else key = "t:" + (e.getTitle()==null?"":e.getTitle())
                        + "|" + Math.round(e.getLat()*1e4) + "|" + Math.round(e.getLng()*1e4); // ~10m
                if (!dedup.containsKey(key) ||
                        (e.getDistMeters()!=null &&
                                (dedup.get(key).getDistMeters()==null ||
                                        e.getDistMeters() < dedup.get(key).getDistMeters()))) {
                    dedup.put(key, e);
                }
            }
        }
        List<NearbyPlaceDto> merged = new ArrayList<>(dedup.values());
        merged.sort(Comparator.comparing(x -> x.getDistMeters()==null ? Integer.MAX_VALUE : x.getDistMeters()));
        if (maxTotal > 0 && merged.size() > maxTotal) return merged.subList(0, maxTotal);
        return merged;
    }

    public record FoodDetail(String firstmenu, String treatmenu, String overview) {}

    // 인코딩 상태(serviceKey가 이미 % 포함인지)에 따라 build(true) vs encode 통일
    private URI buildUri(UriComponentsBuilder b) {
        boolean encoded = serviceKey != null && serviceKey.contains("%");
        return encoded ? b.build(true).toUri() : b.encode(StandardCharsets.UTF_8).build().toUri();
    }

    public FoodDetail fetchFoodDetail(Long contentId) {
        if (contentId == null) return new FoodDetail(null, null, null);
        try {
            // detailIntro2 (firstmenu, treatmenu)
            UriComponentsBuilder b1 = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/detailIntro2")
                    .queryParam("serviceKey", serviceKey == null ? "" : serviceKey.trim())
                    .queryParam("MobileOS", mobileOs)
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("contentId", contentId)
                    .queryParam("contentTypeId", 39)
                    .queryParam("listYN", "Y")
                    .queryParam("numOfRows", 10)
                    .queryParam("pageNo", 1);
            URI introUri = buildUri(b1);
            log.info("CALL detailIntro2: {}", introUri);

            String introRaw = rest.getForObject(introUri, String.class);
            JsonNode introItem = mapper.readTree(introRaw)
                    .path("response").path("body").path("items").path("item");
            if (introItem.isArray() && introItem.size() > 0) introItem = introItem.get(0);

            String firstmenu = introItem.path("firstmenu").asText(null);
            String treatmenu = introItem.path("treatmenu").asText(null);

            // detailCommon2 (overview)
            UriComponentsBuilder b2 = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/detailCommon2")
                    .queryParam("serviceKey", serviceKey == null ? "" : serviceKey.trim())
                    .queryParam("MobileOS", mobileOs)
                    .queryParam("MobileApp", mobileApp)
                    .queryParam("_type", "json")
                    .queryParam("contentId", contentId)
                    .queryParam("overviewYN", "Y")
                    .queryParam("defaultYN", "N")
                    .queryParam("addrinfoYN", "N")
                    .queryParam("firstImageYN", "N");
            URI commonUri = buildUri(b2);
            log.info("CALL detailCommon2: {}", commonUri);

            String commonRaw = rest.getForObject(commonUri, String.class);
            JsonNode commonItem = mapper.readTree(commonRaw)
                    .path("response").path("body").path("items").path("item");
            if (commonItem.isArray() && commonItem.size() > 0) commonItem = commonItem.get(0);

            String overview = commonItem.path("overview").asText(null);

            return new FoodDetail(firstmenu, treatmenu, overview);
        } catch (RestClientResponseException e) {
            log.warn("fetchFoodDetail error status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return new FoodDetail(null, null, null);
        } catch (Exception e) {
            log.warn("fetchFoodDetail error", e);
            return new FoodDetail(null, null, null);
        }
    }

    public List<NearbyPlaceDto> searchKeyword(
            String keyword, Integer areaCode, Integer sigunguCode,
            int contentTypeId,
            String cat1, String cat2, String cat3,   //  추가
            int size, int page, String arrange) {

        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/searchKeyword2")
                .queryParam("serviceKey", serviceKey == null ? "" : serviceKey.trim())
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("keyword", keyword)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("listYN", "Y")
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page);

        if (areaCode != null) b.queryParam("areaCode", areaCode);
        if (sigunguCode != null) b.queryParam("sigunguCode", sigunguCode);
        if (cat1 != null && !cat1.isBlank()) b.queryParam("cat1", cat1);
        if (cat2 != null && !cat2.isBlank()) b.queryParam("cat2", cat2);
        if (cat3 != null && !cat3.isBlank()) b.queryParam("cat3", cat3);
        if (arrange != null && !arrange.isBlank()) b.queryParam("arrange", arrange);

        URI uri = b.build()/* 또는 build(true): 키 인코딩 정책에 따라 */.toUri();
        log.info("CALL searchKeyword2: {}", uri);

        String raw;
        try {
            raw = rest.getForObject(uri, String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.warn("TourAPI error status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("TourAPI error", e);
            return List.of();
        }

        try {
            JsonNode items = mapper.readTree(raw)
                    .path("response").path("body").path("items").path("item");
            List<NearbyPlaceDto> list = new ArrayList<>();
            if (items.isArray()) for (JsonNode n : items) list.add(toDto(n));
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }



}
