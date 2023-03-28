package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RoutedObjectEnvelope;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.LimiterPassRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.*;
import java.util.stream.IntStream;

@Slf4j
public class TokenBucketRateLimiter {
    @Getter
    private String name;

    @Getter
    private int bucketCapacity = 450; // default value corresponds to 90% of connections Netty can initiate instantly

    private final Deque<String> tokenBucket = new ConcurrentLinkedDeque<>();

    private final Set<String> spentCoins =  ConcurrentHashMap.newKeySet();
    private final Map<String, String> billsInProcess = new ConcurrentHashMap<>();

    private BiConsumer<String,Object> requestSink;

    private Consumer<Object> reportOverloadStart;
    private Consumer<Object> reportOverloadEnd;

    public void connectOverloadMonitoring(
            Consumer<Object> reportOverloadStart,
            Consumer<Object> reportOverloadEnd
    ){
        this.reportOverloadStart = reportOverloadStart;
        this.reportOverloadEnd = reportOverloadEnd;
    }

    private BiFunction<String,String,String> onTokenGot;
    private Consumer<String> onTokenReleased;

    public void connectTokenStatMonitoring(
            BiFunction<String,String, String> onTokenGot,
            Consumer<String> onTokenReleased
    ){
        this.onTokenGot = onTokenGot;
        this.onTokenReleased = onTokenReleased;
    }

    private Consumer<RouterBypassRequest> parkRequestSink;
    private Supplier<RouterBypassRequest> parkRequestSource;

    Function<String, Boolean> requestPriorityDetector;
    private BiFunction<String, RequestPriority,String> requestPrioritySetter;
    private BiFunction<String,String,String> correlIdSetter;

    @Builder
    public TokenBucketRateLimiter(
            int bucketCapacity,
            BiConsumer<String, Object> requestSink,
            Consumer<RouterBypassRequest> parkRequestSink,
            Supplier<RouterBypassRequest> parkRequestSource,
            Function<String, Boolean> requestPriorityDetector,
            BiFunction<String, RequestPriority,String> requestPrioritySetter,
            BiFunction<String,String,String> correlIdSetter,
            String limiterName,
            Consumer<Object> reportOverloadStart,
            Consumer<Object> reportOverloadEnd
    ){
        this.name = limiterName;
        this.bucketCapacity = bucketCapacity;
        this.requestSink = requestSink;

        // connect the external stuff to park queued requests
        this.parkRequestSink = parkRequestSink;
        this.parkRequestSource = parkRequestSource;
        this.requestPriorityDetector = requestPriorityDetector;
        this.requestPrioritySetter = requestPrioritySetter;
        this.correlIdSetter = correlIdSetter;

        // monitoring stuff
        this.reportOverloadStart = reportOverloadStart;
        this.reportOverloadEnd = reportOverloadEnd;

        // and now some gold
        // $$$$$        x_0
        IntStream.range(0,this.bucketCapacity).forEach(
                num -> tokenBucket.push(
                        //"coin Id"
                        UUID.randomUUID().toString()
                )
        );
    }

    public BiFunction<Object,RoutedObjectEnvelope,RoutedObjectEnvelope> executePassedRequest(){
        return (requestObj, envelope) -> {

            LimiterPassRequest requestEnvelope = null;
            try {
                requestEnvelope = (LimiterPassRequest) requestObj;
            } catch (Exception e){
                // for some reason we subscribed to some other type of objects
            }

            if (requestEnvelope != null){
                String coinId = requestEnvelope.getCoinId();

                Object runResult = null;

                if (requestEnvelope.getTransmissionFn() != null){
                    runResult = requestEnvelope.getTransmissionFn().apply(requestEnvelope.getPayload());
                }

                //billsInProcess.put(coinId,envelope.getRqId());
                if (onTokenGot != null){
                    String bill = onTokenGot.apply(envelope.getRqId(),coinId);
                    billsInProcess.put(coinId,bill);
                }

                Object output = tryReleaseToken(coinId).apply(runResult);

                envelope.setPayload(output);
            }
            return envelope;
        };
    }

    public BiFunction<Object,RoutedObjectEnvelope,RoutedObjectEnvelope> aggregateBypass(){
        return (requestObj, envelope) -> {

            RouterBypassRequest bypassRequest = null;
            try{
                bypassRequest = (RouterBypassRequest) requestObj;
            } catch (Exception e){}

            if (bypassRequest != null){
                bypassRequest.setRequestId(envelope.getRqId());
                bypassRequest.setCorrelId(envelope.getCorrelId());
                bypassRequest.setPriority(envelope.getPriority());
            }

            return envelope;
        };
    }



    public Consumer<RouterBypassRequest> receiveBypassRequest(){
        return bypassRequest -> {

            if (parkRequestSink != null){
                parkRequestSink.accept(bypassRequest);
            }
        };
    }

    private Map<String,Integer> cashFlowHistory = new HashMap<>();



    private void updateHistory(String coin) {
        if (!cashFlowHistory.containsKey(coin)){
            cashFlowHistory.put(coin,1);
        } else {
            cashFlowHistory.put(coin, cashFlowHistory.get(coin) + 1);
        }
    }

    private synchronized String spendCoin(){
        String output = null;
        if (!tokenBucket.isEmpty()){
            String coin = tokenBucket.pop();
            spentCoins.add(coin);

            updateHistory(coin);

            if (reportOverloadEnd != null){
                reportOverloadEnd.accept("");
            }

            output = coin;
        }

        return output;
    }

