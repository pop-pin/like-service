package com.poppin.likeservice.service;

import com.poppin.likeservice.entity.LocationLike;
import com.poppin.likeservice.entity.UserLike;
import com.poppin.likeservice.repository.LocationLikeRepository;
import com.poppin.likeservice.repository.UserLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LikeService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final UserLikeRepository userLikeRepository;
    private final LocationLikeRepository locationLikeRepository;

    @Transactional
    public void addLike(Long userId, Long locationId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = "locationId::" + locationId;
        String hashkey = "likes";

        Long likesCount = (Long) hashOperations.get(key, hashkey);
        if (likesCount == null) {
            Optional<LocationLike> locationLike = locationLikeRepository.findByLocationId(locationId);
            if (locationLike.isPresent()) {
                likesCount = locationLike.get().getLikeCount();
            } else {
                String url = "http://location-service/location/likes-count"+locationId;
                likesCount = restTemplate.getForObject(url, Long.class);
            }

            hashOperations.put(key, hashkey, likesCount);
        }

        hashOperations.increment(key, hashkey, 1L);

        String eventKey = "likesEvents";
        String eventValue = "add:" + userId + ":" + locationId;
        redisTemplate.opsForList().leftPush(eventKey, eventValue);
    }

    public Page<Long> getUserLikeLocation(Long userId, Pageable pageable) {
        Page<UserLike> userLikes = userLikeRepository.findByUserId(userId, pageable);
        return userLikes.map(UserLike::getLocationId);
    }

    @Transactional
    public void removeLike(Long userId, Long locationId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = "locationId::" + locationId;
        String hashkey = "likes";

        Long likesCount = (Long) hashOperations.get(key, hashkey);
        if (likesCount == null) {
            Optional<LocationLike> locationLike = locationLikeRepository.findByLocationId(locationId);
            likesCount = locationLike.get().getLikeCount();
            hashOperations.put(key, hashkey, likesCount);
        }

        if (likesCount > 0) {
            hashOperations.increment(key, hashkey, -1L);

            String eventKey = "likesEvents";
            String eventValue = "remove:" + userId + ":" + locationId;
            redisTemplate.opsForList().leftPush(eventKey, eventValue);
        }
    }


    @Scheduled(fixedDelay = 10000)
    public void processLikeEvent() {
        String eventKey = "likesEvents";
        while (true) {
            String eventValue = (String) redisTemplate.opsForList().rightPop(eventKey);
            if (eventValue == null) {
                break;
            }

            String[] parts = eventValue.split(":");
            String action = parts[0];
            Long userId = Long.parseLong(parts[1]);
            Long locationId = Long.parseLong(parts[2]);

            if ("add".equals(action)) {
                // 좋아요 추가에 대한 데이터베이스 업데이트 로직
                UserLike userLike = UserLike.builder()
                        .userId(userId)
                        .locationId(locationId)
                        .build();
                userLikeRepository.save(userLike);

                locationLikeRepository.findByLocationId(locationId).ifPresentOrElse(
                        locationLike -> {
                            locationLike.setLikeCount(locationLike.getLikeCount() + 1);
                            locationLikeRepository.save(locationLike);
                        },
                        () -> {
                            LocationLike newLocationLike = LocationLike.builder()
                                    .locationId(locationId)
                                    .likeCount(1L)
                                    .build();
                            locationLikeRepository.save(newLocationLike);
                        }
                );

            } else if ("remove".equals(action)) {
                // 좋아요 제거에 대한 데이터베이스 업데이트 로직
                userLikeRepository.findByUserIdAndLocationId(userId, locationId)
                        .ifPresent(userLikeRepository::delete);

                locationLikeRepository.findByLocationId(locationId).ifPresent(
                        locationLike -> {
                            long newCount = Math.max(0, locationLike.getLikeCount() - 1);
                            locationLike.setLikeCount(newCount);
                            locationLikeRepository.save(locationLike);
                        }
                );
            }
        }
    }


}
