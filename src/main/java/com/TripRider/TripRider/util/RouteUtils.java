package com.TripRider.TripRider.util;

import com.TripRider.TripRider.dto.custom.CoursePreview.Waypoint;

import java.util.*;

public class RouteUtils {

    public static List<Waypoint> nearestNeighbor(List<Waypoint> src) {
        if (src == null || src.isEmpty()) return src;
        List<Waypoint> rest = new ArrayList<>(src);
        List<Waypoint> path = new ArrayList<>();
        Waypoint cur = rest.remove(0);
        path.add(cur);
        while (!rest.isEmpty()) {
            final Waypoint current = cur; // 람다에서 사용할 고정 참조
            Waypoint next = rest.stream()
                    .min(Comparator.comparingDouble(w -> dist(current, w)))
                    .orElse(rest.get(0));
            rest.remove(next);
            path.add(next);
            cur = next;
        }

        return path;
    }

    public static List<Waypoint> twoOpt(List<Waypoint> path) {
        int n = path.size();
        if (n < 4) return path;
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < n - 2; i++) {
                for (int k = i + 1; k < n - 1; k++) {
                    double d0 = dist(path.get(i - 1), path.get(i)) + dist(path.get(k), path.get(k + 1));
                    double d1 = dist(path.get(i - 1), path.get(k)) + dist(path.get(i), path.get(k + 1));
                    if (d1 + 1e-6 < d0) {
                        reverse(path, i, k);
                        improved = true;
                    }
                }
            }
        }
        return path;
    }

    public static double totalKm(List<Waypoint> path) {
        double s = 0;
        for (int i = 0; i < path.size() - 1; i++) s += dist(path.get(i), path.get(i + 1));
        return s;
    }

    private static double dist(Waypoint a, Waypoint b) {
        return GeoUtil.haversineKm(a.getLat(), a.getLng(), b.getLat(), b.getLng());
    }

    private static void reverse(List<Waypoint> path, int i, int k) {
        while (i < k) { Collections.swap(path, i++, k--); }
    }
}
