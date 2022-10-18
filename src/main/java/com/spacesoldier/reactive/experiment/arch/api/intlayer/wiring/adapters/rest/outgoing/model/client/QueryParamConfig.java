package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.client;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class QueryParamConfig {
    CollectionFormat collectionFormat;
    String name;
    Object value;
}
