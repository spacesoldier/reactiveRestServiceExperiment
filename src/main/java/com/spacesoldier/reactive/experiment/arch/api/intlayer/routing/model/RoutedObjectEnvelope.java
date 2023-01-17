package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RoutedObjectEnvelope {
    private String rqId;
    private String correlId; // for future one-to-many transformations and etc
    private RequestPriority priority;
    private Object payload;
}
