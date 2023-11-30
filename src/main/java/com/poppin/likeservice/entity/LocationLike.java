package com.poppin.likeservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class LocationLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "locationlike_id")
    private Long id;

    @Column(nullable = false)
    private Long locationId;

    @Column(nullable = false)
    private Long likeCount;

    @Builder
    public LocationLike(Long locationId, Long likeCount) {
        this.locationId = locationId;
        this.likeCount = likeCount;
    }
}
