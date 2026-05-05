package com.TripRider.TripRider.service.board;

import com.TripRider.TripRider.domain.board.Post;
import com.TripRider.TripRider.domain.board.PostLike;
import com.TripRider.TripRider.domain.user.User;
import com.TripRider.TripRider.dto.board.PostResponse;
import com.TripRider.TripRider.repository.board.CommentRepository;
import com.TripRider.TripRider.repository.board.PostLikeRepository;
import com.TripRider.TripRider.repository.board.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;




    //  게시글 저장
    public void create(String content, String imageUrl, String location, String hashtags, User user) {
        if (user == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Post post = Post.builder()
                .user(user)
                .content(content)
                .imageUrl(imageUrl)
                .location(location)
                .hashtags(hashtags)
                .createdAt(LocalDateTime.now())
                .build();

        postRepository.save(post);
    }

    //  게시글 목록 조회 (작성자 프로필 사진 포함, null-safe)
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts(User currentUser) {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(p -> {
                    User u = p.getUser();
                    String writer = (u != null && u.getNickname() != null && !u.getNickname().isBlank())
                            ? u.getNickname()
                            : "익명";
                    String profileImage = (u != null) ? u.getProfileImage() : null;

                    return PostResponse.builder()
                            .id(p.getId())
                            .content(p.getContent())
                            .imageUrl(p.getImageUrl())
                            .location(p.getLocation())
                            .hashtags(p.getHashtags())
                            .writer(writer)
                            .profileImage(profileImage)
                            .likeCount(p.getLikeCount())
                            .liked(currentUser != null && postLikeRepository.existsByPostAndUser(p, currentUser))
                            .commentCount(commentRepository.countByPost(p))
                            .build();
                })
                .collect(Collectors.toList());
    }

    //  게시글 상세 조회 (null-safe)
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id, User currentUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        User u = post.getUser();
        String writer = (u != null && u.getNickname() != null && !u.getNickname().isBlank())
                ? u.getNickname()
                : "익명";
        String profileImage = (u != null) ? u.getProfileImage() : null;

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .location(post.getLocation())
                .hashtags(post.getHashtags())
                .writer(writer)
                .profileImage(profileImage)
                .likeCount(post.getLikeCount())
                .liked(currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser))
                .commentCount(commentRepository.countByPost(post))
                .build();
    }


    //  추가: 특정 사용자 게시물 목록(현재 로그인 사용자 관점으로 liked 값 계산)
    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUserId(Long targetUserId, User currentUser) {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(targetUserId))
                .map(p -> {
                    var u = p.getUser();
                    String writer = (u != null && u.getNickname() != null && !u.getNickname().isBlank())
                            ? u.getNickname()
                            : "익명";
                    String profileImage = (u != null) ? u.getProfileImage() : null;

                    return PostResponse.builder()
                            .id(p.getId())
                            .content(p.getContent())
                            .imageUrl(p.getImageUrl())
                            .location(p.getLocation())
                            .hashtags(p.getHashtags())
                            .writer(writer)
                            .profileImage(profileImage)
                            .likeCount((int) postLikeRepository.countByPost(p))
                            .liked(currentUser != null && postLikeRepository.existsByPostAndUser(p, currentUser))
                            .commentCount(commentRepository.countByPost(p))
                            .build();
                })
                .toList();
    }


    //  게시글 삭제
    @Transactional
    public void deletePost(Long postId, User requester) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        if (requester == null || post.getUser() == null || !post.getUser().getId().equals(requester.getId())) {
            throw new SecurityException("게시글 삭제 권한이 없습니다.");
        }

        commentRepository.deleteByPost(post);
        postRepository.delete(post);
    }

    //  게시글 좋아요
    @Transactional
    public int likePost(Long postId, User user) {
        if (user == null) throw new IllegalArgumentException("로그인이 필요합니다.");

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        if (postLikeRepository.existsByPostAndUser(post, user)) {
            return post.getLikeCount();
        }

        try {
            PostLike like = PostLike.builder()
                    .post(post)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            postLikeRepository.save(like);
            post.increaseLikeCount();
            postRepository.save(post);
        } catch (DataIntegrityViolationException e) {
            // 동시성 충돌 시 무시 후 다시 카운트
        }

        int count = (int) postLikeRepository.countByPost(post);
        post.setLikeCount(count);
        return count;
    }

    //  게시글 좋아요 취소
    @Transactional
    public int unlikePost(Long postId, User user) {
        if (user == null) throw new IllegalArgumentException("로그인이 필요합니다.");

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        if (!postLikeRepository.existsByPostAndUser(post, user)) {
            return post.getLikeCount();
        }

        postLikeRepository.deleteByPostAndUser(post, user);
        post.decreaseLikeCount();
        postRepository.save(post);

        int count = (int) postLikeRepository.countByPost(post);
        post.setLikeCount(count);
        return count;
    }

    //  게시글 좋아요 개수 조회
    @Transactional(readOnly = true)
    public int getLikeCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
        return (int) postLikeRepository.countByPost(post);
    }
}
