package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RoutedObjectEnvelope;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RoutingUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class IntlayerObjectRouter {

    private Map<Class, List<RoutingUnit>> routingUnits = new HashMap<>();
    private Map<Class,List<String>> routingTable = new HashMap<>();

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

    private String routerInitLogMsgTemplate = "[ROUTER]: add routable unit %s";
    public void addRoutableFunction(Class typeToRoute, Function routeToFn){
        if (!routingUnits.containsKey(typeToRoute)){
            routingUnits.put(typeToRoute, new ArrayList<>());
        }

        List<RoutingUnit> routeRecord = routingUnits.get(typeToRoute);

        String routableUnitNameTemplate = "%sProcessor-%s";
        String routableUnitName = String.format(
                routableUnitNameTemplate,
                typeToRoute.getSimpleName(),
                routeRecord.size()
        );

        log.info(String.format(routerInitLogMsgTemplate, routableUnitName));

        routeRecord.add(
                            RoutingUnit.builder()
                                            .name(routableUnitName)
                                            .call(routeToFn)
                                        .build()
                        );
    }

    private void buildRoutingTable(){
        routingUnits.entrySet().stream()
                .map(
                        entry -> {
                            List<RoutingUnit> units = entry.getValue();

                            List<String> uNames = units.stream().map(
                                    unit -> unit.getName()
                            ).collect(Collectors.toList());

                            routingTable.put(entry.getKey(), uNames);

                            return uNames;
                        }
                    )
                .collect(Collectors.toList());
        log.info("[ROUTER]: routing table built");
    }

    private Consumer routeObjectSink(){

        return envelope -> {
            log.info("beep");
        };
    }
    private void buildStreams(){

    }

    public void start(){
        buildRoutingTable();
    }

    public BiConsumer<String, Object> singleRequestsInput(){

        String logTemplate = "[ROUTER]: request %s received";

        return (rqId, payload) -> {
            log.info(String.format(logTemplate,rqId));
            routeObjectSink().accept(
                    RoutedObjectEnvelope.builder()
                                                .rqId(rqId)
                                                .payload(payload)
                                            .build()
            );
        };
    }

}
