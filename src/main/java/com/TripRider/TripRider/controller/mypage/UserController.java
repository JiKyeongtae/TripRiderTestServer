package com.TripRider.TripRider.controller.mypage;

import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.mypage.NicknameUpdateRequest;
import com.TripRider.TripRider.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    // 닉네임으로 사용자 ID 조회
    @GetMapping("/by-nickname")
    public ResponseEntity<?> getUserIdByNickname(@RequestParam String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(Map.of("userId", user.getId()));
    }


    // 닉네임 사용 가능 여부 체크 (공개)
    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) {
        String validated = normalize(nickname);
        if (!isValidNickname(validated)) {
            return ResponseEntity.badRequest().body(Map.of("available", false, "reason", "형식오류"));
        }
        boolean exists = userRepository.existsByNickname(validated);
        return ResponseEntity.ok(Map.of("available", !exists));
    }

    // 내 닉네임 저장/수정 (JWT 필요)
    @PatchMapping("/me/nickname")
    public ResponseEntity<?> updateMyNickname(
            @RequestBody NicknameUpdateRequest req,
            @AuthenticationPrincipal(expression = "id") Long userId   // ★ User 대신 id만
    ) {
        if (userId == null) {
            // 토큰 없음/무효 → 401 (이전엔 NPE로 500)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String target = normalize(req.getNickname());
        if (!isValidNickname(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "닉네임 형식이 올바르지 않습니다."));
        }

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user"));

        // 내 닉네임과 동일하면 통과, 다르면 중복 체크
        if (!target.equals(me.getNickname()) && userRepository.existsByNickname(target)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "이미 사용중인 닉네임"));
        }

        me.setNickname(target);
        userRepository.save(me);
        return ResponseEntity.ok(Map.of("message", "OK", "nickname", target));
    }

    // ===== 유틸 =====
    private String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }
    // 2~16자, 한/영/숫자/밑줄 허용
    private boolean isValidNickname(String s) {
        return s != null && s.matches("^[A-Za-z0-9가-힣_]{2,16}$");
    }
}
