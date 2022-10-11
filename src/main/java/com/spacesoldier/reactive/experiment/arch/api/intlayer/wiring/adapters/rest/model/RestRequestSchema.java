package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.model;

import org.springframework.util.MultiValueMap;

import java.util.Map;

public class RestRequestSchema<T> {
    public String requestId;
    public Map<String,String> pathVariables;
    public MultiValueMap<String,String> queryParams;
    public T payload;
}
