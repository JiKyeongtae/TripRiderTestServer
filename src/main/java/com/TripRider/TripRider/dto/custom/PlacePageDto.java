package com.TripRider.TripRider.dto.custom;

import com.TripRider.TripRider.dto.common.NearbyPlaceDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacePageDto {
    private List<NearbyPlaceDto> items;
    private int page;
    private int total;     // 알 수 없으면 -1
}
