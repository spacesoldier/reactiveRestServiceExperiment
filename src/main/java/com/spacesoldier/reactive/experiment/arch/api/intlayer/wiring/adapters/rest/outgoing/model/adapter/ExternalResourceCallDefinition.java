package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import reactor.core.CorePublisher;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public class ExternalResourceCallDefinition<T> {
    @Getter
    private String path;
    @Getter
    private HttpMethod method;

    @Getter
    private Class outgoingMsgType;

    @Getter
    private Function<T, CorePublisher> resourceInvocationCall;

    @Getter
    private Map<HttpStatus, Function> failStatusHandlers;
//    @Getter
//    private Function responseBodyHandler;

    @Builder
    private ExternalResourceCallDefinition(
            String path,
            HttpMethod method,
            Class outgoingMsgType,
            Function<T, CorePublisher> resourceInvocationCall,
            Map<HttpStatus, Function> errorHandlers
    ){
        this.path = path;
        this.method = method;

        this.outgoingMsgType = outgoingMsgType;
        this.resourceInvocationCall = resourceInvocationCall;

//        if (statusHandlers.containsKey(HttpStatus.OK)){
//            responseBodyHandler = statusHandlers.get(HttpStatus.OK);
//        }

        this.failStatusHandlers = new HashMap<>();

        errorHandlers.entrySet().stream()
                .filter(entry -> entry.getKey() != HttpStatus.OK)
                .toList().forEach(
                        entry -> this.failStatusHandlers.put(
                                                                entry.getKey(),
                                                                entry.getValue()
                                                            )
                );
    }
}
