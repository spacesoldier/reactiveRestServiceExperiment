package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ReactorKafkaConfBuilder {

    @Autowired
    ReactorKafkaBindings reactorKafkaBindings;

    @Bean
    public void prepareKafkaAdapterConfiguration(){

        String beep = "boop";

    }
}
