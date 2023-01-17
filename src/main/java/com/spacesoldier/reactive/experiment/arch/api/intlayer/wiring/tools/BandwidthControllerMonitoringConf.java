package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.RateLimiterMonitor;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.TokenBucketRateLimiter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.QueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class BandwidthControllerMonitoringConf {
    @Autowired
    QueueManager queueManager;

    @Autowired
    TokenBucketRateLimiter rateLimiter;

    @Bean
    public RateLimiterMonitor initRateLimiterMonitor(){

        RateLimiterMonitor monitor = RateLimiterMonitor.builder()
                                                            .queuedRequestsCount(
                                                                    queueManager.queueOnDemand(
                                                                                    rateLimiter.getName()
                                                                                )
                                                                                .queueSize()
                                                            )
                                                            .build();
        rateLimiter.connectOverloadMonitoring(
                monitor.setOverloadStart(),
                monitor.checkOverloadEnd()
        );

        queueManager.queueOnDemand(rateLimiter.getName())
                    .subscribeOnItemGet(
                        item -> {
                            RouterBypassRequest request = null;
                            if (item instanceof RouterBypassRequest){
                                request = (RouterBypassRequest) item;
                            }

                            if (request != null){

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
