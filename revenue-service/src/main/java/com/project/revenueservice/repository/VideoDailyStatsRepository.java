package com.project.revenueservice.repository;

import com.project.revenueservice.dto.RankVideoInfoDto;
import com.project.revenueservice.entity.VideoDailyStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;


public interface VideoDailyStatsRepository extends JpaRepository<VideoDailyStats, Long> {

    @Query("SELECT vds.createdAt FROM VideoDailyStats vds ORDER BY vds.createdAt DESC LIMIT 1")
    LocalDate findLastUpdate();

    @Query("SELECT new com.project.revenueservice.dto.RankVideoInfoDto(vds.videoId, vds.dailyViews, 0) "+
            "FROM VideoDailyStats vds "+
            "WHERE vds.createdAt = :targetDate "+
            "ORDER BY vds.dailyViews DESC LIMIT 5"
    ) List<RankVideoInfoDto> findTop5ByCreatedAtOrderByDailyViewsDesc(@Param("targetDate") LocalDate parsedDate);

    @Query("SELECT new com.project.revenueservice.dto.RankVideoInfoDto(vds.videoId, 0, vds.dailyWatchTime) "+
            "FROM VideoDailyStats vds "+
            "WHERE vds.createdAt = :targetDate "+
            "ORDER BY vds.dailyWatchTime DESC LIMIT 5"
    ) List<RankVideoInfoDto> findTop5ByCreatedAtOrderByDailyWatchTimeDesc(@Param("targetDate") LocalDate targetDate);

    // batch
    VideoDailyStats findByVideoIdAndCreatedAt(Long videoId, LocalDate createdAt);

    Page<VideoDailyStats> findByCreatedAt(@Param("currentDate") LocalDate currentDate, Pageable pageable);
}