    private Function tryObtainToken (){

        return inputObj -> {
            //log.info("[LIMITER]: try get token "+envelope.getClass().getSimpleName());

            Object outputObj = null;

            // TODO: put here a function which decides to allow the processing of the request
            if (tokenBucket.isEmpty()){
                //log.info("[LIMITER]: no coins left");
                if (!(inputObj instanceof RouterBypassRequest)){
                    outputObj = wrapToBypassRequest(inputObj);
                }

                if (reportOverloadStart != null) {
                    reportOverloadStart.accept("");
                }
            } else {
                // this call is synchronized
                String coinId = spendCoin();
                if (coinId != null){
                    if (!(inputObj instanceof LimiterPassRequest)){
                        LimiterPassRequest passRequest = LimiterPassRequest.builder()
                                                            .coinId(coinId)
                                                        .build();
                        if (inputObj instanceof RouterBypassRequest){
                            RouterBypassRequest bypassRequest = (RouterBypassRequest) inputObj;
                            passRequest.setPayload(bypassRequest.getPayload());
                            passRequest.setTransmissionFn(bypassRequest.getDelayedCall());
                        } else {
                            passRequest.setPayload(inputObj);
                        }
                        outputObj = passRequest;
                    }

                } else {
                    // the probability we can obtain no token still exist
                    outputObj = wrapToBypassRequest(inputObj);
                }
            }

            return outputObj;
        };
    }

    private static RouterBypassRequest wrapToBypassRequest(Object inputObj) {

        RouterBypassRequest bypassRequest = null;

        if (inputObj instanceof RouterBypassRequest){
            bypassRequest = (RouterBypassRequest) inputObj;
        } else {
            bypassRequest = RouterBypassRequest.builder()
                                    .bypassStart(
                                            OffsetDateTime.now()
                                    )
                                .build();
            if (inputObj instanceof LimiterPassRequest){
                LimiterPassRequest passRq = (LimiterPassRequest) inputObj;
                bypassRequest.setPayload(passRq.getPayload());
                bypassRequest.setDelayedCall(passRq.getTransmissionFn());
            } else {
                bypassRequest.setPayload(inputObj);
            }
        }

        return bypassRequest;
    }

    public void revokeParkedRequest(){

        RouterBypassRequest queuedRequest = null;
        if (parkRequestSource != null){
            queuedRequest = parkRequestSource.get();
        }

        if (queuedRequest != null){
            if (requestSink != null){
                String requestId = queuedRequest.getRequestId();

                if (requestPrioritySetter != null){
                    boolean requestAlreadyPrioritized = false;
                    if (requestPriorityDetector != null){
                        requestAlreadyPrioritized = requestPriorityDetector.apply(requestId);
                    }
                    if (!requestAlreadyPrioritized){
                        requestId = requestPrioritySetter.apply(requestId,queuedRequest.getPriority());
                    }
                }
                if (correlIdSetter != null){
                    requestId = correlIdSetter.apply(requestId,queuedRequest.getCorrelId());
                }

                Object envelope = tryObtainToken().apply(queuedRequest);

                requestSink.accept(requestId, envelope);
            }
        }
    }

    private synchronized void returnCoin(String coinId){

        if (spentCoins.contains(coinId)){
            tokenBucket.push(coinId);
            spentCoins.remove(coinId);

            if (billsInProcess.containsKey(coinId)){
                if (onTokenReleased != null){
                    onTokenReleased.accept(billsInProcess.get(coinId));
                }
                billsInProcess.remove(coinId);
            }
        }

        revokeParkedRequest();
    }

    private Function tryReleaseToken (String coinId) {

        return inputObj -> {

            // let's check if input object is a result of request bypass knee action
            if (inputObj instanceof RouterBypassRequest){
                // there were no tokens to get, so bypass knee was used
                // the request will be queued
            } else {
                // the original function was activated,
                // so we obtain its output here
                // let's check the sort of the output object
                if( inputObj instanceof CorePublisher<?>) {
                    // some async action happen
                    // so the token will be disposed later

                    Flux fluxItem = null;

                    try {
                        fluxItem = (Flux) inputObj;
                    } catch (Exception e){}

                    if (fluxItem == null){

                        Mono monoItem = null;
                        try {
                            monoItem = (Mono) inputObj;
                        } catch (Exception e){}

                        if (monoItem != null){

                            monoItem = monoItem.doFinally(
                                    signal -> {
                                        returnCoin(coinId);
                                    }
                            );
                            //log.info("[LIMITER]: item is Mono");

                            inputObj = monoItem;
                        }

                    } else {
                        //log.info("[LIMITER]: item is Flux");
                        fluxItem = fluxItem.doFinally(
                                signal -> {
                                    returnCoin(coinId);
                                }
                        );

                        inputObj = fluxItem;
                    }
                } else {
                    // no async actions detected,
                    // dispose the token now
                    returnCoin(coinId);
                    log.info("[LIMITER]: SYNC RETURN COIN");
                }

            }

            return inputObj;
        };
    }

    private Function bypassTransmissionKnee(){
        return payload -> payload;
    }
    public Function<Function, Function> takeControlOverTransmission() {

        return transmissionFn ->
                    tryObtainToken()
                    .andThen(
                                inputObj -> {
                                    Function output = transmissionFn;
                                    if (inputObj instanceof RouterBypassRequest){
                                        RouterBypassRequest bypassRequest = (RouterBypassRequest) inputObj;
                                        bypassRequest.setDelayedCall(transmissionFn);
                                        output = bypassTransmissionKnee();
                                    } else {
                                        if (inputObj instanceof LimiterPassRequest){
                                            LimiterPassRequest passEnvelope = (LimiterPassRequest) inputObj;
                                            passEnvelope.setTransmissionFn(transmissionFn);
                                            output = bypassTransmissionKnee();
                                        }
                                    }
                                    return output.apply(inputObj);
                                }
                            );
    }
}
