package com.TripRider.TripRider.dto.custom;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PickReq {
    private String contentId;
    private String type;           // tour|food|stay|shop|event|leports
    private String title;
    private double lat;
    private double lng;
    private String cat1, cat2, cat3;
    private Integer contentTypeId;
    private String addr;
    private String image;
}
