package com.poppin.likeservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "location_like")
public class LocationLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "locationlike_id")
    private Long id;

    @Column(nullable = false)
    private Long locationId;

    @Column(nullable = false)
    private Integer likeCount;

    @Builder
    public LocationLike(Long locationId, Integer likeCount) {
        this.locationId = locationId;
        this.likeCount = likeCount;
    }
}
