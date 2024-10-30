package org.project.paysystem.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.paysystem.entity.GlobalPricing;
import org.project.paysystem.entity.MediaTypeEnum;
import org.project.paysystem.entity.VideoDailyRevenue;
import org.project.paysystem.entity.VideoDailyStats;
import org.project.paysystem.repository.GlobalPricingRepository;
import org.project.paysystem.repository.VideoDailyRevenueRepository;
import org.project.paysystem.repository.VideoDailyStatsRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "동영상 일별 정산 배치")
@Configuration
@RequiredArgsConstructor
public class VideoDailyRevenueBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final VideoDailyStatsRepository videoDailyStatsRepository;
    private final GlobalPricingRepository pricingRepository;
    private final VideoDailyRevenueRepository dailyRevenueRepository;

    private final int chunkSize = 10;
    private MediaTypeEnum type = MediaTypeEnum.VIDEO;

    @Bean
    public Job videoDailyRevenueJob() throws ParseException {
        log.info("videoDailyRevenueJob");

        return new JobBuilder("videoDailyRevenueJob", jobRepository)
                .start(globalPricingTaskletStep())
                .next(dailyRevenueStep())
                .build();
    }

    // tasklet
    @Bean
    public Step globalPricingTaskletStep() {
        return new StepBuilder("globalPricingTaskletStep", jobRepository)
                .tasklet(globalPricingTasklet(), transactionManager)
                .listener(priceListener())
                .build();
    }

    @Bean
    public ExecutionContextPromotionListener priceListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{"priceList"});
        return listener;
    }

    @Bean
    public Tasklet globalPricingTasklet() {
        return (contribution, chunkContext) -> {
            List<GlobalPricing> priceList = pricingRepository.findAllByType(type);
            ExecutionContext stepContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
            stepContext.put("priceList", priceList);
            return RepeatStatus.FINISHED;
        };
    }

    // Video daily revenue step
    @Bean
    public Step dailyRevenueStep() throws ParseException {
        log.info("dailyRevenueStep");

        return new StepBuilder("dailyRevenueStep", jobRepository)
                .<VideoDailyStats, VideoDailyRevenue> chunk(chunkSize, transactionManager)
                .reader(getDailyRevenueReader(null))
                .processor(dailyRevenueProcessor(null))
                .writer(dailyRevenueWriter())
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<VideoDailyStats> getDailyRevenueReader(
            @Value("#{jobParameters[currentDate]}") String targetDate) throws ParseException {
        LocalDate currentDate = LocalDate.parse(targetDate.substring(0, 10));

        return new RepositoryItemReaderBuilder<VideoDailyStats>()
                .name("getDailyRevenueReader")
                .repository(videoDailyStatsRepository)
                .methodName("findByCreatedAt")
                .arguments(currentDate)
                .pageSize(chunkSize)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();

    }

    @Bean
    @StepScope
    public ItemProcessor<VideoDailyStats, VideoDailyRevenue> dailyRevenueProcessor(
            @Value("#{jobExecutionContext[priceList]}") List<GlobalPricing> priceList) {

        return new ItemProcessor<VideoDailyStats, VideoDailyRevenue>() {

            @Override
            public VideoDailyRevenue process(VideoDailyStats item) throws Exception {
                long totalAmount = calculateAmountForViews(priceList, item.getDailyViews());

                return VideoDailyRevenue.builder()
                        .video(item.getVideo())
                        .videoAmount(totalAmount)
                        .build();
            }
        };
    }

    @Bean
    public RepositoryItemWriter<VideoDailyRevenue> dailyRevenueWriter() {
        return new RepositoryItemWriterBuilder<VideoDailyRevenue>()
                .repository(dailyRevenueRepository)
                .methodName("save")
                .build();
    }

    public long calculateAmountForViews(List<GlobalPricing> priceList, long views) {
        long totalAmount = 0;
        long remainingViews = views;

        for (int i = 0; i < priceList.size(); i++) {
            GlobalPricing price = priceList.get(i);
            long minViews = price.getMinViews();
            long maxViews = price.getMaxViews() == 0 ? Long.MAX_VALUE : price.getMaxViews(); // 상한이 없는 경우를 처리

            // 마지막 구간 여부를 확인
            boolean isLastTier = (i == priceList.size() - 1);

            if (isLastTier) {
                // 마지막 구간에서는 remainingViews 전체에 단가를 적용
                totalAmount += (long) (remainingViews * price.getUnitPrice());
                break;
            } else {
                // 마지막 구간이 아닐 때는 구간별 조회수를 계산
                if (remainingViews > minViews) {
                    long viewsInThisRange = Math.min(remainingViews, maxViews - minViews);

                    totalAmount += (long) (viewsInThisRange * price.getUnitPrice());
                    remainingViews -= viewsInThisRange;
                }
            }
            // 남은 조회수가 0이면 더 이상 계산하지 않음
            if (remainingViews <= 0) {
                break;
            }
        }

        return totalAmount;
    }
}
