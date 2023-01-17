package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.RoutingHelper;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.startup.AppReadyListener;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.IntlayerObjectRouter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers.FluxChannelProvider;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers.MonoChannelProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RouterConfig {

    @Autowired
    MonoChannelProvider monoChannelProvider;

    @Autowired
    FluxChannelProvider fluxChannelProvider;

    @Autowired
    RoutingHelper routingHelper;

    @Bean
    public IntlayerObjectRouter initObjectRouter(){

        return IntlayerObjectRouter.builder()
                    .sinkByRqIdProvider         (   requestId -> monoChannelProvider.getInput(requestId)             )
                    .sinkByChannelNameProvider  (   channelName -> fluxChannelProvider.getExistingSink(channelName)  )
                    .fluxProvider               (   channelName -> fluxChannelProvider.getStream(channelName)        )
                    .requestPriorityDetector    (   requestId -> routingHelper.requestIsPrioritised(requestId)       )
                    .requestPriorityExtractor   (   requestId -> routingHelper.parsePriorityForRequestID(requestId)  )
                    .removePriorityFromRqId     (   requestId -> routingHelper.removePriorityFromRequestID(requestId))
                .build();
    }

    @Bean
    public AppReadyListener initAppListener(){
        return new AppReadyListener();
    }
}
