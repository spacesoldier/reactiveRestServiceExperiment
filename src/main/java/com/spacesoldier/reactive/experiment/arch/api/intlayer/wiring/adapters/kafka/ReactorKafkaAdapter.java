package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka;

import lombok.Builder;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ReactorKafkaAdapter {

    private Map<String, BiFunction<Object,Object,Object>> incomingMessageParsers = new HashMap<>();

    private Map<Class, Function<Object, String>> outgoingMessageConverters = new HashMap<>();

    private Function<String, Flux> fluxByNameProvider;

    @Builder
    private ReactorKafkaAdapter(
            Function<String, Flux> fluxByNameProvider
    ){
        this.fluxByNameProvider = fluxByNameProvider;
    }

    public void registerIncomingMessageTransformer(String bindingName, BiFunction<Object,Object,Object> inputConverter){
        if (!incomingMessageParsers.containsKey(bindingName)){
            incomingMessageParsers.put(bindingName, inputConverter);
        }
    }

    public void registerOutgoingMessageConverter(Class typeToReceive, Function<Object,String> outgoingMsgConverter){
        if (!outgoingMessageConverters.containsKey(outgoingMsgConverter)){
            outgoingMessageConverters.put(typeToReceive,outgoingMsgConverter);
        }
    }

    public void prepareForOperations(){

    }

    public void startOperations(){

    }

}