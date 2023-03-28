package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.IntlayerObjectRouter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.RoutingHelper;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.TokenBucketRateLimiter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.LimiterPassRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.QueueManager;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.QueueTactics;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.RequestsQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BandwidthControllerConfig {
    private WiringAdapter wiringAdapter;
    private IntlayerObjectRouter intlayerObjectRouter;
    private QueueManager queueManager;
    private RoutingHelper routingHelper;

    public BandwidthControllerConfig(
            WiringAdapter wiringAdapter,
            IntlayerObjectRouter intlayerObjectRouter,
            QueueManager queueManager,
            RoutingHelper routingHelper
    ){
        this.wiringAdapter = wiringAdapter;
        this.intlayerObjectRouter = intlayerObjectRouter;
        this.queueManager = queueManager;
        this.routingHelper = routingHelper;

        this.requestsQueue = queueManager.queueOnDemand(
                bandwidthGateName,
                QueueTactics.ROUND_ROBIN
        );
    }

    private final String bandwidthGateName = "callGpAPI";

    private final RequestsQueue requestsQueue;

    @Bean
    public RequestsQueue initRateLimiterRequestsQueue(){
        return requestsQueue;
    }

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
                                                        request.setPriority(RequestPriority.BACKGROUND);
                                                        priority = request.getPriority();
                                                    }

                                                    requestsQueue
                                                            .putItem()
                                                            .accept(request);
                                                }

                                        )
                                        .parkRequestSource(
                                                    requestsQueue.getItem()
                                        )
                                        .requestPriorityDetector(
                                                requestId -> routingHelper.requestIsPrioritised(requestId)
                                        )
                                        .requestPrioritySetter(
                                                (rqId, priority) -> routingHelper.defineRequestPriority(rqId,priority)
                                        )
                                        .correlIdSetter(
                                                (rqId,correlId) -> routingHelper.encodeCorrelId(rqId,correlId)
                                        )
                                    .build();

        intlayerObjectRouter.addEnvelopeAggregation(
                LimiterPassRequest.class,
                rateLimiter.executePassedRequest()
        );

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
