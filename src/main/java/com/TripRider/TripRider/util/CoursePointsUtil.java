package com.TripRider.TripRider.util;

import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;

import java.util.*;

public final class CoursePointsUtil {
    private CoursePointsUtil(){}

    /** 경로 길이의 50% 지점을 선 위에서 보간하여 리턴 */
    public static RidingCourseDetailDto.LatLng midpointByPathLength(RidingCourseDetailDto d) {
        var p = d.getPolyline();
        if (p == null || p.isEmpty()) return new RidingCourseDetailDto.LatLng(0,0);
        if (p.size() == 1) return p.get(0);

        double total = 0;
        for (int i = 1; i < p.size(); i++) total += haversine(p.get(i-1), p.get(i));
        double target = total / 2.0;

        double acc = 0;
        for (int i = 1; i < p.size(); i++) {
            double seg = haversine(p.get(i-1), p.get(i));
            if (acc + seg >= target) {
                double remain = target - acc;
                double t = seg == 0 ? 0 : remain / seg;
                double lat = p.get(i-1).getLat() + t * (p.get(i).getLat() - p.get(i-1).getLat());
                double lng = p.get(i-1).getLng() + t * (p.get(i).getLng() - p.get(i-1).getLng());
                return new RidingCourseDetailDto.LatLng(lat, lng);
            }
            acc += seg;
        }
        return p.get(p.size()-1);
    }

    /** 시작·중간(거리기준)·끝 3개 좌표 */
    public static List<RidingCourseDetailDto.LatLng> startMidEndByLength(RidingCourseDetailDto d) {
        var p = d.getPolyline();
        List<RidingCourseDetailDto.LatLng> out = new ArrayList<>();
        if (p == null || p.isEmpty()) return out;
        out.add(p.get(0));
        out.add(midpointByPathLength(d));
        out.add(p.get(p.size()-1));
        return out;
    }

    /** 경로 전체를 K-1 등분하여 K개 지점을 균등 샘플링 */
    public static List<RidingCourseDetailDto.LatLng> sampleByCount(RidingCourseDetailDto d, int k) {
        var p = d.getPolyline();
        List<RidingCourseDetailDto.LatLng> out = new ArrayList<>();
        if (p == null || p.isEmpty() || k <= 0) return out;
        if (k == 1) { out.add(p.get(0)); return out; }

        double[] cum = new double[p.size()];
        for (int i = 1; i < p.size(); i++) cum[i] = cum[i-1] + haversine(p.get(i-1), p.get(i));
        double total = cum[cum.length-1];

        for (int j = 0; j < k; j++) {
            double target = (total * j) / (k - 1); // 0..total
            int idx = Arrays.binarySearch(cum, target);
            if (idx >= 0) { out.add(p.get(idx)); continue; }
            idx = -idx - 1;
            if (idx <= 0) { out.add(p.get(0)); continue; }
            if (idx >= cum.length) { out.add(p.get(p.size()-1)); continue; }

            var a = p.get(idx-1); var b = p.get(idx);
            double seg = cum[idx] - cum[idx-1];
            double t = seg == 0 ? 0 : (target - cum[idx-1]) / seg;
            double lat = a.getLat() + t * (b.getLat() - a.getLat());
            double lng = a.getLng() + t * (b.getLng() - a.getLng());
            out.add(new RidingCourseDetailDto.LatLng(lat, lng));
        }
        return out;
    }

    private static double haversine(RidingCourseDetailDto.LatLng a, RidingCourseDetailDto.LatLng b) {
        double R=6371000, dLat=Math.toRadians(b.getLat()-a.getLat()), dLng=Math.toRadians(b.getLng()-a.getLng());
        double x=Math.sin(dLat/2), y=Math.sin(dLng/2);
        double h=x*x + Math.cos(Math.toRadians(a.getLat()))*Math.cos(Math.toRadians(b.getLat()))*y*y;
        return 2*R*Math.atan2(Math.sqrt(h), Math.sqrt(1-h));
    }
}
