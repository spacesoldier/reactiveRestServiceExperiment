package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

@Data @Builder
public class ErrorStatusHandlerDefinition {
    private String path;
    private HttpMethod method;
    private HttpStatus errorStatus;
    private Function<String, ? extends Throwable> errorHandler;
}
