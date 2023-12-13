package com.poppin.likeservice.controller;

import com.poppin.likeservice.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    //userId와 locationId를 통해 좋아요 추가
    @PostMapping("/")
    public ResponseEntity<Void> addLike(@RequestParam Long userId, @RequestParam Long locationId) {
        likeService.addLike(userId, locationId);
        return ResponseEntity.ok().build();
    }

    //레디스에 저장된 현재 특정 location의 좋아요 수
    @GetMapping("/redis-count")
    public ResponseEntity<Integer> getLikesCountFromRedis(@RequestParam Long locationId) {
        Integer likesCount = likeService.getLikesCountFromRedis(locationId);
        return likesCount != null ? ResponseEntity.ok(likesCount) : ResponseEntity.notFound().build();
    }

    //특정 유저가 좋아요한 location 목록 가져오기
    @GetMapping("/user")
    public ResponseEntity<Page<Long>> getUserLikeLocation(@RequestParam Long userId, @RequestParam int page, @RequestParam int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> likeLocationId = likeService.getUserLikeLocation(userId, pageable);
        return ResponseEntity.ok(likeLocationId);
    }

    //특정 유저가 location 좋아요 삭제
    @DeleteMapping("/")
    public ResponseEntity<Void> removeLike(@RequestParam Long userId, @RequestParam Long locationId) {
        likeService.removeLike(userId, locationId);
        return ResponseEntity.ok().build();
    }
}
