package com.poppin.likeservice.controller;

import com.poppin.likeservice.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 좋아요 추가
     */
    @PostMapping("/add")
    public ResponseEntity<Void> addLike(@RequestParam Long userId, @RequestParam Long locationId) {
        likeService.addLike(userId, locationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 유저가 좋아요한 가게id 가져오기
     */
    @GetMapping("/user")
    public ResponseEntity<Page<Long>> getUserLikeLocation(@RequestParam Long userId, @RequestParam int page, @RequestParam int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> likeLocationId = likeService.getUserLikeLocation(userId, pageable);
        return ResponseEntity.ok(likeLocationId);
    }

    /**
     * 좋아요 삭제
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeLike(@RequestParam Long userId, @RequestParam Long locationId) {
        likeService.removeLike(userId, locationId);
        return ResponseEntity.ok().build();
    }
}
