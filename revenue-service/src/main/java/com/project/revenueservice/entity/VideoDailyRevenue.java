package com.project.revenueservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="videoDailyRevenue")
public class VideoDailyRevenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long videoId;

    private long videoAmount; // 동영상 정산 금액

    private LocalDate createdAt;

    @Builder
    public VideoDailyRevenue(Long videoId, long videoAmount, LocalDate createdAt) {
        this.videoId = videoId;
        this.videoAmount = videoAmount;
        this.createdAt = LocalDate.now();
    }
}
