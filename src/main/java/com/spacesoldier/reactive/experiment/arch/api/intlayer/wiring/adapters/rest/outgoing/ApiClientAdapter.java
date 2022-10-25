package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter.ExternalResourceCallDefinition;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter.ResourceCallDescriptor;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ApiClientAdapter {

    @NonNull
    private BiConsumer<String, Object> intlayerInputSink;

    private BiConsumer<Class, Function> routableFunctionSink;

    @Builder
    private ApiClientAdapter(
            BiConsumer<String, Object> intlayerInputSink,
            BiConsumer<Class,Function> routableFunctionSink
    ){
        this.intlayerInputSink = intlayerInputSink;
        this.routableFunctionSink = routableFunctionSink;
    }

    public void registerResourceClient(ExternalResourceCallDefinition resourceCallDefinition){


        if (routableFunctionSink != null){
            routableFunctionSink.accept(
                    resourceCallDefinition.getOutgoingMsgType(),
                    resourceCallDefinition.getResourceInvocationCall()
            );
        }
    }

}
