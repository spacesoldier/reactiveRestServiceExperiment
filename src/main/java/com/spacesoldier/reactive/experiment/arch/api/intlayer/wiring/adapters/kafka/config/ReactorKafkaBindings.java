package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

//@Configuration
@Component
@Data
@ConfigurationProperties(prefix = "reactor-kafka-config")
//@ConfigurationPropertiesScan
public class ReactorKafkaBindings {

    private String bootstrapServers;

    private Map<String,Map<String,String>> bindings;

    private Map<String,Map<String,Map<String,String>>> connections;




}
