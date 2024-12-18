package com.project.revenueservice.service;

import com.project.revenueservice.dto.RankVideoInfoDto;
import com.project.revenueservice.dto.Top5RequestDto;
import com.project.revenueservice.dto.Top5ResponseDto;
import com.project.revenueservice.repository.VideoDailyStatsRepository;
import com.project.revenueservice.repository.VideoMonthlyStatsRepository;
import com.project.revenueservice.repository.VideoWeeklyStatsRepository;
import com.project.revenueservice.util.LastUpdatedStatsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final VideoDailyStatsRepository dailyStatsRepository;
    private final VideoWeeklyStatsRepository weeklyStatsRepository;
    private final VideoMonthlyStatsRepository monthlyStatsRepository;

    public Top5ResponseDto top5ViewsVideo(Top5RequestDto requestDto) {
        String currentDate = requestDto.getCurrentDate() == null ? String.valueOf(LocalDate.now()) : requestDto.getCurrentDate();
        String period = requestDto.getPeriod();
        String type = requestDto.getType();

        // 일, 주, 월에 따른 조회수 또는 재생시간 Top5 가져오기
        List<RankVideoInfoDto> rankVideoViewsDtoList = fetchPeriodViewsData(currentDate, period, type);

        return Top5ResponseDto.builder()
                .type(type)
                .period(period)
                .searchDate(LocalDate.parse(currentDate))
                .rankVideoInfoDtoList(rankVideoViewsDtoList)
                .build();
    }

    // 일, 주, 월에 따른 조회수 또는 재생시간 Top5 가져오기
    @Transactional
    public List<RankVideoInfoDto> fetchPeriodViewsData(String currentDate, String period, String type) {
        List<RankVideoInfoDto> rankVideoInfoDtoList = new ArrayList<>();
        LocalDate targetDate = LocalDate.parse(currentDate.substring(0, 10));

        if(period.equals("day")) {
            if(type.equals("views")) { // 조회수
                rankVideoInfoDtoList = dailyStatsRepository.findTop5ByCreatedAtOrderByDailyViewsDesc(targetDate);

            } else if(type.equals("watchtime")) { // 재생 시간
                rankVideoInfoDtoList = dailyStatsRepository.findTop5ByCreatedAtOrderByDailyWatchTimeDesc(targetDate);
            }
       } else if(period.equals("week")) {
            LocalDate nextSunday = getNextSundayFromTargetDate(targetDate); // targetDate 이후 가장 가까운 일요일을 계산
            if(type.equals("views")) { // 조회수
                rankVideoInfoDtoList = weeklyStatsRepository.findTop5ByCreatedAtOrderByWeeklyViewsDesc(nextSunday);

            } else if(type.equals("watchtime")) { // 재생 시간
                rankVideoInfoDtoList = weeklyStatsRepository.findTop5ByCreatedAtOrderByWeeklyWatchTimeDesc(nextSunday);
            }
        } else if(period.equals("month")) {
            YearMonth targetMonth = YearMonth.parse(currentDate.substring(0, 7)); // 'yyyy-mm'

            if (type.equals("views")) { // 조회수
                rankVideoInfoDtoList = monthlyStatsRepository.findTop5ByCreatedAtOrderByMonthlyViewsDesc(String.valueOf(targetMonth));

            } else if(type.equals("watchtime")) { // 재생 시간
                log.info("targetMonth {}",targetMonth);
                rankVideoInfoDtoList = monthlyStatsRepository.findTop5ByCreatedAtOrderByMonthlyWatchTimeDesc(String.valueOf(targetMonth));
                log.info("rankVideoInfoDtoList {}",rankVideoInfoDtoList);
            }
        }

        for(int i = 0 ; i < rankVideoInfoDtoList.size(); i++) {
            rankVideoInfoDtoList.get(i).updateRank(i+1);
        }

        return rankVideoInfoDtoList;
    }


    public LocalDate getWeeklyLastUpdate() {
        return weeklyStatsRepository.findWeeklyLastUpdate();
    }

    public LocalDate getNextSundayFromTargetDate(LocalDate targetDate) {
        return targetDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    public LocalDate getMonthlyLastUpdate() {
        return monthlyStatsRepository.findMonthlyLastUpdate();
    }

    public LocalDate getDailyLastUpdate() {
        return dailyStatsRepository.findLastUpdate();
    }


}
