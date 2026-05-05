package com.TripRider.TripRider.dto.custom;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SelectionDtos {
    private String selectionId;
    private int count;
    private List<PickedItem> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PickedItem {
        private String contentId;
        private String type; // tour, food, stay, shop, event, leports
        private String title;
        private double lat;
        private double lng;
        private String cat1, cat2, cat3;
        private Integer contentTypeId;
        private String addr;
        private String image;
    }
}
