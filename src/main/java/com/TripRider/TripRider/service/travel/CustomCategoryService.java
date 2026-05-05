package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.custom.CategoryTreeDto;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomCategoryService {

    @Value("${tourapi.base-url}")   private String baseUrl;
    @Value("${tourapi.service-key}") private String serviceKey;
    @Value("${tourapi.mobile-os:ETC}") private String mobileOs;
    @Value("${tourapi.mobile-app:TripRider}") private String mobileApp;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    //  contentTypeId → 기본 cat1 코드들 (fallback)
    private static final Map<Integer, List<CategoryTreeDto.Node>> DEFAULT_CAT1 = Map.of(
            39, List.of(node("A05","음식점")),
            14, List.of(node("A02","인문/문화/역사")),
            15, List.of(node("A02","인문/문화/역사")),  // 행사/공연/축제도 A02 트리 사용
            28, List.of(node("A03","레포츠")),
            32, List.of(node("B02","숙박")),          // 숙박은 B계열
            38, List.of(node("A04","쇼핑")),
            12, List.of(node("A01","자연"), node("A02","인문/문화/역사"), node("A03","레포츠"), node("A04","쇼핑"))
    );

    private static CategoryTreeDto.Node node(String code, String name){
        return CategoryTreeDto.Node.builder().code(code).name(name).build();
    }

    public CategoryTreeDto getTreeForType(int contentTypeId){
        // 1) cat1 시도
        List<CategoryTreeDto.Node> cat1 = fetch(null, null, contentTypeId);

        // 2) 비었으면 기본 cat1로 대체
        if (cat1 == null || cat1.isEmpty()) {
            cat1 = new ArrayList<>(DEFAULT_CAT1.getOrDefault(contentTypeId, List.of()));
        }

        // 3) 하위 cat2, cat3 채우기
        for (CategoryTreeDto.Node c1 : cat1){
            List<CategoryTreeDto.Node> cat2 = fetch(c1.getCode(), null, contentTypeId);
            c1.setCat2(cat2);
            if (cat2 != null) {
                for (CategoryTreeDto.Node c2 : cat2){
                    List<CategoryTreeDto.Node> cat3 = fetch(c1.getCode(), c2.getCode(), contentTypeId);
                    c2.setCat3(cat3);
                }
            }
        }
        return CategoryTreeDto.builder().contentTypeId(contentTypeId).cat1(cat1).build();
    }

    private List<CategoryTreeDto.Node> fetch(String cat1, String cat2, int ctype){
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/categoryCode2")
                .queryParam("serviceKey", serviceKey == null ? "" : serviceKey.trim())
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("contentTypeId", ctype)
                .queryParam("numOfRows", 1000);   //  누락 방지

        if (cat1 != null && !cat1.isBlank()) b.queryParam("cat1", cat1);
        if (cat2 != null && !cat2.isBlank()) b.queryParam("cat2", cat2);

        URI uri = b.build().toUri();
        String raw = rest.getForObject(uri, String.class);

        try{
            JsonNode items = mapper.readTree(raw).path("response").path("body").path("items").path("item");
            List<CategoryTreeDto.Node> out = new ArrayList<>();
            if (items.isArray()){
                for (JsonNode n: items){
                    out.add(CategoryTreeDto.Node.builder()
                            .code(n.path("code").asText(""))
                            .name(n.path("name").asText(""))
                            .build());
                }
            }
            return out;
        }catch(Exception e){
            return List.of();
        }
    }
}