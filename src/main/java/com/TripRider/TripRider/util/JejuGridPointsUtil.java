package com.TripRider.TripRider.util;

import com.TripRider.TripRider.dto.riding.RidingCourseDetailDto;
import java.util.*;

public class JejuGridPointsUtil {
    // 제주 전역을 고르게 커버하는 12개 샘플 포인트(대략값)
    public static List<RidingCourseDetailDto.LatLng> grid12() {
        double[][] pts = {
                {33.56,126.71},{33.55,126.54},{33.55,126.37},
                {33.49,126.74},{33.48,126.56},{33.48,126.38},
                {33.43,126.76},{33.42,126.58},{33.42,126.40},
                {33.36,126.74},{33.36,126.56},{33.36,126.38}
        };
        List<RidingCourseDetailDto.LatLng> list=new ArrayList<>();
        for (double[] p:pts) list.add(new RidingCourseDetailDto.LatLng(p[0],p[1]));
        return list;
    }
}
