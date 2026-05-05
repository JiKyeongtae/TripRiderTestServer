package com.TripRider.TripRider.dto.board;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequest {
    private String content;
    private String imageUrl;
    private String location;
    private String hashtags;
}
