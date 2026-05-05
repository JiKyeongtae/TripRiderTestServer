package com.TripRider.TripRider.util;

public class GeoUtil {
    // 제주도 대략 BBOX (간단 필터)
    public static boolean isInJeju(double lat, double lng) {
        return (lat >= 33.10 && lat <= 33.60) && (lng >= 126.10 && lng <= 126.99);
    }

    // 하버사인 거리(km)
    public static double haversineKm(double lat1,double lon1,double lat2,double lon2){
        double R=6371.0088;
        double dLat=Math.toRadians(lat2-lat1), dLon=Math.toRadians(lon2-lon1);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.asin(Math.sqrt(a));
    }

    // 하버사인 거리(m)
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        return haversineKm(lat1, lon1, lat2, lon2) * 1000;
    }

    // GPS 튀는 값 억제 (속도가 비현실적이면 0 처리)
    public static double safeSegmentMeters(
            double lat1, double lon1, long t1,
            double lat2, double lon2, long t2
    ) {
        double meters = haversineMeters(lat1, lon1, lat2, lon2);
        long dt = Math.max(1, (t2 - t1)); // ms
        double vKmh = (meters / (dt/1000.0)) * 3.6;
        if (vKmh > 200) return 0.0; // 하드 컷
        return meters;
    }
}
