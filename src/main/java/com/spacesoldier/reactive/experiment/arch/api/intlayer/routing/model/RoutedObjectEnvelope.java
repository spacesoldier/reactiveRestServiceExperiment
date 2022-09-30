package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RoutedObjectEnvelope {
    private String rqId;
    private Object payload;
}
