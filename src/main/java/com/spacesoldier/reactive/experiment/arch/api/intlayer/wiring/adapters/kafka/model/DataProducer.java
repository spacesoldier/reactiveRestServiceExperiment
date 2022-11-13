package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka.model;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DataProducer{
    private String clientId;
    private String acks;
    private String keySerializer;
    private String valueSerializer;
}
