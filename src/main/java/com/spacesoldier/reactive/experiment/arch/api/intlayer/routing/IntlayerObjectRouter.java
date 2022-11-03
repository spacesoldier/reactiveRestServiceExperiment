package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RoutedObjectEnvelope;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RoutingUnit;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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

    private BiFunction<Class,Function,Function> functionDecorator;

    @Setter
    private Runnable onRouterReadyAction;

    private Function<RoutedObjectEnvelope,RoutedObjectEnvelope> defaultFunctionDecorator(
            Function fnToDecorate
    ){

        return envelope -> {
            Object result = null;

            try {
                result = fnToDecorate.apply(envelope.getPayload());
            } catch (Exception e){
                result = e;
            }

            return RoutedObjectEnvelope.builder()
                    .rqId(envelope.getRqId())
                    .payload(result)
                    .build();
        };
    }

    @Builder
    private IntlayerObjectRouter(
            Function<String,Flux> fluxProvider,
            Function<String,Consumer> sinkByRqIdProvider,
            Function<String,Consumer> sinkByChannelNameProvider,
            BiFunction<Class, Function, Function> functionDecorator,
            Runnable onRouterReadyAction
    ){
        this.fluxProvider = fluxProvider;
        this.sinkByRqIdProvider = sinkByRqIdProvider;
        this.sinkByChannelNameProvider = sinkByChannelNameProvider;
        this.functionDecorator = functionDecorator;
        this.onRouterReadyAction = onRouterReadyAction;
    }

    private String routerInitLogMsgTemplate = "[ROUTER]: add routable unit %s";
    public void addRoutableFunction(Class typeToRoute, Function routeToFn){
        if (!routingUnits.containsKey(typeToRoute)){
            routingUnits.put(typeToRoute, new ArrayList<>());
        }

        List<RoutingUnit> routeRecord = routingUnits.get(typeToRoute);

        String routableUnitNameTemplate = "%sProcessor-%s";
        String routeTypeName = typeToRoute != null? typeToRoute.getSimpleName() : "object";
        String routableUnitName = String.format(
                routableUnitNameTemplate,
                routeTypeName,
                routeRecord.size()
        );

        log.info(String.format(routerInitLogMsgTemplate, routableUnitName));

        routeRecord.add(
                RoutingUnit.builder()
                        .name(routableUnitName)
                        .inputType(typeToRoute)
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
                .toList();
        log.info("[ROUTER]: routing table built");
    }

    private final Scheduler fluxPool = Schedulers.newParallel("flux outs");
    private final Scheduler monoPool = Schedulers.newParallel("mono outs");

    private Function<Object,RoutedObjectEnvelope> envelopeObjectToRoute(String requestId){
        return payload -> {
            //log.info("RqId: "+ requestId);

            return RoutedObjectEnvelope.builder()
                    .rqId(requestId)
                    .correlId(requestId)
                    .payload(payload)
                    .build();
        };
    }

    private void handlePublisherOutputs(String rqId, CorePublisher publisher){

        Flux fluxPub = null;
        Mono monoPub = null;

        try {
            fluxPub = (Flux) publisher;
        } catch (Exception e){
            log.info("[ROUTER]: payload is not a flux");
        }

        if (fluxPub == null ){
            try {
                monoPub = (Mono) publisher;
            } catch (Exception e){
                log.info("[ROUTER]: payload is not a mono");
            }
        }

        // very dumb part, sorry
        if (fluxPub != null){
            fluxPub
                    .publishOn(fluxPool)
                    .map(   envelopeObjectToRoute(rqId) )
                    .subscribe(
                            routeObjectSink(),
                            // when errors happen we won't lose them and route according to the plan
                            // until client outside receives a report
                            error -> routeObjectSink().accept(
                                    RoutedObjectEnvelope.builder()
                                            .rqId(rqId)
                                            .payload(error)
                                            .build()
                            ),
                            () -> {
                                // let's perform an action when the publisher finishes the transmission
                                log.info("flux "+ rqId+ " completed");
                            }
                    );
        } else {
            if (monoPub != null){
                monoPub
                        .publishOn(monoPool)
                        .map(   envelopeObjectToRoute(rqId) )
                        .subscribe(
                                routeObjectSink(),
                                error -> routeObjectSink().accept(
                                        RoutedObjectEnvelope.builder()
                                                .rqId(rqId)
                                                .payload(error)
                                                .build()
                                )
                        );
            }
        }

    }

    private Consumer<RoutedObjectEnvelope> routeObjectSink(){

        return envelope -> {
            if (envelope.getPayload() instanceof CorePublisher<?>){
                handlePublisherOutputs(envelope.getRqId(), (CorePublisher) envelope.getPayload());
            } else {
                routeBasicPayload(envelope);
            }
        };
    }

    private void routeBasicPayload(RoutedObjectEnvelope envelope) {
        Class routingType = envelope.getPayload().getClass();

        List<Consumer> sinks = new ArrayList<>();

        List<String> receivers = null;


        if (routingTable.containsKey(routingType)){
            receivers = routingTable.get(routingType);
            sinks.addAll(
                    receivers.stream()
                            .map(
                                    receiverName -> {
                                        Consumer sinkToRoute = sinkByChannelNameProvider.apply(receiverName);
                                        return sinkToRoute;
                                    }
                            )
                            .filter(sink -> sink != null)
                            .toList()
            );
            //log.info("route");
        }


        if (sinks.size() == 0){
            // we did not find any receiver for the object among channel names,
            // so we return the object to the adapter which sent it here
            // finding a sink by envelope's request id
            Consumer sinkById = sinkByRqIdProvider.apply(envelope.getRqId());
            if (sinkById != null){
                // we unwrap the envelope because the receiver
                // definitely knows the request id
                sinkById.accept(envelope.getPayload());
            }
        } else {
            // and finally we throw our envelope to all receivers found
            sinks.forEach(
                    sink -> sink.accept(envelope)
            );
        }
    }

    List<Flux> allStreams;

    private void buildStreams(){
        allStreams = routingUnits.entrySet()
                .stream()
                .flatMap(
                        entry -> {
                            List<Flux> streams =
                                    entry.getValue()
                                            .stream()
                                            .map(
                                                    unit -> {
                                                        Function logicFn = functionDecorator != null         ?
                                                                functionDecorator.apply(
                                                                        unit.getInputType(),
                                                                        unit.getCall()
                                                                )                               :
                                                                defaultFunctionDecorator(
                                                                        unit.getCall()
                                                                );

                                                        Flux stream = null;

                                                        if (fluxProvider != null){
                                                            stream = fluxProvider.apply(unit.getName());
                                                            stream = stream.map(logicFn);
                                                        }

                                                        return stream;
                                                    }
                                            )
                                            .filter(Objects::nonNull)
                                            .toList();
                            return streams.stream();
                        }
                )
                .toList();
    }

    public void subscribeToAllStreams(){
        allStreams.stream()
                .map(
                        stream -> stream.subscribe(
                                routeObjectSink()
                        )
                )
                .toList();
    }

    public void start(){
        buildRoutingTable();
        buildStreams();
        subscribeToAllStreams();
        if (onRouterReadyAction != null){
            onRouterReadyAction.run();
        }
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