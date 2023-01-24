package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.RateLimiterMonitor;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.TokenBucketRateLimiter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RateLimiterPauseReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class BandwidthControlReportsConf {
    @Autowired
    TokenBucketRateLimiter rateLimiter;

    @Autowired
    RateLimiterMonitor rateLimiterMonitor;
    @Autowired
    WiringAdapter wiringAdapter;

    @Bean
    public void configRateLimiterPausesReporting(){
        wiringAdapter.registerFeature(
                RateLimiterPauseReportRequest.class,
                inputObj -> {
                    try{
                        RateLimiterPauseReportRequest statusRq = (RateLimiterPauseReportRequest) inputObj;
                        rateLimiterMonitor.reportStatsPerMinute();
                    } catch (Exception e){
                        log.info("[RATE LIMITER]: input is not a RateLimiterPauseReportRequest");
                    }
                    return "Ok";
                }
        );
    }

    @Bean
    @Scheduled(fixedDelay = 60000, initialDelay = 20000)
    public void initRateLimiterPauseReportSchedule(){
        String requestId = ">|<" + UUID.randomUUID();
        wiringAdapter.initSingleRequest(requestId)

                .subscribe(
                        message -> {
                            log.info("[RATE LIMITER]: report "+message.toString());
                        }
                );

        RateLimiterPauseReportRequest pauseReportRq = RateLimiterPauseReportRequest.builder()
                .build();

        wiringAdapter.receiveSingleRequest(
                requestId,
                pauseReportRq
        );
    }
}
