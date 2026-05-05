package com.TripRider.TripRider.controller.travel;

import com.TripRider.TripRider.dto.custom.PickReq;
import com.TripRider.TripRider.dto.custom.SelectionDtos;
import com.TripRider.TripRider.service.travel.SelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/custom/selection")
@RequiredArgsConstructor
public class SelectionController {

    private final SelectionService selectionService;

    @PostMapping("/sessions")
    public Map<String,String> create() {
        return Map.of("selectionId", selectionService.create());
    }

    @PostMapping("/{sid}/picks")
    public void add(@PathVariable String sid, @RequestBody PickReq req) {
        selectionService.add(sid, req);
    }

    @DeleteMapping("/{sid}/picks/{contentId}")
    public void remove(@PathVariable String sid, @PathVariable String contentId) {
        selectionService.remove(sid, contentId);
    }

    @GetMapping("/{sid}")
    public SelectionDtos view(@PathVariable String sid) {
        return selectionService.view(sid);
    }
}