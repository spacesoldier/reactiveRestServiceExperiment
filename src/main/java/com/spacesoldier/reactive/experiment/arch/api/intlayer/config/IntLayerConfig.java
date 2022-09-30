package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.EndpointAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class IntLayerConfig {

    @Autowired
    WiringAdapter wiringAdapter;

    @Bean
    public EndpointAdapter initEndpointAdapter(){

        return EndpointAdapter.builder()
                                .monoProvider(
                                        rqId -> wiringAdapter.initSingleRequest(rqId)
                                )
                                .requestSink(
                                        (rqId, payload) -> wiringAdapter.receiveSingleRequest(rqId,payload)
                                )
                .build();
    }
}
