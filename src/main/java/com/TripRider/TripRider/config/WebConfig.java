package com.TripRider.TripRider.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String home = System.getProperty("user.home");
        String project = System.getProperty("user.dir");

        // 물리 경로들 (프로젝트 내부 + 홈 디렉터리 2종)
        String projectUploads = "file:" + project + "/uploads/";
        String homeCurrent    = "file:" + home + "/TripRider/uploads/";
        String homeLegacy     = "file:" + home + "/triprider-uploads/";

        // 없으면 생성
        new File(project + "/uploads/").mkdirs();
        new File(home + "/TripRider/uploads/").mkdirs();
        new File(home + "/triprider-uploads/").mkdirs();

        // /uploads/** 로 세 위치 모두 서빙
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(projectUploads, homeCurrent, homeLegacy)
                .setCachePeriod(0); // 개발 중 캐시 비활성화
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS");
    }
}
