package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter.ErrorStatusHandlerDefinition;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter.ExternalResourceCallDefinition;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ApiClientAdapter {

    private Consumer<ErrorStatusHandlerDefinition> errorHandlerSink;

    private BiConsumer<Class, Function> routableFunctionSink;

    @Builder
    private ApiClientAdapter(
            BiConsumer<Class,Function> routableFunctionSink,
            Consumer<ErrorStatusHandlerDefinition> errorHandlerSink
    ){
        this.routableFunctionSink = routableFunctionSink;
        this.errorHandlerSink = errorHandlerSink;
    }

    public void registerResourceClient(ExternalResourceCallDefinition resourceCallDefinition){

        if (errorHandlerSink != null){
            Map<HttpStatus, Function<String, Throwable>> errorHandlers = resourceCallDefinition.getFailStatusHandlers();

            if (errorHandlers != null && !errorHandlers.isEmpty()){
                errorHandlers.forEach(
                        (errorStatus, errorHandler) -> errorHandlerSink.accept(
                                ErrorStatusHandlerDefinition.builder()
                                                                .path(resourceCallDefinition.getPath())
                                                                .method(resourceCallDefinition.getMethod())
                                                                .errorStatus(errorStatus)
                                                                .errorHandler(errorHandler)
                                                            .build()
                        )
                );
            }
        }

        if (routableFunctionSink != null){
            routableFunctionSink.accept(
                    resourceCallDefinition.getOutgoingMsgType(),
                    resourceCallDefinition.getResourceInvocationCall()
            );
        }
    }

}
