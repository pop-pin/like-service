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

    @Transactional(readOnly = false)
    public void addLike(Long userId, Long locationId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = "locationId::" + locationId;
        String hashkey = "likes";

        Integer likesCount;
        Object rawLikesCount = hashOperations.get(key, hashkey);
        if (rawLikesCount instanceof Integer) {
            likesCount = (Integer) rawLikesCount;
        } else if (rawLikesCount instanceof Long) {
            likesCount = ((Long) rawLikesCount).intValue();
        } else {// likecount가 레디스에 없을 때
            Optional<LocationLike> locationLike = locationLikeRepository.findByLocationId(locationId);
            if (locationLike.isPresent()) {
                likesCount = locationLike.get().getLikeCount();
            } else {//likecount가 데베에도 없을때
                likesCount = 0;
                LocationLike saveLocationLike = LocationLike.builder()
                        .locationId(locationId)
                        .likeCount(0)
                        .build();
                locationLikeRepository.save(saveLocationLike);
            }
            hashOperations.put(key, hashkey, likesCount);
        }

        hashOperations.increment(key, hashkey, 1);

        String eventKey = "likesEvents";
        String eventValue = "add:" + userId + ":" + locationId;
        redisTemplate.opsForList().leftPush(eventKey, eventValue);
    }

    public Integer getLikesCountFromRedis(Long locationId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = "locationId::" + locationId;
        String hashkey = "likes";

        Object rawLikesCount = hashOperations.get(key, hashkey);
        if (rawLikesCount instanceof Integer) {
            return (Integer) rawLikesCount;
        } else if (rawLikesCount instanceof Long) {
            return ((Long) rawLikesCount).intValue();
        } else {
            return null;
        }
    }

    public Page<Long> getUserLikeLocation(Long userId, Pageable pageable) {
        Page<UserLike> userLikes = userLikeRepository.findByUserId(userId, pageable);
        return userLikes.map(UserLike::getLocationId);
    }

    @Transactional(readOnly = false)
    public void removeLike(Long userId, Long locationId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = "locationId::" + locationId;
        String hashkey = "likes";

        Integer likesCount;
        Object rawLikesCount = hashOperations.get(key, hashkey);
        if (rawLikesCount instanceof Integer) {
            likesCount = (Integer) rawLikesCount;
        } else if (rawLikesCount instanceof Long) {
            likesCount = ((Long) rawLikesCount).intValue();
        } else {
            Optional<LocationLike> locationLike = locationLikeRepository.findByLocationId(locationId);
            likesCount = locationLike.isPresent() ? locationLike.get().getLikeCount() : 0;
            hashOperations.put(key, hashkey, likesCount);
        }

        if (likesCount > 0) {
            hashOperations.increment(key, hashkey, -1);
            String eventKey = "likesEvents";
            String eventValue = "remove:" + userId + ":" + locationId;
            redisTemplate.opsForList().leftPush(eventKey, eventValue);
        }
    }


    @Scheduled(fixedDelay = 1000)//현재 빠른 확인을 위해 1초로 설정
    @Transactional(readOnly = false)
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
                        () -> {//혹시라도 데베에 저장되어있지 않다면
                            LocationLike newLocationLike = LocationLike.builder()
                                    .locationId(locationId)
                                    .likeCount(1)
                                    .build();
                            locationLikeRepository.save(newLocationLike);
                        }
                );

            } else if ("remove".equals(action)) {
                userLikeRepository.findByUserIdAndLocationId(userId, locationId)
                        .ifPresent(userLikeRepository::delete);

                locationLikeRepository.findByLocationId(locationId).ifPresent(
                        locationLike -> {
                            int newCount = Math.max(0, locationLike.getLikeCount() - 1);
                            locationLike.setLikeCount(newCount);
                            locationLikeRepository.save(locationLike);
                        }
                );
            }
        }
    }

}
