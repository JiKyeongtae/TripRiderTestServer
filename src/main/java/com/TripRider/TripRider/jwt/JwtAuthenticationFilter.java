package com.TripRider.TripRider.jwt;

import com.TripRider.TripRider.config.UserPrincipal;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.repository.user.UserRepository;
import com.TripRider.TripRider.service.mypage.LogoutService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends GenericFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final LogoutService logoutService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) request;
        String token = resolveToken(http);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 블랙리스트 확인
            if (logoutService.isBlacklisted(token)) {
                ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String email = jwtTokenProvider.getEmail(token);
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                // ★ principal을 User → UserPrincipal 로 축소
                UserPrincipal principal = new UserPrincipal(
                        user.getId(), user.getEmail(), user.getNickname()
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, user.getAuthorities()
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}
