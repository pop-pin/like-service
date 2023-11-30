package com.poppin.likeservice.repository;

import com.poppin.likeservice.entity.LocationLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationLikeRepository extends JpaRepository<LocationLike, Long> {
    Optional<LocationLike> findByLocationId(Long locationId);
}
