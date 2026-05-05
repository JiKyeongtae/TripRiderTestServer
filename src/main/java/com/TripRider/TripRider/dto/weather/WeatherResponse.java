package com.TripRider.TripRider.dto.weather;

import lombok.Data;
import java.util.List;

@Data
public class WeatherResponse {
    private Response response;

    @Data
    public static class Response {
        private Body body;

        @Data
        public static class Body {
            private Items items;

            @Data
            public static class Items {
                private List<Item> item;

                @Data
                public static class Item {
                    private String category;
                    private String fcstValue;
                    private String fcstTime;
                    private String baseDate;
                    private String baseTime;
                    private String nx;
                    private String ny;
                }
            }
        }
    }
}
