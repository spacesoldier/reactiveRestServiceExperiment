package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.RateLimiterMonitor;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.TokenBucketRateLimiter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.RequestsQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Slf4j
public class BandwidthControllerMonitoringConf {

    @Autowired
    RequestsQueue requestsQueue;

    @Autowired
    TokenBucketRateLimiter rateLimiter;

    @Bean
    public RateLimiterMonitor initRateLimiterMonitor(){

        RateLimiterMonitor monitor = RateLimiterMonitor.builder()
                                                                .queuedRequestsCount(
                                                                        requestsQueue.queueSize()
                                                                )
                                                                .bandwidth(rateLimiter.getBucketCapacity())
                                                            .build();
        rateLimiter.connectOverloadMonitoring(
                monitor.setOverloadStart(),
                monitor.checkOverloadEnd()
        );

        rateLimiter.connectTokenStatMonitoring(
                monitor.requestCheckIn(),
                monitor.requestCheckOut()
        );

        requestsQueue
                .subscribeOnItemGet(
                        request -> {

                            if (request != null){

                                if (request.getPriority() == RequestPriority.USER_LEVEL){
                                    log.info("[RATE LIMITER]: revoke USER request "+request.getRequestId());
                                }

                                request.setBypassEnd(OffsetDateTime.now());

                                monitor.requestDurationMeterSink().accept(
                                        request.getBypassStart(),
                                        request.getBypassEnd()
                                );
                            }
                        }
                );

        return monitor;
    }
}
