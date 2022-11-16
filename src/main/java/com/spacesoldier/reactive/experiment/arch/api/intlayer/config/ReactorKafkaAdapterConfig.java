package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka.ReactorKafkaAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka.config.ReactorKafkaBindings;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers.FluxChannelProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(ReactorKafkaBindings.class)
public class ReactorKafkaAdapterConfig {

    @Autowired
    WiringAdapter wiringAdapter;

    @Autowired
    FluxChannelProvider fluxChannelProvider;

    @Bean
    public ReactorKafkaAdapter prepareReactorKafkaAdapter(){


        return ReactorKafkaAdapter.builder()
                                        .fluxByNameProvider(
                                                channelName -> fluxChannelProvider.getStream(channelName)
                                        )
                                    .build();

    }

}
