package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers.FluxChannelProvider;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers.MonoChannelProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChannelProvidersConfig {

    @Bean
    public FluxChannelProvider initFluxProvider(){
        return FluxChannelProvider.builder().build();
    }

    @Bean
    public MonoChannelProvider initMonoProvider(){
        return MonoChannelProvider.builder().build();
    }
}
