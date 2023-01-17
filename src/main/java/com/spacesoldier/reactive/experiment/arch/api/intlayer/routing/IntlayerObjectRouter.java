package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
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
import java.util.concurrent.ConcurrentHashMap;
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

    private Function<String, Boolean> requestPriorityDetector;
    private Function<String, RequestPriority> requestPriorityExtractor;

    private Function<String, String> removePriorityFromRqId;


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
                        .correlId(envelope.getCorrelId())
                        .priority(envelope.getPriority())
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
            Function<String, Boolean> requestPriorityDetector,
            Function<String, RequestPriority> requestPriorityExtractor,
            Function<String, String> removePriorityFromRqId,
            Runnable onRouterReadyAction
    ){
        this.fluxProvider = fluxProvider;
        this.sinkByRqIdProvider = sinkByRqIdProvider;
        this.sinkByChannelNameProvider = sinkByChannelNameProvider;
        this.functionDecorator = functionDecorator;
        this.onRouterReadyAction = onRouterReadyAction;
        this.requestPriorityDetector = requestPriorityDetector;
        this.requestPriorityExtractor = requestPriorityExtractor;
        this.removePriorityFromRqId = removePriorityFromRqId;
    }

    private Map<Class,BiFunction<Object,RoutedObjectEnvelope,Object>> aggregators = new ConcurrentHashMap<>();

    private final Map<Class,BiFunction<Object,RoutedObjectEnvelope,RoutedObjectEnvelope>> envelopeAggr = new ConcurrentHashMap<>();

    public void addEnvelopeAggregation(
            Class typeToApplyAggregator,
            BiFunction<Object,RoutedObjectEnvelope,RoutedObjectEnvelope> envelopeAggregator
    ){
        if (!envelopeAggr.containsKey(typeToApplyAggregator)){
            envelopeAggr.put(typeToApplyAggregator,envelopeAggregator);
        }
    }

    private String routerInitLogMsgTemplate = "[ROUTER]: add routable unit %s";
    public void addRoutableFunction(
            Class typeToRoute,
            Function routeToFn
    ){
        if (!routingUnits.containsKey(typeToRoute)){
            routingUnits.put(typeToRoute, new ArrayList<>());
        }

        List<RoutingUnit> routeRecord = routingUnits.get(typeToRoute);

        String routableUnitNameTemplate = "%s-%s";
        String routeTypeName = typeToRoute != null  ?
                                typeToRoute.getSimpleName()
                                            .replaceAll("[aeiou]", "") :
                                "object"                                               ;
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

    //private final Scheduler fluxPool = Schedulers.newParallel("flux outs");
    private final Scheduler fluxPool = Schedulers.newBoundedElastic(
            16,
            15000,
            "flux outs"
    );
    private final Scheduler monoPool = Schedulers.newSingle("mono outs");

    private Function<Object,RoutedObjectEnvelope> envelopeObjectToRoute(String requestId){
        return payload -> {
            //log.info("RqId: "+ requestId);

            RequestPriority priority = null;
            String rqId = requestId;

            if (requestIsPrioritised(requestId)){
                priority = extractPriority(requestId);
                rqId = removePriorityFromRqId(requestId);
            }
            if (priority == null){
                priority = RequestPriority.BACKGROUND;
            }

            return RoutedObjectEnvelope.builder()
                                            .rqId(rqId)
                                            .correlId(requestId)
                                            .priority(priority)
                                            .payload(payload)
                                        .build();
        };
    }

    private boolean requestIsPrioritised(String requestId){
        boolean result = false;

        if (requestPriorityDetector != null){
            Boolean isPrioritised = requestPriorityDetector.apply(requestId);
            result = isPrioritised;
        }

        return result;
    }
    private RequestPriority extractPriority(String requestId) {
        RequestPriority priority = RequestPriority.BACKGROUND;

        if (requestPriorityExtractor != null){
            priority = requestPriorityExtractor.apply(requestId);
        }
        return priority;
    }

    private String removePriorityFromRqId(String requestId){
        String output = requestId;

        if (removePriorityFromRqId != null){
            output = removePriorityFromRqId.apply(requestId);
        }

        return output;
    }

    private void handlePublisherOutputs(String rqId, CorePublisher publisher){

        Flux fluxPub = null;
        Mono monoPub = null;

        try {
            fluxPub = (Flux) publisher;
        } catch (Exception e){
            // probably we got a Mono
            // log.info("[ROUTER]: payload is not a flux");
        }

        if (fluxPub == null ){
            // try to cast to Mono
            try {
                monoPub = (Mono) publisher;
            } catch (Exception e){
                // ok, some unexpected sort of CorePublisher received
                log.info("[ROUTER]: payload is not a mono, it is " + publisher.getClass().getSimpleName());
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
                                                                .priority(RequestPriority.USER_LEVEL)
                                                                .payload(error)
                                                            .build()
                            ),
                            () -> {
                                // let's perform an action when the publisher finishes the transmission
                                // log.info("flux "+ rqId+ " completed");
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
                                                                .priority(RequestPriority.USER_LEVEL)
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

    private RoutedObjectEnvelope envelopeAggregation(RoutedObjectEnvelope envelope){

        RoutedObjectEnvelope output = envelope;

        if (envelope.getPayload() != null){
            Class payloadType = envelope.getPayload().getClass();
            // we perform an envelope aggregation if it is defined for certain payload type
            if (envelopeAggr.containsKey(payloadType)){
                output = envelopeAggr.get(payloadType).apply(envelope.getPayload(),envelope);
            }
        }

        return output;
    }

    private void routeBasicPayload(RoutedObjectEnvelope incomingEnvelope) {
        // sometimes we could want to aggregate the payload with envelope data
        // for example to add requestId for some purpose
        //RoutedObjectEnvelope envelope = postProcessPayload(incomingEnvelope);

        // sometimes we could need to restore original request id in outgoing envelope
        // for example after some aggregation stage in business logic

        // briefly speaking, we have a need to aggregate the envelope fields
        // with request payload in several cases,
        // so we perform an envelope aggregation if it is defined for certain payload type
        RoutedObjectEnvelope finalEnvelope = envelopeAggregation(incomingEnvelope);

        Class routingType = null;

        if (finalEnvelope.getPayload() != null){
            routingType = finalEnvelope.getPayload().getClass();
        } else {
            finalEnvelope.setPayload(
                    new HashMap<String,String>() {{
                        put("RqId",finalEnvelope.getRqId());
                        put("CorrelId",finalEnvelope.getCorrelId());
                        put("Error","null payload");
                    }}
            );
            log.info(
                        "[OBJECT ROUTER]: null payload in envelope RqId = "+finalEnvelope.getRqId()
                        +" CorrelId = "+finalEnvelope.getCorrelId()
            );
        }

        List<Consumer> sinks = new ArrayList<>();

        List<String> receivers = null;

        if (
                routingType != null                  &&
                routingTable.containsKey(routingType)
        )
        {
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
            Consumer sinkById = sinkByRqIdProvider.apply(finalEnvelope.getRqId());
            if (sinkById != null){
                // we unwrap the envelope because the receiver
                // definitely knows the request id
                sinkById.accept(finalEnvelope.getPayload());
            } else {
                log.info("[ROUTER]: drop the request "+ finalEnvelope.getRqId()+" - nowhere to route");
            }
        } else {
            // and finally we throw our envelope to all receivers found
            sinks.forEach(
                    sink -> sink.accept(finalEnvelope)
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
                                                        Function logicFn = functionDecorator != null ?
                                                                functionDecorator.apply(
                                                                        unit.getInputType(),
                                                                        unit.getCall()
                                                                )                                    :
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

    private boolean isInternalSignalId(String rqId){
        String prefix = "";
        if (rqId != null){
            if (rqId.length() > 3) {
                prefix = rqId.substring(0,3);
            } else {
                log.info("[ROUTER WARN]: received request with empty rqId");
            }
        } else {
            log.info("[ROUTER WARN]: received request with rqId = null");
        }

        return prefix.equals(">|<");
    }

    public BiConsumer<String, Object> singleRequestsInput(){

        String logTemplate = "[ROUTER]: request %s received";

        return (rqId, payload) -> {
            if (!isInternalSignalId(rqId)){
                log.info(String.format(logTemplate,rqId));
            }

            RequestPriority priority = null;
            if (requestIsPrioritised(rqId)){
                priority = extractPriority(rqId);
            }


            routeObjectSink().accept(
                    RoutedObjectEnvelope.builder()
                                            .rqId(rqId)
                                            .correlId(rqId)
                                            .priority(priority)
                                            .payload(payload)
                                        .build()
            );
        };
    }

}
