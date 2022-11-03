package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
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

    private void invokeInitAction(
            String actionName,
            Supplier initAction
    ){
        if (incomingMsgSink != null){
            String rqId = UUID.randomUUID().toString();
            incomingMsgSink.accept(rqId,initAction.get());
            log.info("[WIRING]: invoke init action \"" + actionName + "\"");
        }
    }

    public Map<String, Supplier> initActions = new HashMap<>();

    public void registerInitAction(
            String actionName,
            Supplier initAction
    ){
        log.info("[WIRING]: register init action \"" + actionName + "\"");
        initActions.put(actionName,initAction);
    }

    public Runnable invokeInitActions(){
        return () -> {
          initActions.forEach(
                  (name, action) -> invokeInitAction(name, action)
          );
        };
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
