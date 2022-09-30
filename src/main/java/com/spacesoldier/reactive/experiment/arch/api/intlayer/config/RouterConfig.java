package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

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

    @Bean
    public IntlayerObjectRouter initObjectRouter(){

        return IntlayerObjectRouter.builder()
                    .sinkByRqIdProvider(requestId -> monoChannelProvider.getInput(requestId))
                    .sinkByChannelNameProvider(channelName -> fluxChannelProvider.getSink(channelName))
                    .fluxProvider(channelName -> fluxChannelProvider.getStream(channelName))
                .build();
    }
}
