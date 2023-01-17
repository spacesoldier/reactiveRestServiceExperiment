package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.IntlayerObjectRouter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.TokenBucketRateLimiter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.QueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BandwidthControllerConfig {

    @Autowired
    WiringAdapter wiringAdapter;

    @Autowired
    IntlayerObjectRouter intlayerObjectRouter;

    @Autowired
    QueueManager queueManager;

    private String bandwidthGateName = "callGpAPI";

    @Bean
    public TokenBucketRateLimiter initTokenBucketBandwidthControl(){

        TokenBucketRateLimiter rateLimiter =
                TokenBucketRateLimiter.builder()
                                        .limiterName(bandwidthGateName)
                                        .bucketCapacity(400)
                                        .requestSink(
                                            (rqId, payload) -> wiringAdapter.receiveSingleRequest(rqId,payload)
                                        )
                                        .parkRequestSink(
                                                request ->{
                                                    RequestPriority priority = request.getPriority();

                                                    if (priority == null){
                                                        priority = RequestPriority.BACKGROUND;
                                                    }

                                                    queueManager
                                                            .queueOnDemand(bandwidthGateName)
                                                            .putItem(priority)
                                                            .accept(request);
                                                }

                                        )
                                        .parkRequestSource(
                                                    queueManager.queueOnDemand(bandwidthGateName).getItem()
                                        )
                                    .build();

        intlayerObjectRouter.addEnvelopeAggregation(
                RouterBypassRequest.class,
                rateLimiter.aggregateBypass()
        );

        wiringAdapter.registerFeature(
                RouterBypassRequest.class,
                requestObj -> {
                    RouterBypassRequest request = null;
                    try {
                        request = (RouterBypassRequest) requestObj;
                    } catch (Exception e){

                    }

                    if (request != null){
                        rateLimiter.receiveBypassRequest().accept(request);
                    }

                    return "Ok";
                }
        );

        return rateLimiter;
    }


}
