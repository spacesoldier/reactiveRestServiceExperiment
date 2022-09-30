package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
@Slf4j
public class IntlayerObjectRouter {

    private Map<Class, String> routingTable = new HashMap<>();

    private Function<String, Flux> fluxProvider;
    private Function<String, Consumer> sinkByRqIdProvider;

    private Function<String,Consumer> sinkByChannelNameProvider;

    @Builder
    private IntlayerObjectRouter(
            Function<String,Flux> fluxProvider,
            Function<String,Consumer> sinkByRqIdProvider,
            Function<String,Consumer> sinkByChannelNameProvider
    ){
        this.fluxProvider = fluxProvider;
        this.sinkByRqIdProvider = sinkByRqIdProvider;
        this.sinkByChannelNameProvider = sinkByChannelNameProvider;
    }

    public void addRoutableFunction(Class typeToRoute, Function routeToFn){

    }

    public BiConsumer<String, Object> singleRequestsInput(){

        String logTemplate = "[REQUEST] %s received";

        return (rqId, payload) -> {
            log.info(String.format(logTemplate,rqId));
        };
    }

}
