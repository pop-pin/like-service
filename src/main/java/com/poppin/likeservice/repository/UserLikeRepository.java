package com.poppin.likeservice.repository;

import com.poppin.likeservice.entity.UserLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLikeRepository extends JpaRepository<UserLike, Long> {
    Optional<UserLike> findByUserIdAndLocationId(Long userId, Long locationId);

    Page<UserLike> findByUserId(Long userId, Pageable pageable);
}
