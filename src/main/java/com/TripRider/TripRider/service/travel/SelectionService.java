package com.TripRider.TripRider.service.travel;

import com.TripRider.TripRider.dto.custom.PickReq;
import com.TripRider.TripRider.dto.custom.SelectionDtos;
import com.TripRider.TripRider.dto.custom.SelectionDtos.PickedItem;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SelectionService {
    // 데모용 인메모리 저장 (운영에서는 Redis 권장)
    private final Map<String, List<PickedItem>> store = new ConcurrentHashMap<>();

    public String create() {
        String id = "sel_" + UUID.randomUUID().toString().replace("-","");
        store.put(id, new ArrayList<>());
        return id;
    }

    public void add(String sid, PickReq req) {
        store.computeIfAbsent(sid, k -> new ArrayList<>());
        store.get(sid).removeIf(p -> p.getContentId().equals(req.getContentId()));
        store.get(sid).add(toPicked(req));
    }

    public void remove(String sid, String contentId) {
        store.computeIfPresent(sid, (k, v) -> { v.removeIf(p -> p.getContentId().equals(contentId)); return v; });
    }

    public SelectionDtos view(String sid) {
        var list = store.getOrDefault(sid, List.of());
        return SelectionDtos.builder()
                .selectionId(sid)
                .count(list.size())
                .items(list)
                .build();
    }

    public List<PickedItem> getAll(String sid){ return new ArrayList<>(store.getOrDefault(sid, List.of())); }

    private PickedItem toPicked(PickReq r){
        return PickedItem.builder()
                .contentId(r.getContentId()).type(r.getType())
                .title(r.getTitle()).lat(r.getLat()).lng(r.getLng())
                .cat1(r.getCat1()).cat2(r.getCat2()).cat3(r.getCat3())
                .contentTypeId(r.getContentTypeId()).addr(r.getAddr()).image(r.getImage())
                .build();
    }
}
