package com.TripRider.TripRider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.TripRider.TripRider.domain") //  여기에만 엔티티 스캔
public class TripRiderApplication {
	public static void main(String[] args) {
		SpringApplication.run(TripRiderApplication.class, args);
	}
}
