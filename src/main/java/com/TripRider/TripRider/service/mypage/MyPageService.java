
package com.TripRider.TripRider.service.mypage;

import com.TripRider.TripRider.domain.user.Badge;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.domain.user.UserBadge;
import com.TripRider.TripRider.repository.user.BadgeRepository;
import com.TripRider.TripRider.repository.user.UserBadgeRepository;
import com.TripRider.TripRider.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    //  프로필 이미지 업데이트
    public String updateProfileImage(User user, MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            try {
                String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                String uploadDir = System.getProperty("user.home") + "/triprider-uploads";

                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String fullPath = uploadDir + "/" + filename;
                image.transferTo(new File(fullPath));

                String imagePath = "/uploads/" + filename;
                user.updateProfileImage(imagePath);
                userRepository.save(user);

                return imagePath;
            } catch (IOException e) {
                throw new RuntimeException("이미지 업로드 실패", e);
            }
        } else {
            throw new IllegalArgumentException("이미지가 비어 있습니다.");
        }
    }

    //  닉네임, 한줄소개, 뱃지 업데이트
    public void updateProfile(String nickname, String intro, String badge, User user) {
        user.setNickname(nickname);
        user.setIntro(intro);
        user.setBadge(badge);
        userRepository.save(user);
    }

    //  한줄소개만 따로 업데이트
    public void updateIntro(String intro, User user) {
        user.setIntro(intro);
        userRepository.save(user);
    }

    //  대표 뱃지 선택
    @Transactional
    public void updateRepresentativeBadge(User user, String badgeName) {
        user.setRepresentativeBadge(badgeName);
        userRepository.save(user);
    }

    //  내가 가진 뱃지 조회
    @Transactional(readOnly = true)
    public List<Badge> getUserBadges(User user) {
        return userBadgeRepository.findByUser(user).stream()
                .map(UserBadge::getBadge)
                .toList();
    }

    //  거리 기반 자동 뱃지 지급
    @Transactional
    public void checkAndGiveDistanceBadge(User user) {
        int distance = user.getTotalDistance();

        Map<Integer, String> badgeRules = Map.of(
                100, "100km 달성",
                200, "200km 달성",
                500, "500km 달성",
                1000, "1000km 달성"
        );

        for (var entry : badgeRules.entrySet()) {
            if (distance >= entry.getKey()) {
                String badgeName = entry.getValue();
                Badge badge = badgeRepository.findByName(badgeName)
                        .orElseGet(() -> badgeRepository.save(
                                Badge.builder()
                                        .name(badgeName)
                                        .description(entry.getKey() + " 이상 달성한 라이더")
                                        .iconUrl("/images/badges/" + entry.getKey() + ".png")
                                        .build()
                        ));

                if (!userBadgeRepository.existsByUserAndBadge(user, badge)) {
                    userBadgeRepository.save(UserBadge.builder()
                            .user(user)
                            .badge(badge)
                            .build());
                }
            }
        }
    }
}
