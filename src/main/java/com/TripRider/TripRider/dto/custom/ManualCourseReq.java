package com.TripRider.TripRider.dto.custom;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ManualCourseReq {
    private String title;
    private List<OrderItem> orderedContent;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderItem {
        private String contentId;
        private String type;
    }
}
