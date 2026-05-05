package com.TripRider.TripRider.dto.mypage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyPageUpdateRequest {
    private String nickname;
    private String badge;
    private String intro;
}
