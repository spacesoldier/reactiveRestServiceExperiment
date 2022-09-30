package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters;

import lombok.Builder;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.function.BiConsumer;
import java.util.function.Function;


public class WiringAdapter {
    @NonNull
    private BiConsumer<String, Object> incomingMsgSink;
    @NonNull
    private BiConsumer<Class, Function> routableFunctionSink;

    @NonNull
    private Function<String, Mono> monoProvider;

    @Builder
    private WiringAdapter(
            BiConsumer<String, Object> incomingMsgSink,
            Function<String,Mono> monoProvider,
            BiConsumer<Class,Function> routableFunctionSink
    ){
        this.incomingMsgSink = incomingMsgSink;
        this.monoProvider = monoProvider;
        this.routableFunctionSink = routableFunctionSink;
    }

    public void registerFeature(
            Class inputType,
            Function featureFunction
    ){
        if (routableFunctionSink != null){
            routableFunctionSink.accept(inputType, featureFunction);
        }
    }

    public Mono initSingleRequest(String wireId){

        return monoProvider.apply(wireId);
    }

    public void receiveSingleRequest(String rqId, Object payload){
        if (incomingMsgSink != null){
            incomingMsgSink.accept(rqId,payload);
        }
    }

}
