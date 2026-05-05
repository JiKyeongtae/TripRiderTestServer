package com.TripRider.TripRider.service.ride;

import org.springframework.web.multipart.MultipartFile;

public interface RouteImageStorage {
    String store(Long rideId, MultipartFile file);
}
