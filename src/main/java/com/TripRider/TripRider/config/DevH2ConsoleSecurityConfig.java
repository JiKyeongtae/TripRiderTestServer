package com.TripRider.TripRider.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class DevH2ConsoleSecurityConfig {

    /**
     * dev 에서만 H2 콘솔 오픈 + frameOptions sameOrigin 허용
     */
    @Bean
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
