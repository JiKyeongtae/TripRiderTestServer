package com.TripRider.TripRider.service.mypage;

import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.auth.AddUserRequest;
import com.TripRider.TripRider.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public boolean save(AddUserRequest req) {
        String email = (req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase());
        String rawPw = (req.getPassword() == null ? "" : req.getPassword());
        String nick  = (req.getNickname() == null || req.getNickname().isBlank())
                ? "익명" : req.getNickname().trim();

        // 선(先)중복 검사
        if (userRepository.existsByEmail(email)) {
            return false; // 중복
        }

        try {
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPw))
                    .nickname(nick)
                    .build();
            userRepository.save(user);
            return true;
        } catch (DataIntegrityViolationException e) {
            // 동시요청 등으로 DB 유니크 제약 위반 시에도 중복으로 처리
            return false;
        }
    }
}