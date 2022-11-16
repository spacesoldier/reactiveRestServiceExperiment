package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka.model;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DataConsumer{
    private String clientId;
    private String groupId;
    private String keyDeserializer;
    private String valueDeserializer;
    private String autoOffsetReset;
}