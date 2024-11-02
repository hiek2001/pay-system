package com.project.revenueservice.batch;

import com.project.revenueservice.dto.VideoWeeklyStatsBatchDto;
import com.project.revenueservice.entity.VideoMonthlyStats;
import com.project.revenueservice.repository.VideoMonthlyStatsRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Map;

@Slf4j(topic = "동영상 월별 통계 배치")
@Configuration
@RequiredArgsConstructor
public class VideoMonthlyStatsBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    //private final VideoRepository videoRepository;
    private final VideoMonthlyStatsRepository monthlyStatsRepository;

    private final int chunkSize = 10;

    @Bean
    public Job videoMonthlyStatsJob() {
        log.info("동영상 - 월별 통계 배치 시작");

        return new JobBuilder("videoMonthlyStatsJob", jobRepository)
                .start(monthlyStatsStep())
                .build();
    }

    // 조회수, 재생 시간
    @Bean
    public Step monthlyStatsStep() {
        log.info("monthlyStatsStep ");

        return new StepBuilder("monthlyStatsStep", jobRepository)
                .<VideoWeeklyStatsBatchDto, VideoMonthlyStats> chunk(chunkSize, transactionManager)
                .reader(getWeeklyStatsReader(null))
                .processor(monthlyStatsProcessor())
                .writer(monthlyStatsWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<VideoWeeklyStatsBatchDto> getWeeklyStatsReader(
            @Value("#{jobParameters[targetMonth]}") String targetMonth) {
        log.info("getWeeklyStatsReader");

        return new JpaPagingItemReaderBuilder<VideoWeeklyStatsBatchDto>()
                .name("getWeeklyStatsReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT new com.project.revenueservice.dto.VideoWeeklyStatsBatchDto(vws.videoId, SUM(vws.weeklyViews), SUM(vws.weeklyWatchTime)) "+
                            "FROM VideoWeeklyStats vws WHERE DATE_FORMAT(vws.createdAt, '%Y-%m') = :targetMonth GROUP BY vws.videoId"
                )
                .parameterValues(Map.of("targetMonth", targetMonth))
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public ItemProcessor<VideoWeeklyStatsBatchDto, VideoMonthlyStats> monthlyStatsProcessor() {
        log.info("weeklyStatsProcessor");

        return new ItemProcessor<VideoWeeklyStatsBatchDto, VideoMonthlyStats>() {

            @Override
            public VideoMonthlyStats process(VideoWeeklyStatsBatchDto item) throws Exception {
                //Video video = videoRepository.batchFindById(item.getVideoId());

                return VideoMonthlyStats.builder()
                        .videoId(item.getVideoId())
                        .monthlyViews(item.getWeeklyViews())
                        .monthlyWatchTime(item.getWeeklyWatchTime())
                        .createdAt(LocalDate.now().withDayOfMonth(1)) // 월의 시작일을 기준으로 저장
                        .build();
            }
        };
    }
    @Bean
    public RepositoryItemWriter<VideoMonthlyStats> monthlyStatsWriter() {
        log.info("동영상 - 월별 통계 배치 끝---");
        return new RepositoryItemWriterBuilder<VideoMonthlyStats>()
                .repository(monthlyStatsRepository)
                .methodName("save")
                .build();
    }

}
