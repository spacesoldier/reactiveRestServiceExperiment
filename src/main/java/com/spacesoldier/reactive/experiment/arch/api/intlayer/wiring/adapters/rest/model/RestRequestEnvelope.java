package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Data @Builder
public class RestRequestEnvelope {
    private String requestId;
    private Map<String,String> pathVariables;
    private MultiValueMap<String,String> queryParams;
    private Object payload;
}
